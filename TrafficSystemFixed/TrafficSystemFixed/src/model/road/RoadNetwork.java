package model.road;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import model.junction.Junction;

public class RoadNetwork {

    private List<Road> roads;

    private List<Junction> junctions;

    public RoadNetwork() {

        roads = new ArrayList<>();

        junctions = new ArrayList<>();
    }

    public void addRoad(Road road) {

        if(road != null) {

            roads.add(road);
        }
    }

    public void addJunction(
            Junction junction) {

        if(junction != null) {

            junctions.add(junction);
        }
    }

    public List<Road> getRoads() {

        return Collections.unmodifiableList(roads);
    }

    public List<Junction> getJunctions() {

        return Collections.unmodifiableList(junctions);
    }

    public int getTotalVehicleCount() {

        int total = 0;

        for(Road road : roads) {

            total += road.getTotalVehicleCount();
        }

        return total;
    }
}