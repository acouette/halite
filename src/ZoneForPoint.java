/**
 * Created by acouette on 12/11/16.
 */
public class ZoneForPoint {
    
    private Zone zone;
    
    private double score;

    public ZoneForPoint(Zone zone, double score) {
        this.zone = zone;
        this.score = score;
    }

    public Zone getZone() {
        return zone;
    }

    public double getScore() {
        return score;
    }
}
