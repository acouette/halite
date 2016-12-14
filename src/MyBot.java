import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MyBot {


    PathManager pathManager = new PathManager();
    Map<Location, Vertex> vertexMap = null;
    List<LocationAndSite> allLocationAndSites;
    Map<Location, Site> sitesPerLocation;
    int turn;

    public static void main(String[] args) throws java.io.IOException {

        new MyBot().run();

    }


    public void run() {


        Constants.initConstants();

        Networking.sendInit("Couettos");

        allLocationAndSites = getAllLocationAndSites();
        sitesPerLocation = allLocationAndSites.stream().collect(Collectors.toMap(LocationAndSite::getLocation, LocationAndSite::getSite));
        List<Zone> zones = getZones();

        turn = 0;
        while (true) {


            ArrayList<Move> moves = new ArrayList<>();
            Constants.gameMap = Networking.getFrame();
            refreshSitesOwnership();
            vertexMap = pathManager.getVertexMap(allLocationAndSites);


            long start = System.currentTimeMillis();
            for (int y = 0; y < Constants.gameMap.height; y++) {
                for (int x = 0; x < Constants.gameMap.width; x++) {
                    Location currentLocation = new Location(x, y);
                    Site currentSite = sitesPerLocation.get(currentLocation);
                    if (currentSite.owner == Constants.myID && currentSite.strength > 0) {

                        vertexMap.values().forEach(Vertex::reset);
                        pathManager.computePaths(vertexMap.get(currentLocation));

                        Direction closeHostile = findCloseHostile(currentLocation);
                        if (closeHostile != null) {
                            moves.add(new Move(currentLocation, closeHostile));
                        } else {
                            Location locationToTarget = getLocationToTarget(zones);
                            if (locationToTarget == null) {
                                moves.add(new Move(currentLocation, Direction.STILL));
                            } else {
                                Direction direction = getDirectionFromVertex(currentLocation, locationToTarget);
                                Location locationToMoveOnto = Constants.gameMap.getLocation(currentLocation, direction);
                                Site siteToMoveOnto = sitesPerLocation.get(locationToMoveOnto);
                                if ((siteToMoveOnto.owner == Constants.myID && currentSite.strength < (turn > 40 ? 50 : 20))
                                        || (siteToMoveOnto.owner == 0 && siteToMoveOnto.strength >= currentSite.strength)) {
                                    direction = Direction.STILL;
                                }
                                moves.add(new Move(currentLocation, direction));
                            }
                        }
                    }
                }
            }
            Logger.log("time : " + (System.currentTimeMillis() - start) + " ms");
            Networking.sendFrame(moves);
        }
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
        double strengthInterest = 255d / (((double) zone.getStrength()) / zone.getLocations().size());
        return (((zone.getDensity()) / (locationWithDistance.getDistance() + (turn > 120 ? 0 : 10)))) * strengthInterest;
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
            locationAndSite.getSite().owner = Constants.gameMap.getSite(locationAndSite.getLocation()).owner;
            locationAndSite.getSite().strength = Constants.gameMap.getSite(locationAndSite.getLocation()).strength;
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
            double minimumProduction = bestProduction / 1.4;
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
//        if (firstBasedOnVertex.size() < 2) {
//            return Direction.EAST;
//        }
        return getDirection(currentLocation, firstBasedOnVertex.get(1).getLocation());


    }


    private Direction getDirection(Location source, Location destination) {
        double angle = Constants.gameMap.getAngle(source, destination);
        if (Math.random() < 0.5) {
            angle += 0.01;
        } else {
            angle -= 0.01;
        }

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


    private Direction findCloseHostile(Location currentLocation) {
        return findClosest(currentLocation, owner -> owner != Constants.myID && owner != 0, 2);
    }

    private Direction findClosest(Location currentLocation, Predicate<Integer> ownershipCondition, Integer minDistanceAllowed) {

        double minDistance = Double.MAX_VALUE;
        Location minDistanceLocation = null;
        for (int x = 0; x < Constants.gameMap.width; x++) {


            for (int y = 0; y < Constants.gameMap.height; y++) {
                Location currentScannedLocation = new Location(x, y);
                Site currentScannedSite = Constants.gameMap.getSite(currentScannedLocation);
                if (ownershipCondition.test(currentScannedSite.owner)) {
                    double currentScannedDistance = Constants.gameMap.getDistance(currentLocation, currentScannedLocation);
                    if (currentScannedDistance < minDistance && (minDistanceAllowed == null || minDistanceAllowed > currentScannedDistance)) {
                        minDistance = currentScannedDistance;
                        minDistanceLocation = currentScannedLocation;
                    }
                }


            }

        }

        if (minDistanceLocation != null) {
            return getDirection(currentLocation, minDistanceLocation);

        } else {
            return null;
        }


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