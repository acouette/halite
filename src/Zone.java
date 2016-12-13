import java.util.List;

/**
 * Created by acouette on 12/11/16.
 */
public class Zone {

    private List<LocationAndSite> locations;

    private int totalProduction;

    private double density;

    private int strength;

    public Zone(List<LocationAndSite> locations, int totalProduction, double density, int strength) {
        this.locations = locations;
        this.totalProduction = totalProduction;
        this.density = density;
        this.strength = strength;
    }

    public List<LocationAndSite> getLocations() {
        return locations;
    }

    public int getTotalProduction() {
        return totalProduction;
    }

    public double getDensity() {
        return density;
    }

    public int getStrength() {
        return strength;
    }


    @Override
    public String toString() {
        return "Zone{" +
                "locations=" + locations +
                ", totalProduction=" + totalProduction +
                ", density=" + density +
                ", strength=" + strength +
                '}';
    }
}
