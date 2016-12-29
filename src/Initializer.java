import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by acouette on 12/28/16.
 */
public class Initializer {


    private List<LocationAndSite> allLocationAndSites;

    List<Zone> getZones() {


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
                maxReversedScoreInZone = (bestRemainingLocation.getSite().strength / bestRemainingLocation.getSite().production) * 2;
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


    void buildDirections() {
        Map<Location, LocationAndSite> locationAndSitePerLocations = allLocationAndSites.stream().collect(Collectors.toMap(LocationAndSite::getLocation, l -> l));
        for (LocationAndSite l : allLocationAndSites) {
            HashMap<Direction, LocationAndSite> toBuild = new HashMap<>();
            for (Direction dir : Direction.DIRECTIONS) {
                toBuild.put(dir, locationAndSitePerLocations.get(Constants.gameMap.getLocation(l.getLocation(), dir)));
            }
            Constants.DIRECTIONS.put(l.getLocation(), toBuild);
        }


    }

    void buildAverageCellCost() {
        double cost = 0;
        for (LocationAndSite locationAndSite : allLocationAndSites) {
            if (locationAndSite.getSite().production > 0) {
                cost += (double) locationAndSite.getSite().strength / locationAndSite.getSite().production;
            }
        }
        Constants.AVERAGE_CELL_COST = cost / allLocationAndSites.size();
    }

    void buildPlayerDensity() {
        Set<Integer> players = new HashSet<>();
        for (LocationAndSite locationAndSite : allLocationAndSites) {
            players.add(locationAndSite.getSite().owner);
        }
        int playerCount = players.size() - 1;
        Constants.DENSE_PLAYER = (Constants.gameMap.width + Constants.gameMap.height) / playerCount < 15;
    }

    List<LocationAndSite> getAllLocationAndSites() {
        List<LocationAndSite> locationAndSites = new ArrayList<>();
        for (int y = 0; y < Constants.gameMap.height; y++) {
            for (int x = 0; x < Constants.gameMap.width; x++) {
                Location loc = new Location(x, y);
                locationAndSites.add(new LocationAndSite(loc, Constants.gameMap.getSite(loc)));
            }
        }
        allLocationAndSites = locationAndSites;
        return locationAndSites;
    }


}
