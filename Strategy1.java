import java.util.ArrayList;

/**
 * Created by acouette on 12/9/16.
 */
public class Strategy1 {

    public ArrayList<Move> run() {

        ArrayList<Move> moves = new ArrayList<>();
        for (int y = 0; y < Constants.gameMap.height; y++) {
            for (int x = 0; x < Constants.gameMap.width; x++) {
                Location currentLocation = new Location(x, y);
                Site currentSite = Constants.gameMap.getSite(currentLocation);
                if (currentSite.owner == Constants.myID) {
                    Direction direction;
                    DirectionAndSite bestValue = Utils.getBestValueAround(currentLocation);
                    if (bestValue.getSite().strength < currentSite.strength) {
                        direction = bestValue.getDirection();
                    } else {
                        direction = Direction.STILL;
                    }
                    moves.add(new Move(currentLocation, direction));
                }
            }
        }
        return moves;
    }


}
