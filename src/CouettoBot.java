import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class CouettoBot {


    private final String name;

    private PathManager pathManager;
    private List<Loc> allLocs;
    private List<Zone> zones;

    private boolean timeoutFallback = false;
    private List<Loc> myLocations;
    private List<Loc> emptyNeutrals;
    private boolean firstContact = false;


    public CouettoBot(String name) {
        this.name = name;
    }


    public void run() {


        Constants.initConstants();
        Networking.sendInit(name);
        Initializer initializer = new Initializer();
        allLocs = initializer.getAllLocs();
        initializer.buildDirections();
        zones = initializer.getZones();
        initializer.buildAverageCellCost();
        initializer.buildPlayerDensity();


        while (true) {
            long start = System.currentTimeMillis();
            Constants.turn++;
            ArrayList<Move> moves = new ArrayList<>();
            Constants.gameMap = Networking.getFrame();
            refreshSitesData();
            List<Loc> locationsToMove = getLocationsToMoveInOrder();


            if (timeoutFallback && Constants.turn > 100 && Constants.gameMap.width > 40 && myLocations.size() > allLocs.size() * (1f / 2)) {

                for (Loc current : locationsToMove) {
                    Direction direction;
                    if (current.strength > 5 * current.production) {
                        Loc closestNotMine = findClosestNotMine(current);
                        direction = getDirection(current.getLocation(), closestNotMine.getLocation());
                    } else {
                        direction = Direction.STILL;
                    }
                    moves.add(new Move(current.getLocation(), direction));
                }
            } else {
                timeoutFallback = false;
                pathManager = new PathManager(allLocs, myLocations);

                if (handleQuickStart()) {
                    continue;
                }

                NextTurnState nextTurnState = new NextTurnState(allLocs);


                for (Loc current : locationsToMove) {
                    Location currentLocation = current.getLocation();
                    boolean wentToCombat = false;
                    Direction direction;
                    if (current.strength == 0) {
                        direction = Direction.STILL;
                    } else {

                        boolean strengthNotEnough = isStrengthNotEnough(current);

                        if (current.isSuroundedByMine() && strengthNotEnough) {
                            direction = Direction.STILL;
                        } else {
                            if (!current.isSuroundedByMine() || !firstContact || (direction = nextTurnState.getGoodMergeDirection(current)) == null) {
                                pathManager.computePaths(currentLocation);
                                Loc locationToTarget = getLocationToGo();
                                direction = getDirectionFromVertex(currentLocation, locationToTarget.getLocation());
                                Loc toMoveOnto = Constants.DIRECTIONS.get(currentLocation).get(direction);
                                if (toMoveOnto.isMine()) {
                                    if (strengthNotEnough) {
                                        direction = Direction.STILL;
                                    }
                                } else if (toMoveOnto.isEmptyNeutral()) {
                                    if (!isOkToFight(current)) {
                                        direction = Direction.STILL;
                                    } else {
                                        wentToCombat = true;
                                        firstContact = true;
                                    }
                                } else if (toMoveOnto.isNeutral() &&
                                        (toMoveOnto.strength != 255 || current.strength != 255)
                                        && (toMoveOnto.strength >= current.strength)) {
                                    direction = Direction.STILL;
                                }
                            }
                        }
                    }
                    direction = nextTurnState.preventStackingStrength(current, direction, wentToCombat);
                    moves.add(new Move(currentLocation, direction));
                }

                if ((System.currentTimeMillis() - start) > 1100) {
                    timeoutFallback = true;
                }
            }
            Networking.sendFrame(moves);

        }
    }


    private boolean isStrengthNotEnough(Loc currentSite) {
        if (myLocations.size() < 20) {
            return currentSite.strength < currentSite.production * 5;
        } else if (myLocations.size() < 50) {
            return currentSite.strength < currentSite.production * 6;
        } else if (myLocations.size() < 400) {
            return currentSite.strength < currentSite.production * 6 || currentSite.strength < 30;
        } else if (myLocations.size() < 800) {
            return currentSite.strength < currentSite.production * 8 || currentSite.strength < 60;
        } else {
            return currentSite.strength < currentSite.production * 10 || currentSite.strength < 100;
        }

    }


    private List<Loc> getLocationsToMoveInOrder() {


        List<Loc> nextToEmptyNeutral = myLocations.stream()
                .filter(l -> l.hasNextEmptyNeutral() && isOkToFight(l))
                .sorted((l1, l2) -> Integer.compare(l2.strength, l1.strength))
                .collect(Collectors.toList());
        List<Loc> notNextToEmptyNeutral = myLocations.stream()
                .filter(l -> !l.hasNextEmptyNeutral() || !isOkToFight(l))
                .sorted((l1, l2) -> Integer.compare(l2.strength, l1.strength))
                .collect(Collectors.toList());

        nextToEmptyNeutral.addAll(notNextToEmptyNeutral);
        return nextToEmptyNeutral;
    }

    private boolean isOkToFight(Loc l) {
        return l.strength >= l.production * 4;
    }


    private Loc findClosestNotMine(Loc current) {
        double minDistance = Double.MAX_VALUE;
        Loc closest = null;
        for (Loc loc : allLocs) {
            if (loc.owner != Constants.myID) {
                double distance = Constants.gameMap.getDistance(current.getLocation(), loc.getLocation());
                if (distance < minDistance) {
                    minDistance = distance;
                    closest = loc;
                }
            }
        }
        return closest;
    }

    private Loc getLocationToGo() {
        Loc locationToTarget = null;
        double bestScore = -1;


        Optional<LocWithCost> emptyNeutralsInOrder = emptyNeutrals.stream()
                .map(l -> new LocWithCost(l, pathManager.getCostTo(l.getLocation())))
                .min((l1, l2) -> Double.compare(l1.getDistance(), l2.getDistance()));
        if (emptyNeutralsInOrder.isPresent()) {
            double score = (3.2d / Constants.AVERAGE_CELL_COST) / emptyNeutralsInOrder.get().getDistance();
            if (score > bestScore) {
                bestScore = score;
                locationToTarget = emptyNeutralsInOrder.get().getLocation();
            }
        }

        for (Zone zone : zones) {
            List<Loc> locsWithCost = zone.getLocations().stream()
                    .filter(Loc::isNeutral)
                    .filter(l -> !l.isEmptyNeutral())
                    .collect(Collectors.toList());

            if (!locsWithCost.isEmpty()) {


                Optional<LocWithCost> locWithCost = locsWithCost.stream().map(l -> new LocWithCost(l, pathManager.getCostTo(l.getLocation())))
                        .min((l1, l2) -> Double.compare(l1.getDistance(), l2.getDistance()));

                if (locWithCost.isPresent()) {
                    double score;
                    if ((Constants.DENSE_PLAYER && myLocations.size() < 12) || !(Constants.DENSE_PLAYER && myLocations.size() < 25)) {
                        score = zone.getScore() / (locWithCost.get().getDistance() + (2 * Constants.AVERAGE_CELL_COST));
                    } else {
                        score = zone.getScore() / (locWithCost.get().getDistance() + Constants.AVERAGE_CELL_COST / 2);

                    }

                    if (score > bestScore) {
                        bestScore = score;
                        locationToTarget = locWithCost.get().getLocation();
                    }
                }
            }
        }
        return locationToTarget;
    }


    private void refreshSitesData() {

        for (Loc loc : allLocs) {
            Site site = Constants.gameMap.getSite(loc.getLocation());
            loc.owner = site.owner;
            loc.strength = site.strength;
            loc.setEmptyNeutral(false);
            loc.setNeutral(false);
            loc.setEnemy(false);
            loc.setMine(false);
            loc.setHasNextEmptyNeutral(false);
            loc.setSuroundedByMine(false);
            loc.setCost(0);
            if (site.owner == 0) {
                loc.setNeutral(true);
            } else if (site.owner == Constants.myID) {
                loc.setMine(true);
            } else {
                loc.setEnemy(true);
            }
        }


        myLocations = allLocs.stream()
                .filter(Loc::isMine)
                .collect(Collectors.toList());

        for (Loc loc : allLocs) {
            double cost;
            if (loc.isNeutral()) {
                cost = loc.production == 0 ? 1000 : (double) loc.strength / loc.production;
            } else if (loc.isMine()) {
                if (myLocations.size() < 6) {
                    cost = 1;
                } else if ((Constants.DENSE_PLAYER && myLocations.size() < 12) || (!Constants.DENSE_PLAYER && myLocations.size() < 25)) {
                    cost = 6;
                } else if ((Constants.DENSE_PLAYER && myLocations.size() < 25) || (!Constants.DENSE_PLAYER && myLocations.size() < 50)) {
                    cost = 14;
                } else {
                    cost = 20;
                }
            } else {
                cost = 2;
            }
            loc.setCost(cost);
        }

        emptyNeutrals = new ArrayList<>();

        for (Loc loc : allLocs) {
            if (loc.isNeutral() && loc.strength == 0) {
                boolean emptyNeutral = false;
                double cost = 1;
                for (Loc aroundEmpty : Constants.DIRECTIONS.get(loc.getLocation()).values()) {
                    if (aroundEmpty.isMine()) {
                        if (!emptyNeutral) {
                            loc.setEmptyNeutral(true);
                            emptyNeutrals.add(loc);
                            emptyNeutral = true;
                        }
                        cost += 0.05;
                    } else if (aroundEmpty.isEnemy()) {
                        cost -= 0.05;
                    }
                }
                loc.setCost(cost);
            }
        }


        for (Loc myLocation : myLocations) {
            int myLocationsCount = 0;
            for (Loc around : Constants.DIRECTIONS.get(myLocation.getLocation()).values()) {
                if (around.isEmptyNeutral()) {
                    myLocation.setHasNextEmptyNeutral(true);
                    break;
                } else if (around.isMine()) {
                    myLocationsCount++;
                }
            }
            if (myLocationsCount == 5) {
                myLocation.setSuroundedByMine(true);
            }
        }
    }


    private Direction getDirectionFromVertex(Location currentLocation, Location destination) {
        List<Loc> firstBasedOnVertex = pathManager.getShortestPathTo(destination);
        if (firstBasedOnVertex.size() < 1) {
            throw new RuntimeException(currentLocation + " could not get path to " + destination + " on turn " + Constants.turn);
        }
        return getDirection(currentLocation, firstBasedOnVertex.get(0).getLocation());


    }


    private Direction getDirection(Location source, Location destination) {
        double angle = Constants.gameMap.getAngle(source, destination);

        if (angle > -(1f / 4) * Math.PI && angle <= (1f / 4) * Math.PI) {
            return Direction.WEST;
        }

        if (angle > (1f / 4) * Math.PI && angle <= (3f / 4) * Math.PI) {
            return Direction.SOUTH;
        }

        if (angle < -(1f / 4) * Math.PI && angle >= -(3f / 3) * Math.PI) {
            return Direction.NORTH;
        }
        return Direction.EAST;

    }


    private class LocWithCost {
        private final Loc location;

        private final double distance;

        public LocWithCost(Loc location, double distance) {
            this.location = location;
            this.distance = distance;
        }

        public Loc getLocation() {
            return location;
        }


        public double getDistance() {
            return distance;
        }

    }


    private boolean handleQuickStart() {
        List<Loc> myLocations = allLocs.stream()
                .filter(l -> l.owner == Constants.myID)
                .sorted((l1, l2) -> Integer.compare(l2.strength, l1.strength))
                .collect(Collectors.toList());
        if (myLocations.size() > 5) {
            return false;
        }
        List<AllTargetInfo> locations = new ArrayList<>();
        int totalStrength = 0;
        for (Loc currentLoc : myLocations) {
            Location currentLocation = currentLoc.getLocation();
            totalStrength += currentLoc.strength;
            pathManager.computePaths(currentLocation);
            Loc locationToTarget = getLocationToGo();
            Direction dir = getDirectionFromVertex(currentLocation, locationToTarget.getLocation());
            Loc locationToMoveOnto = Constants.DIRECTIONS.get(currentLocation).get(dir);
            double cost = pathManager.getCostTo(locationToTarget.getLocation());
            AllTargetInfo l = new AllTargetInfo(currentLoc, locationToTarget, locationToMoveOnto, cost, dir);
            locations.add(l);
        }
        Set<Loc> locationsToTarget = locations.stream().map(AllTargetInfo::getLocationToTarget).collect(Collectors.toSet());
        if (locationsToTarget.size() != 1) {
            return false;
        }

        ArrayList<Move> shortMoves = new ArrayList<>();
        List<Loc> notOwnedNextLocations = locations.stream()
                .map(AllTargetInfo::getNextLocation).filter(l -> l.owner != Constants.myID).collect(Collectors.toList());
        if (notOwnedNextLocations.size() != 1) {
            return false;
        }
        Loc notOwnedSite = notOwnedNextLocations.get(0);

        if (notOwnedSite.strength < myLocations.stream().mapToInt(l -> l.production).sum() * 5) {
            return false;
        }


        List<AllTargetInfo> toPlayInOrder = locations.stream().sorted((l1, l2) -> Double.compare(l2.getCost(), l1.getCost())).collect(Collectors.toList());

        int i = 0;
        for (AllTargetInfo toPlay : toPlayInOrder) {
            totalStrength += i * toPlay.getCurrentLocation().production;
            i++;
        }


        if (notOwnedSite.strength >= totalStrength) {
            for (AllTargetInfo currentLocationAndSite : locations) {
                shortMoves.add(new Move(currentLocationAndSite.getCurrentLocation().getLocation(), Direction.STILL));
            }
            Networking.sendFrame(shortMoves);
            return true;
        } else {

            AllTargetInfo closest = toPlayInOrder.get(toPlayInOrder.size() - 1);
            if (notOwnedSite.strength < closest.getCurrentLocation().strength) {
                return false;
            }


            boolean first = true;
            for (AllTargetInfo allTargetInfo : toPlayInOrder) {
                if (!first) {
                    Constants.gameMap = Networking.getFrame();
                    refreshSitesData();
                }

                for (AllTargetInfo notToMove : toPlayInOrder) {
                    if (!notToMove.equals(allTargetInfo)) {
                        shortMoves.add(new Move(notToMove.getCurrentLocation().getLocation(), Direction.STILL));
                    } else {
                        Direction direction;
                        if (allTargetInfo.getCurrentLocation().strength == 0) {
                            direction = Direction.STILL;
                        } else {
                            direction = allTargetInfo.getDirection();
                        }
                        shortMoves.add(new Move(allTargetInfo.getCurrentLocation().getLocation(), direction));
                    }
                }
                Networking.sendFrame(shortMoves);
                first = false;
            }
            return true;
        }
    }

}