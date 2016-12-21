import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CouettoBot {


    private final String name;
    private final float sectionCut;
    private final Function<Site, Boolean> isMinStrengthToFriendlyCell;
    private final Supplier<Integer> distanceWeight;
    private Integer minStrengthToFight;


    private boolean firstContact = false;
    private final PathManager pathManager = new PathManager();
    private Map<Location, Vertex> vertexMap;
    private List<LocationAndSite> allLocationAndSites;
    private List<Zone> zones;
    private Map<Location, LocationAndSite> locationAndSitePerLocations;

    private boolean timeoutFallback = false;


    public CouettoBot(String name, float sectionCut, Function<Site, Boolean> isMinStrengthToFriendlyCell, Supplier<Integer> distanceWeight, Integer minStrengthToFight) {
        this.name = name;
        this.sectionCut = sectionCut;
        this.isMinStrengthToFriendlyCell = isMinStrengthToFriendlyCell;
        this.distanceWeight = distanceWeight;
        this.minStrengthToFight = minStrengthToFight;
    }


    public void run() {


        Constants.initConstants();
        Networking.sendInit(name);
        allLocationAndSites = getAllLocationAndSites();
        locationAndSitePerLocations = allLocationAndSites.stream().collect(Collectors.toMap(LocationAndSite::getLocation, l -> l));
        buildDirections();
        zones = getZones();
        buildAverageCellCost();
        Logger.log("cost : " + Constants.AVERAGE_CELL_COST);
        Logger.log("zones : " + zones.size());

        while (true) {
            long start = System.currentTimeMillis();
            Constants.turn++;
            ArrayList<Move> moves = new ArrayList<>();
            Constants.gameMap = Networking.getFrame();
            refreshSitesData();
            List<LocationAndSite> locationsToMove = allLocationAndSites.stream()
                    .filter(l -> l.getSite().owner == Constants.myID)
                    .filter(l -> l.getSite().strength > 0)
                    .sorted((l1, l2) -> Integer.compare(l2.getSite().strength, l1.getSite().strength))
                    .collect(Collectors.toList());

            if (timeoutFallback && Constants.turn > 150 && Constants.gameMap.width > 45 && allLocationAndSites.stream()
                    .filter(l -> l.getSite().owner == Constants.myID).count() > allLocationAndSites.size() * (3f / 4)) {

                for (LocationAndSite current : locationsToMove) {
                    Direction direction;
                    if (current.getSite().strength > 6 * current.getSite().production) {
                        LocationAndSite closestNotMine = findClosestNotMine(current);
                        direction = getDirection(current.getLocation(), closestNotMine.getLocation());
                    } else {
                        direction = Direction.STILL;
                    }
                    moves.add(new Move(current.getLocation(), direction));
                }
            } else {
                timeoutFallback = false;
                vertexMap = pathManager.getVertexMap(allLocationAndSites, name);
                NextTurnState nextTurnState = new NextTurnState(allLocationAndSites);

                for (LocationAndSite current : locationsToMove) {
                    Logger.log("----------------turn " + Constants.turn);
                    Logger.log("----------------current : " + current);
                    Location currentLocation = current.getLocation();
                    Site currentSite = current.getSite();

                    Direction direction;
                    if (shouldSkip(currentLocation, currentSite)) {
                        direction = Direction.STILL;
                    } else {
                        vertexMap.values().forEach(Vertex::reset);
                        pathManager.computePaths(vertexMap.get(currentLocation));

                        Direction closeHostile = getAttackDirection(currentLocation, currentSite);
                        if (closeHostile != null) {
                            direction = closeHostile;
                            firstContact = true;
                        } else {
                            LocationAndSite locationToTarget = getLocationToTarget();
                            if (locationToTarget == null) {
                                direction = Direction.STILL;
                            } else {
                                Direction dir = getDirectionFromVertex(currentLocation, locationToTarget.getLocation());
                                LocationAndSite toMoveOnto = Constants.DIRECTIONS.get(currentLocation).get(dir);
                                Site siteToMoveOnto = toMoveOnto.getSite();
                                if ((siteToMoveOnto.owner == Constants.myID && !isMinStrengthToFriendlyCell.apply(currentSite))
                                        || (siteToMoveOnto.owner == 0
                                        && siteToMoveOnto.strength >= currentSite.strength
                                        && (siteToMoveOnto.strength != 255 || currentSite.strength != 255))) {
                                    dir = Direction.STILL;
                                }
                                direction = dir;
                            }
                        }
                    }
                    direction = nextTurnState.preventStackingStrength(current, direction, firstContact);
                    moves.add(new Move(currentLocation, direction));

                    //Logger.log("time : " + (System.currentTimeMillis() - start) + " ms");

                }

                if ((System.currentTimeMillis() - start) > 1100) {
                    timeoutFallback = true;
                }

            }
            Networking.sendFrame(moves);


        }
    }

    private void buildAverageCellCost() {
        double cost = 0;
        for (LocationAndSite locationAndSite : allLocationAndSites) {
            if (locationAndSite.getSite().production > 0) {
                cost += (double) locationAndSite.getSite().strength / locationAndSite.getSite().production;
            }
        }
        Constants.AVERAGE_CELL_COST = cost / allLocationAndSites.size();
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

    private void buildDirections() {

        for (LocationAndSite l : allLocationAndSites) {
            HashMap<Direction, LocationAndSite> toBuild = new HashMap<>();
            for (Direction dir : Direction.DIRECTIONS) {
                toBuild.put(dir, locationAndSitePerLocations.get(Constants.gameMap.getLocation(l.getLocation(), dir)));
            }
            Constants.DIRECTIONS.put(l.getLocation(), toBuild);
        }


    }


    private boolean shouldSkip(Location currentLocation, Site currentSite) {
        int i = 0;
        for (LocationAndSite value : Constants.DIRECTIONS.get(currentLocation).values()) {
            if (value.getSite().owner == Constants.myID) {
                i++;
            }
        }
        return i == 5 && !isMinStrengthToFriendlyCell.apply(currentSite);

    }

    private LocationAndSite getLocationToTarget() {
        LocationAndSite locationToTarget = null;
        double bestScore = -1;
        for (Zone zone : zones) {
            if (isAnyNotOwned(zone)) {

                Optional<LocationWithDistance> locationWithDistance = zone.getLocations().stream()
                        .filter(l -> l.getSite().owner == 0)
                        .map(l -> new LocationWithDistance(l, pathManager.getCostTo(vertexMap.get(l.getLocation()))))
                        .min((l1, l2) -> Double.compare(l1.getDistance(), l2.getDistance()));

                if (locationWithDistance.isPresent()) {
                    double score = zone.getScore() / (locationWithDistance.get().getDistance() + (Constants.turn > 80 ? 0 : Constants.AVERAGE_CELL_COST));
                    if (score > bestScore) {
                        bestScore = score;
                        locationToTarget = locationWithDistance.get().getLocation();
                    }
                }
            }
        }
        if (name.equals("MyBot") && Constants.turn < 30) {
            Logger.log("location : " + locationToTarget);
            for (Zone zone : zones) {
                for (LocationAndSite locationAndSite : zone.getLocations()) {
                    if (locationAndSite.equals(locationToTarget)) {
                        Logger.log("zone " + zone);
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


    private List<Zone> getZones() {


        List<LocationAndSite> remainingLocations = new ArrayList<>();
        remainingLocations.addAll(allLocationAndSites);

        List<Zone> zones = new ArrayList<>();
        while (!remainingLocations.isEmpty()) {
            LocationAndSite bestRemainingLocation = remainingLocations.stream()
                    .min((l1, l2) -> Double.compare((double) l1.getSite().strength / l1.getSite().production, (double) l2.getSite().strength / l2.getSite().production)).get();
            double maxReversedScoreInZone;
            if (bestRemainingLocation.getSite().production == 0) {
                maxReversedScoreInZone = 0;
            } else {
                maxReversedScoreInZone = (bestRemainingLocation.getSite().strength / bestRemainingLocation.getSite().production) * 3;
            }
            Zone zone = getZone(remainingLocations, bestRemainingLocation, maxReversedScoreInZone);
            zones.add(zone);
        }
        return zones;
    }

    private Zone getZone(List<LocationAndSite> remainingLocations, LocationAndSite source, double maxReversedScoreInZone) {
        List<LocationAndSite> locations = getLocations(remainingLocations, source, maxReversedScoreInZone);
        double score = locations.stream().filter(l -> l.getSite().strength > 0).
                mapToDouble(l -> (double) l.getSite().production / l.getSite().strength).average().getAsDouble();
        return new Zone(locations, score);
    }


    private List<LocationAndSite> getLocations(List<LocationAndSite> remainingLocations, LocationAndSite source, double maxReversedScore) {
        remainingLocations.remove(source);
        List<LocationAndSite> inZone = new ArrayList<>();
        inZone.add(source);
        for (Direction direction : Direction.CARDINALS) {
            Location scannedLocation = Constants.gameMap.getLocation(source.getLocation(), direction);
            LocationAndSite locationAndSite = allLocationAndSites.stream().filter(l -> l.getLocation().equals(scannedLocation)).findAny().get();
            if (maxReversedScore >= (double) locationAndSite.getSite().strength / locationAndSite.getSite().production && remainingLocations.contains(locationAndSite)) {
                inZone.addAll(getLocations(remainingLocations, locationAndSite, maxReversedScore));
            }
        }
        return inZone;
    }

    private List<LocationAndSite> getAllLocationAndSites() {
        List<LocationAndSite> locationAndSites = new ArrayList<>();
        for (int y = 0; y < Constants.gameMap.height; y++) {
            for (int x = 0; x < Constants.gameMap.width; x++) {
                Location loc = new Location(x, y);
                locationAndSites.add(new LocationAndSite(loc, Constants.gameMap.getSite(loc)));
            }
        }
        return locationAndSites;
    }

    private Direction getDirectionFromVertex(Location currentLocation, Location destination) {
        List<LocationAndSite> firstBasedOnVertex = pathManager.getShortestPathTo(vertexMap.get(destination));
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


    private Direction getAttackDirection(Location currentLocation, Site currentSite) {
        Location closest = findCloseWithManyNeighbours(currentLocation);
        if (closest != null && minStrengthToFight != null && currentSite.strength <= minStrengthToFight) {
            return Direction.STILL;
        }
        if (closest != null) {
            return getDirectionFromVertex(currentLocation, closest);
        }
        return null;
    }

    private Location findCloseWithManyNeighbours(Location currentLocation) {

        double minCost = Double.MAX_VALUE;
        Location minDistanceLocation = null;
        for (LocationAndSite locationAndSite : allLocationAndSites) {
            Site currentScannedSite = locationAndSite.getSite();
            if (currentScannedSite.owner != Constants.myID && currentScannedSite.owner != 0) {
                Location currentScannedLocation = locationAndSite.getLocation();
                double currentScannedCost = pathManager.getCostTo(vertexMap.get(currentScannedLocation));
                if (currentScannedCost < minCost && 3 > Constants.gameMap.getDistance(currentLocation, currentScannedLocation)) {
                    boolean foundNeighbour = false;

                    for (Map.Entry<Direction, LocationAndSite> neighbour : Constants.DIRECTIONS.get(currentScannedLocation).entrySet()) {
                        if (neighbour.getKey() != Direction.STILL && neighbour.getValue().getSite().owner == currentScannedSite.owner) {
                            foundNeighbour = true;
                            break;
                        }
                    }
                    if (foundNeighbour) {
                        minCost = currentScannedCost;
                        minDistanceLocation = currentScannedLocation;
                    }

                }
            }
        }
        return minDistanceLocation;


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


    //private boolean handleQuickStart(List<Zone> zones) {
//        List<LocationAndSite> myLocations = allLocationAndSites.stream()
//                .filter(l -> l.getSite().owner == Constants.myID)
//                .sorted((l1, l2) -> Integer.compare(l2.getSite().strength, l1.getSite().strength))
//                .collect(Collectors.toList());
//        if (myLocations.size() > 4) {
//            return false;
//        }
//        Logger.log("turn : " + turn);
//        List<AllTargetInfo> locations = new ArrayList<>();
//        int totalStrength = 0;
//        for (LocationAndSite currentLocationAndSite : myLocations) {
//            Location currentLocation = currentLocationAndSite.getLocation();
//            Site currentSite = currentLocationAndSite.getSite();
//            totalStrength += currentSite.strength;
//            vertexMap.values().forEach(Vertex::reset);
//            pathManager.computePaths(vertexMap.get(currentLocation));
//            LocationAndSite locationToTarget = getLocationToTarget(zones);
//            Direction dir = getDirectionFromVertex(currentLocation, locationToTarget.getLocation());
//            Location locationToMoveOnto = Constants.gameMap.getLocation(currentLocation, dir);
//            double cost = pathManager.getCostTo(vertexMap.get(locationToTarget.getLocation()));
//            AllTargetInfo l = new AllTargetInfo(currentLocation, locationToTarget.getLocation(), locationToMoveOnto, cost, dir);
//            locations.add(l);
//        }
//        Set<Location> locationsToTarget = locations.stream().map(AllTargetInfo::getLocationToTarget).collect(Collectors.toSet());
//        if (locationsToTarget.size() != 1) {
//            return false;
//        }
//
//        ArrayList<Move> shortMoves = new ArrayList<>();
//        List<Location> notOwnedNextLocations = locations.stream().map(AllTargetInfo::getNextLocation).filter(l -> sitesPerLocation.get(l).owner != Constants.myID).collect(Collectors.toList());
//        if (notOwnedNextLocations.size() != 1) {
//            return false;
//        }
//        Site notOwnedSite = sitesPerLocation.get(notOwnedNextLocations.get(0));
//        Logger.log("locationToTarget : " + notOwnedSite);
//        Logger.log("locationToTarget str : " + notOwnedSite.strength);
//        Logger.log("totalStrength : " + totalStrength);
//
//
//
//
//        if (notOwnedSite.strength > totalStrength) {
//            for (AllTargetInfo currentLocationAndSite : locations) {
//                shortMoves.add(new Move(currentLocationAndSite.getCurrentLocation(), Direction.STILL));
//            }
//            Networking.sendFrame(shortMoves);
//            return true;
//        } else {
//            List<AllTargetInfo> toPlayInOrder = locations.stream().sorted((l1, l2) -> Double.compare(l2.getCost(), l1.getCost())).collect(Collectors.toList());
//
//            AllTargetInfo closest = toPlayInOrder.get(toPlayInOrder.size() - 1);
//            if (notOwnedSite.strength < sitesPerLocation.get(closest.getCurrentLocation()).strength) {
//                shortMoves.add(new Move(closest.getCurrentLocation(), closest.getDirection()));
//                Networking.sendFrame(shortMoves);
//                return true;
//            }
//
//
//            boolean first = true;
//            for (AllTargetInfo allTargetInfo : toPlayInOrder) {
//                if (!first) {
//                    Constants.gameMap = Networking.getFrame();
//                }
//                Direction direction;
//                if (sitesPerLocation.get(allTargetInfo.getCurrentLocation()).strength == 0) {
//                    direction = Direction.STILL;
//                } else {
//                    direction = allTargetInfo.getDirection();
//                }
//                shortMoves.add(new Move(allTargetInfo.getCurrentLocation(), direction));
//                Networking.sendFrame(shortMoves);
//                first = false;
//            }
//            return true;
//        }
//    }

}