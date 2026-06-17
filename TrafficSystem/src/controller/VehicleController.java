package controller;

import java.util.ArrayList;
import java.util.List;

import model.vehicle.Vehicle;

public class VehicleController {

    private final List<Vehicle> vehicles;

    public VehicleController() {
        vehicles = new ArrayList<>();
    }

    public void addVehicle(Vehicle vehicle) {

        if(vehicle != null) {
            vehicles.add(vehicle);
        }
    }

    public void removeVehicle(Vehicle vehicle) {
        vehicles.remove(vehicle);
    }

    public List<Vehicle> getVehicles() {
        return vehicles;
    }

    public void updateVehicles() {

        for(Vehicle vehicle : vehicles) {
            vehicle.update();
        }
    }

    public int getVehicleCount() {
        return vehicles.size();
    }

    public void clearVehicles() {
        vehicles.clear();
    }
}