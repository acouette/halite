import java.util.List;

/**
 * Created by acouette on 12/11/16.
 */
public class Zone {

    private List<LocationAndSite> locations;


    private double averageProduction;

    private double averageStrength;

    public Zone(List<LocationAndSite> locations, double averageProduction, double averageStrength) {
        this.locations = locations;
        this.averageProduction = averageProduction;
        this.averageStrength = averageStrength;
    }

    public List<LocationAndSite> getLocations() {
        return locations;
    }

    public double getAverageProduction() {
        return averageProduction;
    }

    public double getAverageStrength() {
        return averageStrength;
    }
}
