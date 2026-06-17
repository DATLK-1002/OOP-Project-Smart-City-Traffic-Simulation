package view.component;

import model.vehicle.Vehicle;

public class VehicleView {

    private Vehicle vehicle;

    public VehicleView(Vehicle vehicle) {

        this.vehicle = vehicle;
    }

    public void render() {

        System.out.println("Vehicle [" + vehicle.getId()
                + "] pos=" + vehicle.getPosition().getX()
                + "," + vehicle.getPosition().getY()
                + " speed=" + vehicle.getSpeed());
    }

    public Vehicle getVehicle() {

        return vehicle;
    }
}
