import java.util.List;

/**
 * Created by acouette on 12/11/16.
 */
public class Zone {

    private List<Loc> locations;

    private double score;


    public Zone(List<Loc> locations, double score) {
        this.locations = locations;
        this.score = score;
    }

    public List<Loc> getLocations() {
        return locations;
    }

    public double getScore() {
        return score;
    }

    @Override
    public String toString() {
        return "Zone{" +
                "locations=" + locations +
                ", score=" + score +
                '}';
    }
}
