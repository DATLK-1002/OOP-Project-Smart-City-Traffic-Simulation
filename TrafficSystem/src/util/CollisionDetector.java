package util;

import model.vehicle.Vehicle;

public class CollisionDetector {

    private static final double SAFE_DISTANCE = 20.0;

    public static boolean isCollision(
            Vehicle v1,
            Vehicle v2) {

        return v1.getPosition()
                .distance(v2.getPosition())
                < SAFE_DISTANCE;
    }

    public static double distance(
            Vehicle v1,
            Vehicle v2) {

        return v1.getPosition()
                .distance(v2.getPosition());
    }
}