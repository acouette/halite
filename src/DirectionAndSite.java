/**
 * Created by acouette on 12/9/16.
 */
public class DirectionAndSite {

    private final Direction direction;

    private final Site site;

    public DirectionAndSite(Direction direction, Site site) {
        this.direction = direction;
        this.site = site;
    }

    public Direction getDirection() {
        return direction;
    }

    public Site getSite() {
        return site;
    }
}
