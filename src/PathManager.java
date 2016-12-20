import java.util.*;

/**
 * Created by acouette on 12/13/16.
 */
public class PathManager {


    Map<Location, Double> costPerLocation;


    public Map<Location, Vertex> getVertexMap(List<LocationAndSite> locationsAndSites, String name) {
        costPerLocation = new HashMap<>();

        Map<Location, Vertex> vertexMap = new HashMap<>();
        for (LocationAndSite locAndSites : locationsAndSites) {
            vertexMap.put(locAndSites.getLocation(), new Vertex(locAndSites));
            double cost;
            if (locAndSites.getSite().owner == Constants.myID) {
                cost = Constants.turn > 140 ? 16 : (Constants.turn > 60 ? 12 : 4);
            } else if (locAndSites.getSite().owner == 0) {
                cost = (double) locAndSites.getSite().strength / locAndSites.getSite().production;
            } else {
                cost = (double) 10;
            }

            costPerLocation.put(locAndSites.getLocation(), cost);
        }

        for (Vertex vertex : vertexMap.values()) {
            Edge[] edges = new Edge[4];
            int i = 0;
            for (Map.Entry<Direction, LocationAndSite> direction : Constants.DIRECTIONS.get(vertex.location.getLocation()).entrySet()) {
                if (direction.getKey() != Direction.STILL) {
                    edges[i++] = new Edge(vertexMap.get(direction.getValue().getLocation()), costPerLocation.get(direction.getValue().getLocation()));
                }
            }

            vertex.adjacencies = edges;
        }

        return vertexMap;
    }

    public void computePaths(Vertex source) {

        source.minDistance = 0;
        PriorityQueue<Vertex> vertexQueue = new PriorityQueue<>();
        vertexQueue.add(source);

        while (!vertexQueue.isEmpty()) {
            Vertex u = vertexQueue.poll();

            // Visit each edge exiting u
            for (Edge e : u.adjacencies) {
                Vertex v = e.target;
                double weight = e.weight;
                double distanceThroughU = u.minDistance + weight;
                if (distanceThroughU < v.minDistance) {
                    vertexQueue.remove(v);
                    v.minDistance = distanceThroughU;
                    v.previous = u;
                    vertexQueue.add(v);
                }
            }
        }
    }

    public List<LocationAndSite> getShortestPathTo(Vertex target) {
        List<LocationAndSite> path = new ArrayList<>();
        for (Vertex vertex = target; vertex != null; vertex = vertex.previous)
            path.add(vertex.location);

        Collections.reverse(path);
        return path;
    }


    public double getCostTo(Vertex target) {
        double cost = 0;
        for (Vertex vertex = target; vertex != null; vertex = vertex.previous)
            cost += costPerLocation.get(vertex.location.getLocation());
        return cost;
    }


}


class Vertex implements Comparable<Vertex> {
    public final LocationAndSite location;
    public Edge[] adjacencies;
    public double minDistance = Double.MAX_VALUE;
    public Vertex previous;

    public Vertex(LocationAndSite location) {
        this.location = location;
    }

    public String toString() {
        return location.toString();
    }

    public int compareTo(Vertex other) {
        return Double.compare(minDistance, other.minDistance);
    }

    public void reset() {
        minDistance = Double.MAX_VALUE;
        previous = null;
    }

}


class Edge {
    public final Vertex target;
    public final double weight;

    public Edge(Vertex argTarget, double argWeight) {
        target = argTarget;
        weight = argWeight;
    }
}