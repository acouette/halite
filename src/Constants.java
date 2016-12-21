import java.util.HashMap;
import java.util.Map;

/**
 * Created by acouette on 12/9/16.
 */
public class Constants {


    static double AVERAGE_CELL_COST;
    static InitPackage iPackage;
    static int myID;
    static GameMap gameMap;
    static int turn;
    static final Map<Location, Map<Direction, LocationAndSite>> DIRECTIONS = new HashMap<>();


    static void initConstants() {
        turn = 0;
        iPackage = Networking.getInit();
        myID = iPackage.myID;
        gameMap = iPackage.map;
    }


}
