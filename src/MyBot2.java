public class MyBot2 {


    public static void main(String[] args) throws java.io.IOException {

        new CouettoBot("MyBot2", 1.2f,
                (site) -> site.strength >= site.production * 6,
                () -> Constants.turn > 40 ? 0 : 10,
                30).run();

    }

}