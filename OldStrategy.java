import java.util.ArrayList;
import java.util.function.Predicate;


public class OldStrategy {

    public ArrayList<Move> run() {
        ArrayList<Move> moves = new ArrayList<>();
        for (int y = 0; y < Constants.gameMap.height; y++) {
            for (int x = 0; x < Constants.gameMap.width; x++) {
                Location currentLocation = new Location(x, y);
                Site currentSite = Constants.gameMap.getSite(currentLocation);
                if (currentSite.owner == Constants.myID) {
                    Direction direction = getBestDirection(currentLocation, currentSite);


                    moves.add(new Move(currentLocation, direction));
                }
            }
        }
        return moves;
    }


    private static Direction getBestDirection(Location currentLocation, Site currentSite) {


        Direction onlyOurs = getOnlyOursDirection(currentLocation, currentSite);
        if (onlyOurs != null) return onlyOurs;


        Direction fight = findCloseHostile(currentLocation);
        if (fight != null) return fight;


        DirectionAndSite bestValue = Utils.getBestValueAround(currentLocation);
        if (bestValue.getSite().strength < currentSite.strength) {
            return bestValue.getDirection();
        }
//
//        Direction earlyGameFeedNeighbour = getFeedNeighbour(currentLocation, currentSite, turn);
//        if (earlyGameFeedNeighbour != null) return earlyGameFeedNeighbour;


        return Direction.STILL;
    }


//    private static Direction getFeedNeighbour(Location currentLocation, Site currentSite, int turn) {
//
//        int minimumStrengthToHelp = 20;
//
//        if (turn < 40
//                && currentSite.strength >= minimumStrengthToHelp
//                && Boolean.TRUE == isWeaker(currentLocation, currentSite)) {
//            for (Direction targetDirection : Direction.CARDINALS) {
//                Site targetSite = Constants.gameMap.getSite(currentLocation, targetDirection);
//                if (targetSite.owner == Constants.myID) {
//                    return targetDirection;
//                }
//            }
//        }
//        return null;
//
//    }

    private static Boolean isWeaker(Location currentLocation, Site currentSite) {

        boolean foundOthers = false;
        for (int x = 0; x < Constants.gameMap.width; x++) {
            for (int y = 0; y < Constants.gameMap.height; y++) {
                if (currentLocation.x != x || currentLocation.y != y) {
                    Site currentScannedSite = Constants.gameMap.getSite(new Location(x, y));
                    if (currentScannedSite.owner == Constants.myID) {
                        foundOthers = true;
                        if (currentScannedSite.strength <= currentSite.strength) {
                            return false;
                        }
                    }
                }
            }
        }

        if (foundOthers) {
            return true;
        }
        return null;
    }


    private static Direction getOnlyOursDirection(Location currentLocation, Site currentSite) {

        int onlyOursCells = 0;
        for (Direction targetDirection : Direction.CARDINALS) {
            Site targetSite = Constants.gameMap.getSite(currentLocation, targetDirection);
            if (targetSite.owner == Constants.myID) {
                onlyOursCells++;
            }
        }


        if (onlyOursCells == 4) {
            if (currentSite.strength > 30) {
                return findClosestNotOwnedCell(currentLocation);
            } else {
                return Direction.STILL;
            }
        }
        return null;
    }

    private static Direction findCloseHostile(Location currentLocation) {
        return findClosest(currentLocation, owner -> owner != Constants.myID && owner != 0, 3);
    }

    private static Direction findClosestNotOwnedCell(Location currentLocation) {
        return findClosest(currentLocation, owner -> owner != Constants.myID, null);
    }


    private static Direction findClosest(Location currentLocation, Predicate<Integer> ownershipCondition, Integer minDistanceAllowed) {

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
            return Utils.getDirection(currentLocation, minDistanceLocation);

        } else {
            return null;
        }


    }

}
