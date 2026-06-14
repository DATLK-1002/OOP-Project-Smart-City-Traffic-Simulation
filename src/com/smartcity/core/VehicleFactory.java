package com.smartcity.core;


public class VehicleFactory {

    public Object createVehicle(
            String type) {

        switch (type.toUpperCase()) {

            case "CAR":
                return new Object();

            case "MOTORBIKE":
                return new Object();

            case "BICYCLE":
                return new Object();

            case "AMBULANCE":
                return new Object();

            case "FIRETRUCK":
                return new Object();

            default:
                throw new IllegalArgumentException(
                        "Unknown vehicle type: "
                                + type);
        }
    }
}