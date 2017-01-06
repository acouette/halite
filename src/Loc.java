/**
 * Created by acouette on 12/11/16.
 */
public class Loc {

    private Location location;

    public int owner, strength, production;

    private boolean mine;

    private boolean enemy;

    private boolean neutral;

    private boolean emptyNeutral;

    private boolean nextEmptyNeutral;

    private double cost;

    private boolean suroundedByMine;

    public Loc(Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Loc that = (Loc) o;

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


    public boolean isMine() {
        return mine;
    }

    public void setMine(boolean mine) {
        this.mine = mine;
    }

    public boolean isEnemy() {
        return enemy;
    }

    public void setEnemy(boolean enemy) {
        this.enemy = enemy;
    }

    public boolean isNeutral() {
        return neutral;
    }

    public void setNeutral(boolean neutral) {
        this.neutral = neutral;
    }

    public boolean isEmptyNeutral() {
        return emptyNeutral;
    }

    public void setEmptyNeutral(boolean emptyNeutral) {
        this.emptyNeutral = emptyNeutral;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }


    public boolean hasNextEmptyNeutral() {
        return nextEmptyNeutral;
    }

    public void setHasNextEmptyNeutral(boolean hasNextEmptyNeutral) {
        this.nextEmptyNeutral = hasNextEmptyNeutral;
    }

    public boolean isSuroundedByMine() {
        return suroundedByMine;
    }

    public void setSuroundedByMine(boolean suroundedByMine) {
        this.suroundedByMine = suroundedByMine;
    }
}
