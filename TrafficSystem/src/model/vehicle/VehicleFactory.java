package model.vehicle;

import util.Direction;
import util.Vector2D;

public class VehicleFactory {

    public static Vehicle createVehicle(
            String type,
            String id,
            Vector2D position,
            Direction direction) {

        switch(type.toUpperCase()) {

            case "CAR":
                return new Car(id, position, direction);

            case "MOTORBIKE":
                return new Motorbike(id, position, direction);

            case "BICYCLE":
                return new Bicycle(id, position, direction);

            case "BUS":
                return new Bus(id, position, direction);

            case "AMBULANCE":
                return new Ambulance(id, position, direction);

            case "FIRETRUCK":
                return new FireTruck(id, position, direction);

            default:
                throw new IllegalArgumentException(
                        "Unknown vehicle type: " + type);
        }
    }
}