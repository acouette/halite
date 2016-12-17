public class MyBot2 {


    public static void main(String[] args) throws java.io.IOException {

        new CouettoBot("MyBot2", 1.2f,
                (site, turn) -> site.strength >= (turn > 40 ? site.production * 7 : site.production * 5),
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