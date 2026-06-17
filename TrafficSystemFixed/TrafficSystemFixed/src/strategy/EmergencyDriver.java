package strategy;

import model.vehicle.Vehicle;

public class EmergencyDriver implements DrivingStrategy {

    @Override
    public void drive(Vehicle vehicle) {

        vehicle.accelerate();
        vehicle.accelerate();
        vehicle.accelerate();
    }
}
