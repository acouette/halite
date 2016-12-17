/**
 * Created by acouette on 12/15/16.
 */
public class MyBot3 {


    public static void main(String[] args) throws java.io.IOException {

        new CouettoBot("MyBot3", 1.4f,
                (site, turn) -> site.strength >= (turn > 40 ? 40 : 16),
                turn -> turn > 40 ? 0 : 10,
                site -> {
                    if (site.owner == Constants.myID) {
                        return (double) 12;
                    } else if (site.owner == 0) {
                        return (double) site.strength / site.production;
                    } else {
                        return (double) 10;
                    }
                },
                30).run();

    }

}
