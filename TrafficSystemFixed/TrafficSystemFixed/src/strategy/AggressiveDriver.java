package strategy;

import model.vehicle.Vehicle;

public class AggressiveDriver implements DrivingStrategy {

    @Override
    public void drive(Vehicle vehicle) {

        vehicle.accelerate();
        vehicle.accelerate();
    }
}
