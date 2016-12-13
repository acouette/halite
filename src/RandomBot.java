import java.util.ArrayList;
import java.util.function.Predicate;

public class RandomBot {


    public static void main(String[] args) throws java.io.IOException {


        Constants.initConstants();

        Networking.sendInit("Opponent");


        int turn = 0;
        while (true) {
            ArrayList<Move> moves = new ArrayList<Move>();
            Constants.gameMap = Networking.getFrame();
            for (int y = 0; y < Constants.gameMap.height; y++) {
                for (int x = 0; x < Constants.gameMap.width; x++) {
                    Location currentLocation = new Location(x, y);
                    Site currentSite = Constants.gameMap.getSite(currentLocation);
                    if (currentSite.owner == Constants.myID) {
                        moves.add(new Move(currentLocation, getBestDirection(currentLocation, currentSite, turn)));
                    }
                }
            }
            Networking.sendFrame(moves);
            turn++;
        }
    }

    private static Direction getBestDirection(Location currentLocation, Site currentSite, int turn) {


        Direction onlyOurs = getOnlyOursDirection(currentLocation, currentSite);
        if (onlyOurs != null) return onlyOurs;


        Direction fight = findCloseHostile(currentLocation);
        if (fight != null) return fight;


        DirectionAndSite bestValue = Utils.getBestValueAround(currentLocation);
        if (bestValue.getSite().strength <= currentSite.strength) {
            return bestValue.getDirection();
        }

        Direction earlyGameFeedNeighbour = getFeedNeighbour(currentLocation, currentSite, turn);
        if (earlyGameFeedNeighbour != null) return earlyGameFeedNeighbour;


        return Direction.STILL;
    }


    private static Direction getFeedNeighbour(Location currentLocation, Site currentSite, int turn) {

        int minimumStrengthToHelp = 20;

        if (turn < 40
                && currentSite.strength >= minimumStrengthToHelp
                && Boolean.TRUE == isWeaker(currentLocation, currentSite)) {
            for (Direction targetDirection : Direction.CARDINALS) {
                Site targetSite = Constants.gameMap.getSite(currentLocation, targetDirection);
                if (targetSite.owner == Constants.myID) {
                    return targetDirection;
                }
            }
        }
        return null;

    }

    private static Boolean isWeaker(Location currentLocation, Site currentSite) {

        boolean foundOthers = false;
        for (int x = 0; x < Constants.gameMap.width; x++) {
            for (int y = 0; y < Constants.gameMap.height; y++) {
                if (currentLocation.x != x || currentLocation.y != y) {
                    Site currentScannedSite = Constants.gameMap.getSite(new Location(x, y));
                    if (currentScannedSite.owner == Constants.myID) {
                        foundOthers = true;
                        if (currentScannedSite.strength < currentSite.strength) {
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
            double angle = Constants.gameMap.getAngle(currentLocation, minDistanceLocation);
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

        } else {
            return null;
        }


    }


}