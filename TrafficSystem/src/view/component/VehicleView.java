package view.component;

import model.vehicle.Vehicle;

public class VehicleView {

    private Vehicle vehicle;

    public VehicleView(
            Vehicle vehicle) {

        this.vehicle = vehicle;
    }

    public void draw() {

        System.out.println(
                "Draw Vehicle: "
                + vehicle.getId()
        );
    }
}