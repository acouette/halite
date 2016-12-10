/**
 * Created by acouette on 12/9/16.
 */
public class Constants {


    static InitPackage iPackage;
    static int myID;

    static GameMap gameMap;


    static void initConstants() {
        iPackage = Networking.getInit();
        myID = iPackage.myID;
        gameMap = iPackage.map;
    }


}
