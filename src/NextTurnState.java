import java.util.*;

/**
 * Created by acouette on 12/17/16.
 */
public class NextTurnState {

    private final Map<Location, Integer> strengthPerLocation = new HashMap<>();

    private Set<Location> toExclude;

    private final static int TOLERABLE_LOSS_ON_MERGE = 10;

    private static Map<Direction, List<Direction>> LOCATIONS_TO_TRY = new HashMap<>();

    static {
        LOCATIONS_TO_TRY.put(Direction.NORTH, Arrays.asList(Direction.NORTH, Direction.STILL, Direction.WEST, Direction.EAST, Direction.SOUTH));
        LOCATIONS_TO_TRY.put(Direction.EAST, Arrays.asList(Direction.EAST, Direction.STILL, Direction.SOUTH, Direction.NORTH, Direction.WEST));
        LOCATIONS_TO_TRY.put(Direction.WEST, Arrays.asList(Direction.WEST, Direction.STILL, Direction.NORTH, Direction.SOUTH, Direction.EAST));
        LOCATIONS_TO_TRY.put(Direction.SOUTH, Arrays.asList(Direction.SOUTH, Direction.STILL, Direction.WEST, Direction.EAST, Direction.NORTH));
        LOCATIONS_TO_TRY.put(Direction.STILL, Arrays.asList(Direction.STILL, Direction.NORTH, Direction.EAST, Direction.WEST, Direction.SOUTH));
    }

    public NextTurnState(List<LocationAndSite> allLocationAndSite) {

        toExclude = new HashSet<>();
        for (LocationAndSite locationAndSite : allLocationAndSite) {
            if (locationAndSite.getSite().owner == 0) {
                strengthPerLocation.put(locationAndSite.getLocation(), -locationAndSite.getSite().strength);
            } else {
                strengthPerLocation.put(locationAndSite.getLocation(), 0);
            }
        }
    }

    public boolean isToAvoid(Location location) {
        return toExclude.contains(location) || strengthPerLocation.get(location) > 254;
    }

    public Direction preventStackingStrength(LocationAndSite current, Direction direction, boolean wentToCombat) {

        Direction optimizedDir = direction;


        int minLoss = Integer.MAX_VALUE;
        for (Direction dir : LOCATIONS_TO_TRY.get(direction)) {
            LocationAndSite fallback = Constants.DIRECTIONS.get(current.getLocation()).get(dir);
            if (toExclude.contains(fallback.getLocation())) {
                continue;
            }
            Location fallbackLocation = fallback.getLocation();
            boolean noExcess = strengthPerLocation.get(fallbackLocation) < 255
                    && strengthPerLocation.get(fallbackLocation) + current.getSite().strength < 255 + TOLERABLE_LOSS_ON_MERGE;
            if (noExcess) {
                optimizedDir = dir;
                break;
            } else {
                if (strengthPerLocation.get(fallbackLocation) + current.getSite().strength < minLoss) {
                    minLoss = strengthPerLocation.get(fallbackLocation) + current.getSite().strength;
                    optimizedDir = dir;
                }
            }

        }


        int strengthInTargetNextTurn;
        Location locationThatWillHaveMoreStrength;
        if (optimizedDir == Direction.STILL) {
            strengthInTargetNextTurn = current.getSite().strength + current.getSite().production;
            locationThatWillHaveMoreStrength = current.getLocation();
        } else {
            strengthInTargetNextTurn = current.getSite().strength;
            locationThatWillHaveMoreStrength = Constants.DIRECTIONS.get(current.getLocation()).get(optimizedDir).getLocation();
        }
        strengthInTargetNextTurn += strengthPerLocation.get(locationThatWillHaveMoreStrength);
        strengthPerLocation.put(locationThatWillHaveMoreStrength, strengthInTargetNextTurn);

        if (wentToCombat && optimizedDir == direction) {
            for (Map.Entry<Direction, LocationAndSite> aroundBullet : Constants.DIRECTIONS.get(locationThatWillHaveMoreStrength).entrySet()) {
                if (aroundBullet.getKey() != Direction.STILL) {
                    toExclude.add(aroundBullet.getValue().getLocation());
                }
            }
        }

        return optimizedDir;
    }


}
