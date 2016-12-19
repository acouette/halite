/**
 * Created by acouette on 12/9/16.
 */
public class Constants {


    static InitPackage iPackage;
    static int myID;
    static GameMap gameMap;
    static int turn;

    static void initConstants() {
        turn = 0;
        iPackage = Networking.getInit();
        myID = iPackage.myID;
        gameMap = iPackage.map;
    }


}
