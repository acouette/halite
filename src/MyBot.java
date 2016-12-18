/**
 * Created by acouette on 12/15/16.
 */
public class MyBot {


    public static void main(String[] args) throws java.io.IOException {

        new CouettoBot("MyBot", 1.2f,
                (site, turn) -> site.strength >= site.production * 7,
                turn -> turn > 40 ? 0 : 10,
                30, false).run();

    }

}
