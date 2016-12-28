/**
 * Created by acouette on 12/18/16.
 */
public class AllTargetInfo {

    private LocationAndSite currentLocation;

    private LocationAndSite locationToTarget;

    private LocationAndSite nextLocation;

    private double cost;

    private Direction direction;

    public AllTargetInfo(LocationAndSite currentLocation, LocationAndSite locationToTarget, LocationAndSite nextLocation, double cost, Direction direction) {
        this.currentLocation = currentLocation;
        this.locationToTarget = locationToTarget;
        this.nextLocation = nextLocation;
        this.cost = cost;
        this.direction = direction;
    }

    public LocationAndSite getLocationToTarget() {
        return locationToTarget;
    }

    public LocationAndSite getNextLocation() {
        return nextLocation;
    }

    public double getCost() {
        return cost;
    }

    public Direction getDirection() {
        return direction;
    }

    public LocationAndSite getCurrentLocation() {
        return currentLocation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AllTargetInfo that = (AllTargetInfo) o;

        return currentLocation != null ? currentLocation.equals(that.currentLocation) : that.currentLocation == null;

    }

    @Override
    public int hashCode() {
        return currentLocation != null ? currentLocation.hashCode() : 0;
    }
}
