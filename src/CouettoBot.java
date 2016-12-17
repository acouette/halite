import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CouettoBot {


    private final String name;

    private final float sectionCut;

    private final BiFunction<Site, Integer, Boolean> isMinStrengthToFriendlyCell;
    private final Function<Integer, Integer> distancePonderation;
    private final Function<Site, Double> getPathCost;
    private Integer minStrengthToFight;


    private PathManager pathManager = new PathManager();
    private Map<Location, Vertex> vertexMap = null;
    private List<LocationAndSite> allLocationAndSites;
    private Map<Location, Site> sitesPerLocation;
    private int turn;


    public CouettoBot(String name, float sectionCut, BiFunction<Site, Integer, Boolean> isMinStrengthToFriendlyCell, Function<Integer, Integer> distancePonderation, Function<Site, Double> getPathCost, Integer minStrengthToFight) {
        this.name = name;
        this.sectionCut = sectionCut;
        this.isMinStrengthToFriendlyCell = isMinStrengthToFriendlyCell;
        this.distancePonderation = distancePonderation;
        this.getPathCost = getPathCost;
        this.minStrengthToFight = minStrengthToFight;
    }


    public void run() {


        Constants.initConstants();

        Networking.sendInit(name);

        allLocationAndSites = getAllLocationAndSites();
        sitesPerLocation = allLocationAndSites.stream().collect(Collectors.toMap(LocationAndSite::getLocation, LocationAndSite::getSite));
        List<Zone> zones = getZones();

        turn = 0;
        while (true) {
            turn++;

            ArrayList<Move> moves = new ArrayList<>();
            Constants.gameMap = Networking.getFrame();
            refreshSitesOwnership();
            vertexMap = pathManager.getVertexMap(allLocationAndSites, getPathCost);
            List<LocationAndSite> myLocations = allLocationAndSites.stream()
                    .filter(l -> l.getSite().owner == Constants.myID)
                    .filter(l -> l.getSite().strength > 0)
                    .sorted((l1, l2) -> Integer.compare(l2.getSite().strength, l1.getSite().strength))
                    .collect(Collectors.toList());
            NextTurnState nextTurnState = new NextTurnState();

            //long start = System.currentTimeMillis();
            for (LocationAndSite currentLocationAndSite : myLocations) {
                Location currentLocation = currentLocationAndSite.getLocation();
                Site currentSite = currentLocationAndSite.getSite();

                Direction direction;
                if (shouldSkip(currentLocation, currentSite)) {
                    direction = Direction.STILL;
                } else {
                    vertexMap.values().forEach(Vertex::reset);
                    pathManager.computePaths(vertexMap.get(currentLocation));

                    Direction closeHostile = findCloseHostile(currentLocation, currentSite);
                    if (closeHostile != null) {
                        direction = closeHostile;
                    } else {
                        Location locationToTarget = getLocationToTarget(zones);
                        if (locationToTarget == null) {
                            direction = Direction.STILL;
                        } else {
                            Direction dir = getDirectionFromVertex(currentLocation, locationToTarget);
                            Location locationToMoveOnto = Constants.gameMap.getLocation(currentLocation, dir);
                            Site siteToMoveOnto = sitesPerLocation.get(locationToMoveOnto);
                            if ((siteToMoveOnto.owner == Constants.myID && !isMinStrengthToFriendlyCell(currentSite))
                                    || (siteToMoveOnto.owner == 0 && siteToMoveOnto.strength >= currentSite.strength)) {
                                dir = Direction.STILL;
                            }
                            direction = dir;
                        }
                    }
                }
                direction = nextTurnState.preventStackingStrength(currentLocation, currentSite, direction, sitesPerLocation);
                moves.add(new Move(currentLocation, direction));
                //Logger.log("time : " + (System.currentTimeMillis() - start) + " ms");

            }
            Networking.sendFrame(moves);


        }
    }


    private boolean shouldSkip(Location currentLocation, Site currentSite) {
        int i = 0;
        for (Direction dir : Direction.CARDINALS) {
            if (sitesPerLocation.get(Constants.gameMap.getLocation(currentLocation, dir)).owner == Constants.myID) {
                i++;
            }
        }
        return i == 4 && !isMinStrengthToFriendlyCell(currentSite);

    }

    private boolean isMinStrengthToFriendlyCell(Site site) {
        return isMinStrengthToFriendlyCell.apply(site, turn);
    }

    private Location getLocationToTarget(List<Zone> zones) {
        Location locationToTarget = null;
        double bestScore = -1;
        for (Zone zone : zones) {
            if (isAnyNotOwned(zone)) {
                Optional<LocationWithDistance> locationWithDistance = getDistance(zone);
                if (locationWithDistance.isPresent()) {
                    double score = getScore(zone, locationWithDistance.get());
                    if (score > bestScore) {
                        bestScore = score;
                        locationToTarget = locationWithDistance.get().getLocation();
                    }
                }
            }
        }
        return locationToTarget;
    }

    private double getScore(Zone zone, LocationWithDistance locationWithDistance) {
        double strengthInterest = 1 / (((double) zone.getStrength()) / zone.getLocations().size());
        return ((zone.getDensity() / (locationWithDistance.getDistance() + distancePonderation.apply(turn)))) * strengthInterest;
    }

    private boolean isAnyNotOwned(Zone zone) {
        for (LocationAndSite loc : zone.getLocations()) {
            if (loc.getSite().owner != Constants.myID) {
                return true;
            }
        }
        return false;
    }

    private void refreshSitesOwnership() {
        for (LocationAndSite locationAndSite : allLocationAndSites) {
            Site site = Constants.gameMap.getSite(locationAndSite.getLocation());
            locationAndSite.getSite().owner = site.owner;
            locationAndSite.getSite().strength = site.strength;
        }
    }

    private Optional<LocationWithDistance> getDistance(Zone zone) {
        return zone.getLocations().stream()
                .filter(l -> l.getSite().owner == 0)
                .map(l -> new LocationWithDistance(l.getLocation(), pathManager.getCostTo(vertexMap.get(l.getLocation()))))
                .min((l1, l2) -> Double.compare(l1.getDistance(), l2.getDistance()));
    }


    private List<Zone> getZones() {


        List<LocationAndSite> remainingLocations = new ArrayList<>();
        remainingLocations.addAll(allLocationAndSites);

        List<Zone> zones = new ArrayList<>();
        while (!remainingLocations.isEmpty()) {
            int bestProduction = remainingLocations.stream().mapToInt(l -> l.getSite().production).max().getAsInt();
            LocationAndSite peak = remainingLocations.stream().filter(l -> l.getSite().production == bestProduction).findAny().get();
            double minimumProduction = bestProduction / sectionCut;
            Zone zone = getZone(remainingLocations, peak, minimumProduction);
            zones.add(zone);
        }
        return zones;
    }

    private Zone getZone(List<LocationAndSite> remainingLocations, LocationAndSite source, double minimumProduction) {
        List<LocationAndSite> locations = getLocations(remainingLocations, source, minimumProduction);
        int totalProduction = locations.stream().mapToInt(l -> l.getSite().production).sum();
        int totalStrength = locations.stream().mapToInt(l -> l.getSite().strength).sum();
        return new Zone(locations, totalProduction, ((double) totalProduction) / locations.size(), totalStrength);
    }


    private List<LocationAndSite> getLocations(List<LocationAndSite> remainingLocations, LocationAndSite source, double minimumProduction) {
        remainingLocations.remove(source);
        List<LocationAndSite> inZone = new ArrayList<>();
        inZone.add(source);
        for (Direction direction : Direction.CARDINALS) {
            Location scannedLocation = Constants.gameMap.getLocation(source.getLocation(), direction);
            LocationAndSite locationAndSite = allLocationAndSites.stream().filter(l -> l.getLocation().equals(scannedLocation)).findAny().get();
            if (minimumProduction <= locationAndSite.getSite().production && remainingLocations.contains(locationAndSite)) {
                inZone.addAll(getLocations(remainingLocations, locationAndSite, minimumProduction));
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
        if (firstBasedOnVertex.size() < 2) {
            return Direction.EAST;
        }
        return getDirection(currentLocation, firstBasedOnVertex.get(1).getLocation());


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


    private Direction findCloseHostile(Location currentLocation, Site currentSite) {
        Location closest = findClosest(currentLocation, owner -> owner != Constants.myID && owner != 0);
        if (closest != null && minStrengthToFight != null && currentSite.strength <= minStrengthToFight) {
            return Direction.STILL;
        }
        if (closest != null) {
            return getDirectionFromVertex(currentLocation, closest);
        }
        return null;
    }

    private Location findClosest(Location currentLocation, Predicate<Integer> ownershipCondition) {

        double minCost = Double.MAX_VALUE;
        Location minDistanceLocation = null;
        for (int x = 0; x < Constants.gameMap.width; x++) {
            for (int y = 0; y < Constants.gameMap.height; y++) {

                Location currentScannedLocation = new Location(x, y);
                Site currentScannedSite = sitesPerLocation.get(currentScannedLocation);
                if (ownershipCondition.test(currentScannedSite.owner)) {
                    double currentScannedCost = pathManager.getCostTo(vertexMap.get(new Location(x, y)));
                    if (currentScannedCost < minCost && 3 > Constants.gameMap.getDistance(currentLocation, currentScannedLocation)) {
                        minCost = currentScannedCost;
                        minDistanceLocation = currentScannedLocation;
                    }
                }
            }

        }
        return minDistanceLocation;


    }


    private class LocationWithDistance {
        private Location location;

        private double distance;

        public LocationWithDistance(Location location, double distance) {
            this.location = location;
            this.distance = distance;
        }

        public Location getLocation() {
            return location;
        }

        public void setLocation(Location location) {
            this.location = location;
        }

        public double getDistance() {
            return distance;
        }

        public void setDistance(double distance) {
            this.distance = distance;
        }
    }

}