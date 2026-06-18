package util;

import model.vehicle.Vehicle;

public class CollisionDetector {

    private static final double COLLISION_DISTANCE = 28.0;

    public boolean isColliding(
            Vehicle vehicleA,
            Vehicle vehicleB) {

        double distance = vehicleA.getPosition()
                .distance(vehicleB.getPosition());

        return distance < COLLISION_DISTANCE;
    }

    public boolean isNearCollision(
            Vehicle vehicleA,
            Vehicle vehicleB) {

        double distance = vehicleA.getPosition()
                .distance(vehicleB.getPosition());

        return distance < Constants.SAFE_DISTANCE;
    }
}
