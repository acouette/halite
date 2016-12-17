import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by acouette on 12/17/16.
 */
public class NextTurnState {

    private final Map<Location, Integer> strengthPerLocation = new HashMap<>();

    private final static int TOLERABLE_LOSS = 30;

    private static Map<Direction, List<Direction>> locationsToTry = new HashMap<>();

    static {
        locationsToTry.put(Direction.NORTH, Arrays.asList(Direction.NORTH, Direction.STILL, Direction.EAST, Direction.WEST, Direction.SOUTH));
        locationsToTry.put(Direction.EAST, Arrays.asList(Direction.EAST, Direction.STILL, Direction.NORTH, Direction.SOUTH, Direction.WEST));
        locationsToTry.put(Direction.WEST, Arrays.asList(Direction.WEST, Direction.STILL, Direction.NORTH, Direction.SOUTH, Direction.EAST));
        locationsToTry.put(Direction.SOUTH, Arrays.asList(Direction.SOUTH, Direction.STILL, Direction.EAST, Direction.WEST, Direction.NORTH));
        locationsToTry.put(Direction.STILL, Arrays.asList(Direction.STILL, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.NORTH));
    }


    public Direction preventStackingStrength(Location currentLocation, Site currentSite, Direction direction, Map<Location, Site> sitesPerLocation) {

        Direction optimizedDir = direction;
        Location targetLocation = Constants.gameMap.getLocation(currentLocation, direction);
        Site targetSite = sitesPerLocation.get(targetLocation);
        if (targetSite.owner == Constants.myID) {
            int minLoss = Integer.MAX_VALUE;
            for (Direction dir : locationsToTry.get(direction)) {
                Location fallbackLocation = Constants.gameMap.getLocation(currentLocation, dir);
                Site fallbackSite = sitesPerLocation.get(fallbackLocation);
                if (fallbackSite.owner == Constants.myID) {
                    if (!strengthPerLocation.containsKey(fallbackLocation) || strengthPerLocation.get(fallbackLocation) + currentSite.strength < 255 + TOLERABLE_LOSS) {
                        optimizedDir = dir;
                        break;
                    } else {
                        if (strengthPerLocation.get(fallbackLocation) + currentSite.strength < minLoss) {
                            minLoss = strengthPerLocation.get(fallbackLocation) + currentSite.strength;
                            optimizedDir = dir;
                        }
                    }
                }
            }
        }


        int strengthInTargetNextTurn;
        Location locationThatWillHaveMoreStrength;
        if (optimizedDir == Direction.STILL) {
            strengthInTargetNextTurn = currentSite.strength + currentSite.production;
            locationThatWillHaveMoreStrength = currentLocation;
        } else {
            strengthInTargetNextTurn = currentSite.strength;
            locationThatWillHaveMoreStrength = Constants.gameMap.getLocation(currentLocation, optimizedDir);
        }
        if (strengthPerLocation.containsKey(locationThatWillHaveMoreStrength)) {
            strengthInTargetNextTurn += strengthPerLocation.get(locationThatWillHaveMoreStrength);
        }
        strengthPerLocation.put(locationThatWillHaveMoreStrength, strengthInTargetNextTurn);
        return optimizedDir;
    }


}
