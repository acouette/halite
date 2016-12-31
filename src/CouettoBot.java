import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CouettoBot {


    private final String name;

    private Map<Integer, Boolean> firstContacts;
    private boolean firstContact = false;
    private PathManager pathManager;
    private List<LocationAndSite> allLocationAndSites;
    private List<Zone> zones;

    private boolean timeoutFallback = false;
    private List<LocationAndSite> myLocations;


    public CouettoBot(String name) {
        this.name = name;
    }


    public void run() {


        Constants.initConstants();
        Networking.sendInit(name);
        Initializer initializer = new Initializer();
        allLocationAndSites = initializer.getAllLocationAndSites();
        firstContacts = allLocationAndSites.stream().map(l -> l.getSite().owner).distinct().collect(Collectors.toMap(o -> o, o -> false));
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
            checkFirstContact();

            myLocations = allLocationAndSites.stream()
                    .filter(l -> l.getSite().owner == Constants.myID)
                    .collect(Collectors.toList());


            List<LocationAndSite> locationsToMove = getLocationsToMoveInOrder();


            if (timeoutFallback && Constants.turn > 100 && Constants.gameMap.width > 40 && myLocations.size() > allLocationAndSites.size() * (1f / 2)) {

                for (LocationAndSite current : locationsToMove) {
                    Direction direction;
                    if (current.getSite().strength > 5 * current.getSite().production) {
                        LocationAndSite closestNotMine = findClosestNotMine(current);
                        direction = getDirection(current.getLocation(), closestNotMine.getLocation());
                    } else {
                        direction = Direction.STILL;
                    }
                    moves.add(new Move(current.getLocation(), direction));
                }
            } else {
                timeoutFallback = false;
                pathManager = new PathManager(allLocationAndSites, myLocations);

                if (handleQuickStart()) {
                    continue;
                }

                NextTurnState nextTurnState = new NextTurnState(allLocationAndSites);


                for (LocationAndSite current : locationsToMove) {
                    Site currentSite = current.getSite();
                    Location currentLocation = current.getLocation();
                    boolean wentToCombat = false;
                    Direction direction;
                    if (current.getSite().strength == 0) {
                        direction = Direction.STILL;
                    } else {

                        boolean surroundedByFriendly = isSurroundedByFriendly(currentLocation);
                        boolean strengthNotEnough = isStrengthNotEnough(currentSite);


                        if (surroundedByFriendly && strengthNotEnough) {
                            direction = Direction.STILL;
                        } else {

                            if (!surroundedByFriendly && (direction = getAroundEmptyDirection(nextTurnState, currentLocation)) != null) {
                                wentToCombat = true;
                            } else {
                                pathManager.computePaths(currentLocation);
                                LocationAndSite locationToTarget = getLocationToAttack(current);
                                if (locationToTarget == null) {
                                    locationToTarget = getLocationToGo();
                                }
                                if (locationToTarget != null) {
                                    direction = getDirectionFromVertex(currentLocation, locationToTarget.getLocation());
                                    LocationAndSite toMoveOnto = Constants.DIRECTIONS.get(currentLocation).get(direction);
                                    Site siteToMoveOnto = toMoveOnto.getSite();
                                    if ((siteToMoveOnto.owner == Constants.myID && strengthNotEnough)
                                            || (siteToMoveOnto.owner == 0
                                            && (siteToMoveOnto.strength != 255 || currentSite.strength != 255)
                                            && (siteToMoveOnto.strength >= currentSite.strength || getStrongestEnemyAround(toMoveOnto) >= currentSite.strength))) {
                                        direction = Direction.STILL;
                                    }
                                } else {
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

    private int getStrongestEnemyAround(LocationAndSite location) {
        int maxEnemyStrengthAround = 0;
        for (LocationAndSite around : Constants.DIRECTIONS.get(location.getLocation()).values()) {
            Site site = around.getSite();
            if (site.owner != 0 && site.owner != Constants.myID && site.strength > maxEnemyStrengthAround) {
                maxEnemyStrengthAround = site.strength;
            }
        }
        return maxEnemyStrengthAround;
    }

    private boolean isStrengthNotEnough(Site currentSite) {
        if (myLocations.size() > 20) {
            return (currentSite.strength < currentSite.production * 6)
                    || ((firstContact || myLocations.size() > 50) && currentSite.strength < 30);
        } else {
            return currentSite.strength < currentSite.production * 5;
        }
    }

    private Direction getAroundEmptyDirection(NextTurnState nextTurnState, Location currentLocation) {
        int maxNumberOfEnemies = -10;
        LocationAndSite bestEmptyToTarget = null;
        for (Map.Entry<Direction, LocationAndSite> dirAndLoc : Constants.DIRECTIONS.get(currentLocation).entrySet()) {
            LocationAndSite potentialEmpty = dirAndLoc.getValue();
            if (potentialEmpty.getSite().owner == 0 && potentialEmpty.getSite().strength == 0
                    && !nextTurnState.isToAvoid(potentialEmpty.getLocation()) && nextTurnState.willBeEmpty(potentialEmpty.getLocation())) {
                int numberOfCellsToTarget = 0;
                for (LocationAndSite aroundEmpty : Constants.DIRECTIONS.get(potentialEmpty.getLocation()).values()) {
                    if (aroundEmpty.getSite().owner == Constants.myID) {
                        numberOfCellsToTarget--;
                    } else if (aroundEmpty.getSite().owner != 0) {
                        numberOfCellsToTarget++;
                    }
                }
                if (numberOfCellsToTarget > -4 && numberOfCellsToTarget > maxNumberOfEnemies) {
                    bestEmptyToTarget = potentialEmpty;
                    maxNumberOfEnemies = numberOfCellsToTarget;
                }
            }
        }

        if (bestEmptyToTarget != null) {
            return getDirection(currentLocation, bestEmptyToTarget.getLocation());
        }
        return null;
    }

    private List<LocationAndSite> getLocationsToMoveInOrder() {

        final Predicate<LocationAndSite> HAS_NEXT_EMPTY_NEUTRAL = (l) -> {
            for (LocationAndSite locationAndSite : Constants.DIRECTIONS.get(l.getLocation()).values()) {
                if (locationAndSite.getSite().owner == 0 && locationAndSite.getSite().strength == 0) {
                    int myOwnCells = 0;
                    for (LocationAndSite aroundEmpty : Constants.DIRECTIONS.get(locationAndSite.getLocation()).values()) {
                        if (aroundEmpty.getSite().owner == Constants.myID) {
                            myOwnCells++;
                        }
                    }
                    if (myOwnCells < 4) {
                        return true;
                    }
                }
            }
            return false;
        };

        List<LocationAndSite> nextToEmptyNeutral = myLocations.stream()
                .filter(HAS_NEXT_EMPTY_NEUTRAL)
                .filter(l -> l.getSite().strength > 0)
                .sorted((l1, l2) -> Integer.compare(l2.getSite().strength, l1.getSite().strength))
                .collect(Collectors.toList());
        List<LocationAndSite> notNextToEmptyNeutral = myLocations.stream()
                .filter(l -> !HAS_NEXT_EMPTY_NEUTRAL.test(l))
                .filter(l -> l.getSite().strength > 0)
                .sorted((l1, l2) -> Integer.compare(l2.getSite().strength, l1.getSite().strength))
                .collect(Collectors.toList());

        nextToEmptyNeutral.addAll(notNextToEmptyNeutral);
        return nextToEmptyNeutral;
    }


    private void checkFirstContact() {

        if (firstContacts.values().contains(false)) {
            for (LocationAndSite locationAndSite : allLocationAndSites) {
                if (locationAndSite.getSite().owner == 0 && locationAndSite.getSite().strength == 0) {
                    boolean myCell = false;
                    int enemyCell = 0;
                    for (LocationAndSite siteAround : Constants.DIRECTIONS.get(locationAndSite.getLocation()).values()) {
                        if (siteAround.getSite().owner != 0) {
                            if (siteAround.getSite().owner != Constants.myID) {
                                enemyCell = siteAround.getSite().owner;
                            } else if (siteAround.getSite().owner == Constants.myID) {
                                myCell = true;
                            }
                        }
                    }
                    if (myCell && enemyCell != 0) {
                        firstContacts.put(enemyCell, true);
                        firstContact = true;
                    }
                }
            }
        }
    }


    private LocationAndSite findClosestNotMine(LocationAndSite current) {
        double minDistance = Double.MAX_VALUE;
        LocationAndSite closest = null;
        for (LocationAndSite locationAndSite : allLocationAndSites) {
            if (locationAndSite.getSite().owner != Constants.myID) {
                double distance = Constants.gameMap.getDistance(current.getLocation(), locationAndSite.getLocation());
                if (distance < minDistance) {
                    minDistance = distance;
                    closest = locationAndSite;
                }
            }
        }
        return closest;


    }


    private LocationAndSite getLocationToAttack(LocationAndSite current) {
        if (isStrengthNotEnough(current.getSite())) {
            return null;
        }
        Location currentLocation = current.getLocation();
        if (!firstContacts.values().contains(true)) {
            return null;
        }
        double maxDistance = -1;
        LocationAndSite minDistanceLocation = null;
        for (LocationAndSite locationAndSite : allLocationAndSites) {
            double distance = Constants.gameMap.getDistance(currentLocation, locationAndSite.getLocation());
            if (distance <= 3) {
                Site currentScannedSite = locationAndSite.getSite();
                if (currentScannedSite.owner != Constants.myID && currentScannedSite.owner != 0 && firstContacts.get(currentScannedSite.owner)) {
                    //double currentScannedCost = pathManager.getCostTo(vertexMap.get(currentScannedLocation));
                    if (distance > maxDistance) {
                        boolean foundNeighbour = false;

                        for (Map.Entry<Direction, LocationAndSite> neighbour : Constants.DIRECTIONS.get(locationAndSite.getLocation()).entrySet()) {
                            if (neighbour.getKey() != Direction.STILL && neighbour.getValue().getSite().owner == currentScannedSite.owner) {
                                foundNeighbour = true;
                                break;
                            }
                        }
                        if (foundNeighbour || minDistanceLocation == null) {
                            maxDistance = distance;
                            minDistanceLocation = locationAndSite;
                        }

                    }
                }
            }
        }
        return minDistanceLocation;
    }


    private boolean isSurroundedByFriendly(Location currentLocation) {
        int i = 0;
        for (LocationAndSite value : Constants.DIRECTIONS.get(currentLocation).values()) {
            if (value.getSite().owner == Constants.myID) {
                i++;
            }
        }
        return i == 5;

    }

    private LocationAndSite getLocationToGo() {
        LocationAndSite locationToTarget = null;
        double bestScore = -1;
        for (Zone zone : zones) {
            if (isAnyNotOwned(zone)) {

                Optional<LocationWithDistance> locationWithDistance = zone.getLocations().stream()
                        .filter(l -> l.getSite().owner == 0)
                        .map(l -> new LocationWithDistance(l, pathManager.getCostTo(l.getLocation())))
                        .min((l1, l2) -> Double.compare(l1.getDistance(), l2.getDistance()));

                if (locationWithDistance.isPresent()) {
                    double score;
                    if (Constants.DENSE_PLAYER) {
                        if (myLocations.size() < 8) {
                            score = zone.getScore() / (locationWithDistance.get().getDistance() + (2 * Constants.AVERAGE_CELL_COST));
                        } else {
                            score = zone.getScore() / (locationWithDistance.get().getDistance() + Constants.AVERAGE_CELL_COST / 3);

                        }
                    } else {
                        if (myLocations.size() < 12) {
                            score = zone.getScore() / (locationWithDistance.get().getDistance() + (3 * Constants.AVERAGE_CELL_COST));
                        } else {
                            score = zone.getScore() / (locationWithDistance.get().getDistance() + Constants.AVERAGE_CELL_COST);

                        }
                    }
                    if (score > bestScore) {
                        bestScore = score;
                        locationToTarget = locationWithDistance.get().getLocation();
                    }
                }
            }
        }
        return locationToTarget;
    }


    private boolean isAnyNotOwned(Zone zone) {
        for (LocationAndSite loc : zone.getLocations()) {
            if (loc.getSite().owner != Constants.myID) {
                return true;
            }
        }
        return false;
    }

    private void refreshSitesData() {
        for (LocationAndSite locationAndSite : allLocationAndSites) {
            Site site = Constants.gameMap.getSite(locationAndSite.getLocation());
            locationAndSite.getSite().owner = site.owner;
            locationAndSite.getSite().strength = site.strength;
        }
    }


    private Direction getDirectionFromVertex(Location currentLocation, Location destination) {
        List<LocationAndSite> firstBasedOnVertex = pathManager.getShortestPathTo(destination);
        if (firstBasedOnVertex.size() < 1) {
            return Direction.EAST;
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


    private class LocationWithDistance {
        private final LocationAndSite location;

        private final double distance;

        public LocationWithDistance(LocationAndSite location, double distance) {
            this.location = location;
            this.distance = distance;
        }

        public LocationAndSite getLocation() {
            return location;
        }


        public double getDistance() {
            return distance;
        }

    }


    private boolean handleQuickStart() {
        List<LocationAndSite> myLocations = allLocationAndSites.stream()
                .filter(l -> l.getSite().owner == Constants.myID)
                .sorted((l1, l2) -> Integer.compare(l2.getSite().strength, l1.getSite().strength))
                .collect(Collectors.toList());
        if (myLocations.size() > 5) {
            return false;
        }
        List<AllTargetInfo> locations = new ArrayList<>();
        int totalStrength = 0;
        for (LocationAndSite currentLocationAndSite : myLocations) {
            Location currentLocation = currentLocationAndSite.getLocation();
            Site currentSite = currentLocationAndSite.getSite();
            totalStrength += currentSite.strength;
            pathManager.computePaths(currentLocation);
            LocationAndSite locationToTarget = getLocationToGo();
            Direction dir = getDirectionFromVertex(currentLocation, locationToTarget.getLocation());
            LocationAndSite locationToMoveOnto = Constants.DIRECTIONS.get(currentLocation).get(dir);
            double cost = pathManager.getCostTo(locationToTarget.getLocation());
            AllTargetInfo l = new AllTargetInfo(currentLocationAndSite, locationToTarget, locationToMoveOnto, cost, dir);
            locations.add(l);
        }
        Set<LocationAndSite> locationsToTarget = locations.stream().map(AllTargetInfo::getLocationToTarget).collect(Collectors.toSet());
        if (locationsToTarget.size() != 1) {
            return false;
        }

        ArrayList<Move> shortMoves = new ArrayList<>();
        List<LocationAndSite> notOwnedNextLocations = locations.stream()
                .map(AllTargetInfo::getNextLocation).filter(l -> l.getSite().owner != Constants.myID).collect(Collectors.toList());
        if (notOwnedNextLocations.size() != 1) {
            return false;
        }
        Site notOwnedSite = notOwnedNextLocations.get(0).getSite();

        if (notOwnedSite.strength < myLocations.stream().mapToInt(l -> l.getSite().production).sum() * 5) {
            return false;
        }


        List<AllTargetInfo> toPlayInOrder = locations.stream().sorted((l1, l2) -> Double.compare(l2.getCost(), l1.getCost())).collect(Collectors.toList());

        int i = 0;
        for (AllTargetInfo toPlay : toPlayInOrder) {
            totalStrength += i * toPlay.getCurrentLocation().getSite().production;
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
            if (notOwnedSite.strength < closest.getCurrentLocation().getSite().strength) {
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
                        if (allTargetInfo.getCurrentLocation().getSite().strength == 0) {
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