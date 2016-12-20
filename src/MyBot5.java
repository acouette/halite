/**
 * Created by acouette on 12/15/16.
 */
public class MyBot5 {


    public static void main(String[] args) throws java.io.IOException {

        new CouettoBot("MyBot5", 1.2f,
                (site) -> site.strength >= site.production * 5,
                () -> Constants.turn > 40 ? 0 : 10,
                30).run();

    }

}
