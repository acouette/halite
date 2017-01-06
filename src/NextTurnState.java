import java.util.*;

/**
 * Created by acouette on 12/17/16.
 */
public class NextTurnState {

    private final Map<Location, Integer> strengthPerLocation = new HashMap<>();

    private Set<Location> toExclude;

    private final static int TOLERABLE_LOSS_ON_MERGE = 10;

    private static Map<Direction, List<Direction>> LOCATIONS_TO_TRY = new HashMap<>();


    public static final int NEXT_TO_EMPTY_MIN_ATTACK = 15;

    static {
        LOCATIONS_TO_TRY.put(Direction.NORTH, Arrays.asList(Direction.NORTH, Direction.STILL, Direction.SOUTH, Direction.WEST, Direction.EAST));
        LOCATIONS_TO_TRY.put(Direction.EAST, Arrays.asList(Direction.EAST, Direction.STILL, Direction.WEST, Direction.SOUTH, Direction.NORTH));
        LOCATIONS_TO_TRY.put(Direction.WEST, Arrays.asList(Direction.WEST, Direction.STILL, Direction.EAST, Direction.NORTH, Direction.SOUTH));
        LOCATIONS_TO_TRY.put(Direction.SOUTH, Arrays.asList(Direction.SOUTH, Direction.STILL, Direction.NORTH, Direction.WEST, Direction.EAST));
        LOCATIONS_TO_TRY.put(Direction.STILL, Arrays.asList(Direction.STILL, Direction.NORTH, Direction.EAST, Direction.WEST, Direction.SOUTH));
    }

    public NextTurnState(List<Loc> allLoc) {

        toExclude = new HashSet<>();
        for (Loc loc : allLoc) {
            if (loc.owner == 0) {
                strengthPerLocation.put(loc.getLocation(), -loc.strength);
            } else {
                strengthPerLocation.put(loc.getLocation(), 0);
            }
        }
    }

    public boolean isToAvoid(Location location) {
        return toExclude.contains(location) || strengthPerLocation.get(location) > 254;
    }

    public Direction preventStackingStrength(Loc current, Direction direction, boolean wentToCombat) {

        Direction optimizedDir = direction;

        if (wentToCombat && current.strength < NEXT_TO_EMPTY_MIN_ATTACK) {
            optimizedDir = Direction.STILL;
        } else {


            int minLoss = Integer.MAX_VALUE;
            for (Direction dir : LOCATIONS_TO_TRY.get(direction)) {
                Loc fallback = Constants.DIRECTIONS.get(current.getLocation()).get(dir);
                if (toExclude.contains(fallback.getLocation())) {
                    continue;
                }
                Location fallbackLocation = fallback.getLocation();
                boolean noExcess = strengthPerLocation.get(fallbackLocation) < 255
                        && strengthPerLocation.get(fallbackLocation) + current.strength < 255 + TOLERABLE_LOSS_ON_MERGE;
                if (noExcess) {
                    optimizedDir = dir;
                    break;
                } else {
                    if (strengthPerLocation.get(fallbackLocation) + current.strength < minLoss) {
                        minLoss = strengthPerLocation.get(fallbackLocation) + current.strength;
                        optimizedDir = dir;
                    }
                }

            }
        }


        int strengthInTargetNextTurn;
        Location locationThatWillHaveMoreStrength;
        if (optimizedDir == Direction.STILL) {
            strengthInTargetNextTurn = current.strength + current.production;
            locationThatWillHaveMoreStrength = current.getLocation();
        } else {
            strengthInTargetNextTurn = current.strength;
            locationThatWillHaveMoreStrength = Constants.DIRECTIONS.get(current.getLocation()).get(optimizedDir).getLocation();
        }
        strengthInTargetNextTurn += strengthPerLocation.get(locationThatWillHaveMoreStrength);
        strengthPerLocation.put(locationThatWillHaveMoreStrength, strengthInTargetNextTurn);

        if (wentToCombat && optimizedDir == direction) {
            for (Map.Entry<Direction, Loc> aroundBullet : Constants.DIRECTIONS.get(locationThatWillHaveMoreStrength).entrySet()) {
                if (((direction == Direction.SOUTH || direction == Direction.NORTH) && (aroundBullet.getKey() == Direction.SOUTH || aroundBullet.getKey() == Direction.NORTH))
                        || ((direction == Direction.WEST || direction == Direction.EAST) && (aroundBullet.getKey() == Direction.WEST || aroundBullet.getKey() == Direction.EAST))) {
                    toExclude.add(aroundBullet.getValue().getLocation());
                }
            }
        }

        return optimizedDir;
    }


    public boolean willNotExceed(Loc target) {
        return strengthPerLocation.get(target.getLocation()) + target.strength < 256;
    }
}
