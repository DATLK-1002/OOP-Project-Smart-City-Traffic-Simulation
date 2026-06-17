package model.road;

import java.util.ArrayList;
import java.util.List;

import model.vehicle.Vehicle;
import util.Direction;

public class Lane {

    private String id;

    private Direction direction;

    private double speedLimit;

    private double width;

    private double length;

    private List<Vehicle> vehicles;

    public Lane(String id,
                Direction direction,
                double speedLimit,
                double width,
                double length) {

        this.id = id;
        this.direction = direction;
        this.speedLimit = speedLimit;
        this.width = width;
        this.length = length;

        vehicles = new ArrayList<>();
    }

    public void addVehicle(Vehicle vehicle) {

        if(vehicle != null &&
           !vehicles.contains(vehicle)) {

            vehicles.add(vehicle);
        }
    }

    public void removeVehicle(Vehicle vehicle) {

        vehicles.remove(vehicle);
    }

    public List<Vehicle> getVehicles() {

        return new ArrayList<>(vehicles);
    }

    public int getVehicleCount() {

        return vehicles.size();
    }

    public boolean isEmpty() {

        return vehicles.isEmpty();
    }

    public Vehicle getFrontVehicle(
            Vehicle currentVehicle) {

        int index =
                vehicles.indexOf(currentVehicle);

        if(index > 0) {

            return vehicles.get(index - 1);
        }

        return null;
    }

    public double getCongestionLevel() {

        double maxVehicles =
                length / 50.0;

        return Math.min(
                1.0,
                vehicles.size() / maxVehicles
        );
    }

    public String getId() {

        return id;
    }

    public Direction getDirection() {

        return direction;
    }

    public double getSpeedLimit() {

        return speedLimit;
    }

    public void setSpeedLimit(
            double speedLimit) {

        this.speedLimit =
                Math.max(0, speedLimit);
    }

    public double getWidth() {

        return width;
    }

    public double getLength() {

        return length;
    }

    @Override
    public String toString() {

        return String.format(
                "Lane[id=%s, direction=%s, vehicles=%d]",
                id,
                direction,
                vehicles.size()
        );
    }
}