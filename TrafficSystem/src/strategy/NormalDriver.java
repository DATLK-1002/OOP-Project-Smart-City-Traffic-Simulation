package strategy;

import model.vehicle.Vehicle;
import model.vehicle.VehicleState;

public class NormalDriver implements DrivingStrategy {

    @Override
    public void drive(Vehicle vehicle) {

        if(vehicle.getState() == VehicleState.STOPPED) {
            vehicle.accelerate();
        }

        vehicle.accelerate();
    }
}