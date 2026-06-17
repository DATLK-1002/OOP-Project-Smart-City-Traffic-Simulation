package model.road;

import java.util.ArrayList;
import java.util.List;

public class Road {

    private String id;

    private String name;

    private List<Lane> lanes;

    private double length;

    public Road(String id,
                String name,
                double length) {

        this.id = id;
        this.name = name;
        this.length = length;

        this.lanes = new ArrayList<>();
    }

    public void addLane(Lane lane) {

        if(lane != null) {
            lanes.add(lane);
        }
    }

    public void removeLane(Lane lane) {

        lanes.remove(lane);
    }

    public List<Lane> getLanes() {

        return lanes;
    }

    public int getTotalVehicleCount() {

        int total = 0;

        for(Lane lane : lanes) {

            total += lane.getVehicleCount();
        }

        return total;
    }

    public String getId() {

        return id;
    }

    public String getName() {

        return name;
    }

    public double getLength() {

        return length;
    }
}