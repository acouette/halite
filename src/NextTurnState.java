import java.util.*;

/**
 * Created by acouette on 12/17/16.
 */
public class NextTurnState {

    private final Map<Location, Integer> strengthPerLocation = new HashMap<>();

    private Set<Location> toExclude;

    private final static int TOLERABLE_LOSS_ON_MERGE = 25;

    private static Map<Direction, List<Direction>> LOCATIONS_TO_TRY = new HashMap<>();

    static {
        LOCATIONS_TO_TRY.put(Direction.NORTH, Arrays.asList(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.STILL));
        LOCATIONS_TO_TRY.put(Direction.EAST, Arrays.asList(Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH, Direction.STILL));
        LOCATIONS_TO_TRY.put(Direction.WEST, Arrays.asList(Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH, Direction.STILL));
        LOCATIONS_TO_TRY.put(Direction.SOUTH, Arrays.asList(Direction.SOUTH, Direction.NORTH, Direction.WEST, Direction.EAST, Direction.STILL));
        LOCATIONS_TO_TRY.put(Direction.STILL, Arrays.asList(Direction.STILL, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.NORTH));
    }

    public NextTurnState(List<LocationAndSite> allLocationAndSite) {

        toExclude = new HashSet<>();
        for (LocationAndSite locationAndSite : allLocationAndSite) {
            if (locationAndSite.getSite().owner == 0) {
                strengthPerLocation.put(locationAndSite.getLocation(), -locationAndSite.getSite().strength);
            } else {
                strengthPerLocation.put(locationAndSite.getLocation(), 0);
            }
            if ((locationAndSite.getLocation().x + locationAndSite.getLocation().y + Constants.turn) % 2 == 0) {
                toExclude.add(locationAndSite.getLocation());
            }
        }
    }

    public Direction preventStackingStrength(LocationAndSite current, Direction direction, boolean firstContact) {

        Direction optimizedDir = direction;
        Location initialTargetLocation = Constants.DIRECTIONS.get(current.getLocation()).get(direction).getLocation();
        if (firstContact
                && !toExclude.contains(initialTargetLocation)
                && !(strengthPerLocation.get(current.getLocation()) > 254
                || strengthPerLocation.get(current.getLocation()) + current.getSite().strength + current.getSite().production > 255 + TOLERABLE_LOSS_ON_MERGE)) {
            optimizedDir = Direction.STILL;
        } else {

            int minLoss = Integer.MAX_VALUE;
            for (Direction dir : LOCATIONS_TO_TRY.get(direction)) {
                LocationAndSite fallback = Constants.DIRECTIONS.get(current.getLocation()).get(dir);
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
        return optimizedDir;
    }


}
