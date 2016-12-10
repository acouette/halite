/**
 * Created by acouette on 12/10/16.
 */
public class RankedLocation {


    private Location location;

    private int ranking;

    public RankedLocation(Location location, int ranking) {
        this.location = location;
        this.ranking = ranking;
    }

    public Location getLocation() {
        return location;
    }

    public int getRanking() {
        return ranking;
    }
}
