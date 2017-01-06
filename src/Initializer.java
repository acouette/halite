import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by acouette on 12/28/16.
 */
public class Initializer {


    private List<Loc> allLocs;

    List<Zone> getZones() {


        List<Loc> remainingLocations = new ArrayList<>();
        remainingLocations.addAll(allLocs);

        List<Zone> zones = new ArrayList<>();
        while (!remainingLocations.isEmpty()) {
            Loc bestRemainingLocation = remainingLocations.stream()
                    .min((l1, l2) -> Double.compare((double) l1.strength / l1.production, (double) l2.strength / l2.production)).get();
            double maxReversedScoreInZone;
            if (bestRemainingLocation.production == 0) {
                maxReversedScoreInZone = 0;
            } else {
                maxReversedScoreInZone = (bestRemainingLocation.strength / bestRemainingLocation.production) * 2;
            }
            Zone zone = getZone(remainingLocations, bestRemainingLocation, maxReversedScoreInZone);
            zones.add(zone);
        }
        return zones;
    }


    private Zone getZone(List<Loc> remainingLocations, Loc source, double maxReversedScoreInZone) {
        List<Loc> locations = getLocations(remainingLocations, source, maxReversedScoreInZone);
        double score = locations.stream().filter(l -> l.strength > 0).
                mapToDouble(l -> (double) l.production / l.strength).average().getAsDouble();
        return new Zone(locations, score);
    }


    private List<Loc> getLocations(List<Loc> remainingLocations, Loc source, double maxReversedScore) {
        remainingLocations.remove(source);
        List<Loc> inZone = new ArrayList<>();
        inZone.add(source);
        for (Direction direction : Direction.CARDINALS) {
            Location scannedLocation = Constants.gameMap.getLocation(source.getLocation(), direction);
            Loc loc = allLocs.stream().filter(l -> l.getLocation().equals(scannedLocation)).findAny().get();
            if (maxReversedScore >= (double) loc.strength / loc.production && remainingLocations.contains(loc)) {
                inZone.addAll(getLocations(remainingLocations, loc, maxReversedScore));
            }
        }
        return inZone;
    }


    void buildDirections() {
        Map<Location, Loc> locationAndSitePerLocations = allLocs.stream().collect(Collectors.toMap(Loc::getLocation, l -> l));
        for (Loc l : allLocs) {
            HashMap<Direction, Loc> toBuild = new HashMap<>();
            for (Direction dir : Direction.DIRECTIONS) {
                toBuild.put(dir, locationAndSitePerLocations.get(Constants.gameMap.getLocation(l.getLocation(), dir)));
            }
            Constants.DIRECTIONS.put(l.getLocation(), toBuild);
        }


    }

    void buildAverageCellCost() {
        double cost = 0;
        for (Loc loc : allLocs) {
            if (loc.production > 0) {
                cost += (double) loc.strength / loc.production;
            }
        }
        Constants.AVERAGE_CELL_COST = cost / allLocs.size();
    }

    void buildPlayerDensity() {
        Set<Integer> players = new HashSet<>();
        for (Loc loc : allLocs) {
            players.add(loc.owner);
        }
        int playerCount = players.size() - 1;
        Constants.DENSE_PLAYER = (Constants.gameMap.width + Constants.gameMap.height) / playerCount < 15;
    }

    List<Loc> getAllLocs() {
        List<Loc> locs = new ArrayList<>();
        for (int y = 0; y < Constants.gameMap.height; y++) {
            for (int x = 0; x < Constants.gameMap.width; x++) {
                Location location = new Location(x, y);
                Site site = Constants.gameMap.getSite(location);
                Loc loc = new Loc(location);
                loc.owner = site.owner;
                loc.strength = site.strength;
                loc.production = site.production;
                locs.add(loc);
            }
        }
        allLocs = locs;
        return locs;
    }


}
