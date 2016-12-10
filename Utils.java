class Utils {


    static DirectionAndSite getBestValueAround(Location currentLocation) {
        Direction bestValueDirection = null;
        Site bestValueSite = null;
        float bestValue = -1;

        for (Direction targetDirection : Direction.CARDINALS) {
            Site targetSite = Constants.gameMap.getSite(currentLocation, targetDirection);
            if (targetSite.owner != Constants.myID) {
                if (targetSite.strength == 0 || ((float) targetSite.production / targetSite.strength) > bestValue) {
                    if (targetSite.strength == 0) {
                        bestValue = Float.MAX_VALUE;
                    } else {
                        bestValue = (float) targetSite.production / targetSite.strength;
                    }
                    bestValueDirection = targetDirection;
                    bestValueSite = targetSite;
                }
            }
        }

        return new DirectionAndSite(bestValueDirection, bestValueSite);
    }


    static Direction getDirection(Location origin, Location destination) {
        double angle = Constants.gameMap.getAngle(origin, destination);
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


    static int getTotalOwnedCells() {
        int totalCells = 0;
        for (int y = 0; y < Constants.gameMap.height; y++) {
            for (int x = 0; x < Constants.gameMap.width; x++) {
                Site targetSite = Constants.gameMap.getSite(new Location(x, y));
                if (targetSite.owner == Constants.myID) {
                    totalCells++;
                }

            }
        }
        return totalCells;
    }





}
