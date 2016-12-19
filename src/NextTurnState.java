import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by acouette on 12/17/16.
 */
public class NextTurnState {

    private final Map<Location, Integer> strengthPerLocation = new HashMap<>();

    private final static int TOLERABLE_LOSS_ON_MERGE = 30;

    private static Map<Direction, List<Direction>> locationsToTry = new HashMap<>();

    static {
        locationsToTry.put(Direction.NORTH, Arrays.asList(Direction.NORTH, Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.STILL));
        locationsToTry.put(Direction.EAST, Arrays.asList(Direction.EAST, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.STILL));
        locationsToTry.put(Direction.WEST, Arrays.asList(Direction.WEST, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.STILL));
        locationsToTry.put(Direction.SOUTH, Arrays.asList(Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.NORTH, Direction.STILL));
        locationsToTry.put(Direction.STILL, Arrays.asList(Direction.STILL, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.NORTH));
    }


    public Direction preventStackingStrength(LocationAndSite current, Direction direction, Map<Location, Site> sitesPerLocation) {

        Direction optimizedDir = direction;
        Site nextSite = sitesPerLocation.get(Constants.gameMap.getLocation(current.getLocation(), direction));
        Direction outOfTerritoryOption = null;
        if (nextSite.owner == Constants.myID) {
            int minLoss = Integer.MAX_VALUE;
            for (Direction dir : locationsToTry.get(direction)) {
                Location fallbackLocation = Constants.gameMap.getLocation(current.getLocation(), dir);
                Site fallbackSite = sitesPerLocation.get(fallbackLocation);
                boolean noExcess = !strengthPerLocation.containsKey(fallbackLocation)
                        || (strengthPerLocation.get(fallbackLocation) != 255
                        && strengthPerLocation.get(fallbackLocation) + current.getSite().strength < 255 + TOLERABLE_LOSS_ON_MERGE);
                if (fallbackSite.owner == Constants.myID) {
                    if (noExcess) {
                        optimizedDir = dir;
                        break;
                    } else {
                        if (strengthPerLocation.get(fallbackLocation) + current.getSite().strength < minLoss) {
                            minLoss = strengthPerLocation.get(fallbackLocation) + current.getSite().strength;
                            optimizedDir = dir;
                        }
                    }
                } else if (noExcess) {
                    outOfTerritoryOption = dir;
                }
            }
        } else {
            Location targetLocation = Constants.gameMap.getLocation(current.getLocation(), direction);
            boolean noExcess = !strengthPerLocation.containsKey(targetLocation)
                    || (strengthPerLocation.get(targetLocation) != 255
                    && strengthPerLocation.get(targetLocation) + current.getSite().strength < 255 + TOLERABLE_LOSS_ON_MERGE);
            if (!noExcess) {
                optimizedDir = Direction.STILL;
            }
        }

        if (outOfTerritoryOption != null && optimizedDir != direction) {
            optimizedDir = outOfTerritoryOption;
        }


        int strengthInTargetNextTurn;
        Location locationThatWillHaveMoreStrength;
        if (optimizedDir == Direction.STILL) {
            strengthInTargetNextTurn = current.getSite().strength + current.getSite().production;
            locationThatWillHaveMoreStrength = current.getLocation();
        } else {
            strengthInTargetNextTurn = current.getSite().strength;
            locationThatWillHaveMoreStrength = Constants.gameMap.getLocation(current.getLocation(), optimizedDir);
        }
        if (strengthPerLocation.containsKey(locationThatWillHaveMoreStrength)) {
            strengthInTargetNextTurn += strengthPerLocation.get(locationThatWillHaveMoreStrength);
        }
        strengthPerLocation.put(locationThatWillHaveMoreStrength, strengthInTargetNextTurn);
        return optimizedDir;
    }


}
