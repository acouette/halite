/**
 * Created by acouette on 12/11/16.
 */
public class LocationAndSite {

    private Location location;

    private Site site;

    public LocationAndSite(Location location, Site site) {
        this.location = location;
        this.site = site;
    }

    public Location getLocation() {
        return location;
    }

    public Site getSite() {
        return site;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LocationAndSite that = (LocationAndSite) o;

        return location.equals(that.location);

    }

    @Override
    public int hashCode() {
        return location.hashCode();
    }


    @Override
    public String toString() {
        return
                location.toString();
    }
}
