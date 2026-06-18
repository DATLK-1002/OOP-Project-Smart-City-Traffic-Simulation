package model.junction;

import java.util.ArrayList;
import java.util.List;

import model.road.Road;
import util.Vector2D;

public abstract class Junction {

    protected String id;

    protected Vector2D position;

    protected List<Road> connectedRoads;

    public Junction(String id,
                    Vector2D position) {

        this.id = id;
        this.position = position;

        this.connectedRoads =
                new ArrayList<>();
    }

    public void addRoad(Road road) {

        if(road != null &&
                !connectedRoads.contains(road)) {

            connectedRoads.add(road);
        }
    }

    public List<Road> getConnectedRoads() {

        return connectedRoads;
    }

    public abstract int getMaxRoadCount();

    public String getId() {
        return id;
    }

    public Vector2D getPosition() {
        return position;
    }
}