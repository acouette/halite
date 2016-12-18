/**
 * Created by acouette on 12/18/16.
 */
public class AllTargetInfo {

    private Location currentLocation;

    private Location locationToTarget;

    private Location nextLocation;

    private double cost;

    private Direction direction;

    public AllTargetInfo(Location currentLocation, Location locationToTarget, Location nextLocation, double cost, Direction direction) {
        this.currentLocation = currentLocation;
        this.locationToTarget = locationToTarget;
        this.nextLocation = nextLocation;
        this.cost = cost;
        this.direction = direction;
    }

    public Location getLocationToTarget() {
        return locationToTarget;
    }

    public Location getNextLocation() {
        return nextLocation;
    }

    public double getCost() {
        return cost;
    }

    public Direction getDirection() {
        return direction;
    }

    public Location getCurrentLocation() {
        return currentLocation;
    }
}
