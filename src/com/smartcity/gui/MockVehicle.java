package com.smartcity.gui;

import com.smartcity.traffic.road.Lane;
import com.smartcity.traffic.road.Location;
import com.smartcity.traffic.road.MapManager;
import com.smartcity.traffic.road.Road;

public class MockVehicle {
    private String id;
    private String type; // "Car", "Motorbike", "Bicycle", "Ambulance", "FireTruck"
    private Lane lane;
    private double positionOnLane; // distance in pixels from start
    private double speed; // current speed in pixels/s
    private double maxSpeed; // target speed in pixels/s
    
    private double acceleration = 150.0; // px/s^2
    private double deceleration = 300.0; // px/s^2
    private double safetyDistance = 60.0; // px

    private double x;
    private double y;
    private double angle;

    public MockVehicle(String id, String type, Lane lane) {
        this.id = id;
        this.type = type;
        this.lane = lane;
        this.positionOnLane = 0.0;
        
        // Assign speeds based on vehicle type
        switch (type) {
            case "Ambulance":
            case "FireTruck":
                this.maxSpeed = 160.0 + Math.random() * 40.0; // fast
                break;
            case "Car":
                this.maxSpeed = 120.0 + Math.random() * 30.0;
                break;
            case "Motorbike":
                this.maxSpeed = 100.0 + Math.random() * 20.0;
                break;
            case "Bicycle":
                this.maxSpeed = 40.0 + Math.random() * 10.0; // slow
                this.acceleration = 50.0;
                this.deceleration = 150.0;
                this.safetyDistance = 35.0;
                break;
            default:
                this.maxSpeed = 100.0;
        }
        this.speed = this.maxSpeed * 0.5; // start at half speed
        updateCoordinates();
    }

    public void update(double deltaTime, double distanceToVehicleAhead, boolean isLightRedOrYellow) {
        // Simple car-following and traffic light braking logic
        double targetSpeed = maxSpeed;

        // 1. Check leading vehicle
        if (distanceToVehicleAhead >= 0 && distanceToVehicleAhead < safetyDistance) {
            // Decelerate or stop to maintain safety distance
            double ratio = distanceToVehicleAhead / safetyDistance;
            targetSpeed = maxSpeed * Math.max(0.0, ratio - 0.2);
        }

        // 2. Check traffic light at the end of the lane
        double distanceToJunction = lane.getLength() - positionOnLane;
        if (isLightRedOrYellow && distanceToJunction < 100.0 && distanceToJunction > 0.0) {
            // Smoothly slow down to stop at the stop line (5px before the junction)
            double stopDistance = Math.max(5.0, distanceToJunction - 15.0);
            double stopRatio = stopDistance / 85.0; // normalized over the active braking zone
            targetSpeed = Math.min(targetSpeed, maxSpeed * Math.max(0.0, stopRatio));
        }

        // Apply acceleration/deceleration
        if (speed < targetSpeed) {
            speed = Math.min(targetSpeed, speed + acceleration * deltaTime);
        } else if (speed > targetSpeed) {
            speed = Math.max(targetSpeed, speed - deceleration * deltaTime);
        }

        // Move vehicle
        positionOnLane += speed * deltaTime;
        if (positionOnLane > lane.getLength()) {
            positionOnLane = lane.getLength();
        }

        updateCoordinates();
    }

    private void updateCoordinates() {
        Road road = getRoadForLane(lane);
        if (road == null) return;

        Location start = road.getStartLocation();
        Location end = road.getEndLocation();

        double x1 = start.getX();
        double y1 = start.getY();
        double x2 = end.getX();
        double y2 = end.getY();

        double dx = x2 - x1;
        double dy = y2 - y1;
        double roadLen = Math.sqrt(dx * dx + dy * dy);
        if (roadLen == 0) return;

        double ux = dx / roadLen;
        double uy = dy / roadLen;

        // Check if lane direction is opposite to road vector direction
        boolean reverse = false;
        if (Math.abs(dx) > Math.abs(dy)) {
            // Mostly horizontal
            if (lane.getDirection() == Lane.Direction.WEST && dx > 0) reverse = true;
            if (lane.getDirection() == Lane.Direction.EAST && dx < 0) reverse = true;
        } else {
            // Mostly vertical
            if (lane.getDirection() == Lane.Direction.NORTH && dy > 0) reverse = true;
            if (lane.getDirection() == Lane.Direction.SOUTH && dy < 0) reverse = true;
        }

        if (reverse) {
            double tempX = x1; double tempY = y1;
            x1 = x2; y1 = y2;
            x2 = tempX; y2 = tempY;
            dx = -dx; dy = -dy;
            ux = -ux; uy = -uy;
        }

        // Perpendicular vector for lane offset (pointing right of heading)
        double px = -uy;
        double py = ux;

        // Perpendicular offset based on lane index
        int numLanes = road.getLaneCount();
        int idx = road.getLanes().indexOf(lane);
        double laneWidth = road.getWidth() / Math.max(1, numLanes);
        double offset = (idx - (numLanes - 1) / 2.0) * laneWidth;

        // Center line coordinates of this lane
        double laneStartX = x1 + px * offset;
        double laneStartY = y1 + py * offset;
        double laneEndX = x2 + px * offset;
        double laneEndY = y2 + py * offset;

        // Interpolate along the lane center line
        double t = positionOnLane / Math.max(1.0, lane.getLength());
        this.x = laneStartX + t * (laneEndX - laneStartX);
        this.y = laneStartY + t * (laneEndY - laneStartY);

        // Heading angle in degrees
        this.angle = Math.toDegrees(Math.atan2(dy, dx));
    }

    private Road getRoadForLane(Lane lane) {
        for (Road road : MapManager.getInstance().getRoads()) {
            if (road.getLanes().contains(lane)) {
                return road;
            }
        }
        return null;
    }

    // Getters and Setters
    public String getId() { return id; }
    public String getType() { return type; }
    public Lane getLane() { return lane; }
    public void setLane(Lane lane) { this.lane = lane; }
    public double getPositionOnLane() { return positionOnLane; }
    public void setPositionOnLane(double pos) { this.positionOnLane = pos; }
    public double getSpeed() { return speed; }
    public double getMaxSpeed() { return maxSpeed; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getAngle() { return angle; }
    public boolean isFinished() { return positionOnLane >= lane.getLength(); }
}
