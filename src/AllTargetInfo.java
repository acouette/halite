/**
 * Created by acouette on 12/18/16.
 */
public class AllTargetInfo {

    private Loc currentLocation;

    private Loc locationToTarget;

    private Loc nextLocation;

    private double cost;

    private Direction direction;

    public AllTargetInfo(Loc currentLocation, Loc locationToTarget, Loc nextLocation, double cost, Direction direction) {
        this.currentLocation = currentLocation;
        this.locationToTarget = locationToTarget;
        this.nextLocation = nextLocation;
        this.cost = cost;
        this.direction = direction;
    }

    public Loc getLocationToTarget() {
        return locationToTarget;
    }

    public Loc getNextLocation() {
        return nextLocation;
    }

    public double getCost() {
        return cost;
    }

    public Direction getDirection() {
        return direction;
    }

    public Loc getCurrentLocation() {
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
