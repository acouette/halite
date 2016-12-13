import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class MyBot2 {


    public static void main(String[] args) throws java.io.IOException {

        new MyBot2().run();

    }


    public void run() {
        Constants.initConstants();

        Networking.sendInit("MyBot2");

        List<Zone> zones = getZones();


        while (true) {
            ArrayList<Move> moves = new ArrayList<Move>();
            Constants.gameMap = Networking.getFrame();

            refreshZonesOwnerShip(zones);


            for (int y = 0; y < Constants.gameMap.height; y++) {
                for (int x = 0; x < Constants.gameMap.width; x++) {
                    Location currentLocation = new Location(x, y);
                    Site currentSite = Constants.gameMap.getSite(currentLocation);
                    if (currentSite.owner == Constants.myID) {

                        Direction closeHostile = findCloseHostile(currentLocation);
                        if (closeHostile != null) {
                            moves.add(new Move(currentLocation, closeHostile));
                        } else {
                            Location locationToTarget = null;
                            double bestScore = -1;
                            for (Zone zone : zones) {
                                if (isAnyNotOwned(zone)) {
                                    Optional<LocationWithDistance> locationWithDistance = getDistance(currentLocation, zone);
                                    if (locationWithDistance.isPresent()) {
                                        double score = getScore(zone, locationWithDistance.get());
                                        if (score > bestScore) {
                                            bestScore = score;
                                            locationToTarget = locationWithDistance.get().getLocation();
                                        }
                                    }
                                }
                            }
                            Direction direction = getDirection(currentLocation, locationToTarget);
                            Location locationToMoveOnto = Constants.gameMap.getLocation(currentLocation, direction);
                            Site siteToMoveOnto = Constants.gameMap.getSite(locationToMoveOnto);
                            if ((siteToMoveOnto.owner == Constants.myID && currentSite.strength < 20)
                                    || (siteToMoveOnto.owner != Constants.myID && siteToMoveOnto.strength > currentSite.strength)) {
                                direction = Direction.STILL;
                            }
                            moves.add(new Move(currentLocation, direction));
                        }
                    }
                }
            }
            Networking.sendFrame(moves);
        }
    }

    private double getScore(Zone zone, LocationWithDistance locationWithDistance) {
        double strengthInterest = 255d / (((double) zone.getStrength()) / zone.getLocations().size());
        return ((5 * zone.getDensity()) / (locationWithDistance.getDistance() + 1.5d)) * strengthInterest;
    }

    private boolean isAnyNotOwned(Zone zone) {
        boolean anyNotOwned = false;
        for (LocationAndSite loc : zone.getLocations()) {
            if (loc.getSite().owner != Constants.myID) {
                anyNotOwned = true;
                break;
            }
        }
        return anyNotOwned;
    }

    private void refreshZonesOwnerShip(List<Zone> zones) {

        for (Zone zone : zones) {
            for (LocationAndSite locationAndSite : zone.getLocations()) {
                locationAndSite.getSite().owner = Constants.gameMap.getSite(locationAndSite.getLocation()).owner;
            }
        }


    }

    private Optional<LocationWithDistance> getDistance(Location currentLocation, Zone zone) {
        return zone.getLocations().stream()
                .filter(l -> l.getSite().owner != Constants.myID)
                .map(l -> new LocationWithDistance(l.getLocation(), Constants.gameMap.getDistance(currentLocation, l.getLocation())))
                .min((l1, l2) -> Double.compare(l1.getDistance(), l2.getDistance()));
    }


    private List<Zone> getZones() {

        List<LocationAndSite> remainingLocations = getAllLocationAndSites();
        List<Zone> zones = new ArrayList<>();
        while (!remainingLocations.isEmpty()) {
            int bestProduction = remainingLocations.stream().mapToInt(l -> l.getSite().production).max().getAsInt();
            LocationAndSite peak = remainingLocations.stream().filter(l -> l.getSite().production == bestProduction).findAny().get();
            double minimumProduction = bestProduction / 1.5;
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
            Site scannedSite = Constants.gameMap.getSite(scannedLocation);
            LocationAndSite locationAndSite = new LocationAndSite(scannedLocation, scannedSite);
            if (minimumProduction <= scannedSite.production && remainingLocations.contains(locationAndSite)) {
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
        return findClosest(currentLocation, owner -> owner != Constants.myID && owner != 0, 3);
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