

import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by acouette on 12/8/16.
 */
public class Logger {



    static void log(String message){
        try {
            FileWriter fileWriter= new FileWriter("/home/acouette/halite/halite.custom.log", true);
            fileWriter.append(message);
            fileWriter.append("\n");
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }


}
