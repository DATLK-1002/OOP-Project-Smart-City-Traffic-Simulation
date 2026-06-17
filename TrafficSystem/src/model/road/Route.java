package model.road;

import java.util.ArrayList;
import java.util.List;

public class Route {

    private String id;

    private List<Road> roads;

    public Route(String id) {

        this.id = id;

        roads = new ArrayList<>();
    }

    public void addRoad(Road road) {

        if(road != null) {

            roads.add(road);
        }
    }

    public List<Road> getRoads() {

        return roads;
    }

    public String getId() {

        return id;
    }

    public int getRoadCount() {

        return roads.size();
    }
}