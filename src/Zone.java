import java.util.List;

/**
 * Created by acouette on 12/11/16.
 */
public class Zone {

    private List<LocationAndSite> locations;

    private double score;


    public Zone(List<LocationAndSite> locations, double score) {
        this.locations = locations;
        this.score = score;
    }

    public List<LocationAndSite> getLocations() {
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
