package view.component;

import model.road.Road;

public class RoadView {

    private Road road;

    public RoadView(Road road) {

        this.road = road;
    }

    public void render() {

        System.out.println("Road: " + road.getName()
                + " | Vehicles: " + road.getTotalVehicleCount());
    }

    public Road getRoad() {

        return road;
    }
}
