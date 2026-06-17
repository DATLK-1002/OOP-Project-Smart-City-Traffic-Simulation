package controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import model.trafficlight.TrafficLight;
import model.vehicle.Ambulance;
import model.vehicle.Bicycle;
import model.vehicle.FireTruck;
import model.vehicle.Motorbike;
import model.vehicle.Vehicle;
import model.vehicle.VehicleState;
import util.CollisionDetector;
import util.Direction;
import util.SoundManager;

public class VehicleController {

    private enum TurnChoice {
        STRAIGHT,
        LEFT,
        RIGHT
    }

    private static class PendingTurn {
        private final Direction targetDirection;
        private final double targetX;
        private final double targetY;
        private int remainingSteps;

        private PendingTurn(Direction targetDirection, double targetX, double targetY) {
            this.targetDirection = targetDirection;
            this.targetX = targetX;
            this.targetY = targetY;
            this.remainingSteps = 18;
        }
    }

    private static final double SAFE_DISTANCE = 64;
    private static final double EMERGENCY_YIELD_DISTANCE = 130;
    private static final double CENTER_X = 450;
    private static final double CENTER_Y = 350;
    private static final double STOP_LINE_OFFSET = 125;
    private static final double RIGHT_LANE_OFFSET = 52.5;
    private static final double LANE_KEEP_MARGIN = 12;
    private static final double OUTER_LANE_OFFSET = 30;
    private static final double MIDDLE_LANE_OFFSET = 55;
    private static final double INNER_LANE_OFFSET = 82;

    private final List<Vehicle> vehicles;
    private final CollisionDetector collisionDetector;
    private final SoundManager soundManager;
    private final List<Vehicle> blockedVehicles;
    private final Map<Vehicle, TurnChoice> turnChoices;
    private final Map<Vehicle, PendingTurn> turningVehicles;
    private final List<Vehicle> vehiclesThatTurned;
    private final Random random;
    private List<TrafficLight> trafficLights;

    public VehicleController() {
        vehicles = new ArrayList<>();
        collisionDetector = new CollisionDetector();
        soundManager = new SoundManager();
        blockedVehicles = new ArrayList<>();
        turnChoices = new HashMap<>();
        turningVehicles = new HashMap<>();
        vehiclesThatTurned = new ArrayList<>();
        random = new Random();
        trafficLights = Collections.emptyList();
    }

    public void addVehicle(Vehicle vehicle) {


        if (vehicle != null && !vehicles.contains(vehicle)) {
            vehicles.add(vehicle);
        }
    }

    public void removeVehicle(Vehicle vehicle) {

        vehicles.remove(vehicle);
        turnChoices.remove(vehicle);
        turningVehicles.remove(vehicle);
        vehiclesThatTurned.remove(vehicle);
    }

    public List<Vehicle> getVehicles() {


        return Collections.unmodifiableList(vehicles);
    }

    public void setTrafficLights(List<TrafficLight> trafficLights) {
        this.trafficLights = trafficLights == null ? Collections.emptyList() : trafficLights;
    }

    public void updateVehicles() {
        blockedVehicles.clear();
        applyTrafficRules();

        for (Vehicle vehicle : new ArrayList<>(vehicles)) {
            if (!blockedVehicles.contains(vehicle)) {
                if (turningVehicles.containsKey(vehicle)) {
                    advanceTurn(vehicle);
                } else {
                    vehicle.update();
                    applyTurnIfNeeded(vehicle);
                    keepVehicleOnRoad(vehicle);
                }
            }
        }

        preventCollisions();
        removeVehiclesOutsideMap();
    }

    private void applyTrafficRules() {
        for (Vehicle vehicle : vehicles) {
            if (isEmergency(vehicle)) {
                soundManager.playSiren();
                continue;
            }

            if (mustStopAtRedLight(vehicle)) {
                vehicle.stop();
                blockVehicle(vehicle);
                continue;
            }

            slowWhenApproachingIntersection(vehicle);

            Vehicle frontVehicle = findNearestFrontVehicle(vehicle);
            if (frontVehicle != null && distanceInFront(vehicle, frontVehicle) < SAFE_DISTANCE) {
                handleTooCloseFrontVehicle(vehicle, frontVehicle);
            }

            if (shouldYieldToEmergency(vehicle)) {
                slowForEmergency(vehicle);
                vehicle.nudgeAside(rightSideYieldOffset(vehicle));
                keepVehicleOnRoad(vehicle);
                soundManager.playTurnSignal();
            }

            if (vehicle.getState() == VehicleState.WAITING) {
                vehicle.setState(VehicleState.STOPPED);
            }

            soundManager.playVehicleSound(vehicle);
        }
    }

    private boolean mustStopAtRedLight(Vehicle vehicle) {
        TrafficLight light = getTrafficLightForDirection(vehicle.getDirection());
        if (light == null || !light.isRed()) {
            return false;
        }

        switch (vehicle.getDirection()) {
            case EAST:
                return vehicle.getPosition().getX() < CENTER_X - STOP_LINE_OFFSET
                        && vehicle.getPosition().getX() > CENTER_X - STOP_LINE_OFFSET - 45;
            case WEST:
                return vehicle.getPosition().getX() > CENTER_X + STOP_LINE_OFFSET
                        && vehicle.getPosition().getX() < CENTER_X + STOP_LINE_OFFSET + 45;
            case NORTH:
                return vehicle.getPosition().getY() > CENTER_Y + STOP_LINE_OFFSET
                        && vehicle.getPosition().getY() < CENTER_Y + STOP_LINE_OFFSET + 45;
            case SOUTH:
                return vehicle.getPosition().getY() < CENTER_Y - STOP_LINE_OFFSET
                        && vehicle.getPosition().getY() > CENTER_Y - STOP_LINE_OFFSET - 45;
            default:
                return false;
        }
    }

    private TrafficLight getTrafficLightForDirection(Direction direction) {
        if (trafficLights.isEmpty()) {
            return null;
        }

        int index;
        switch (direction) {
            case EAST:
                index = 0;
                break;
            case SOUTH:
                index = 1;
                break;
            case WEST:
                index = 2;
                break;
            case NORTH:
                index = 3;
                break;
            default:
                index = 0;
                break;
        }
        return trafficLights.get(index % trafficLights.size());
    }

    private Vehicle findNearestFrontVehicle(Vehicle vehicle) {
        return vehicles.stream()
                .filter(other -> other != vehicle)
                .filter(other -> other.getDirection() == vehicle.getDirection())
                .filter(other -> lateralDistanceBetween(vehicle, other) <= LANE_KEEP_MARGIN * 2)
                .filter(other -> distanceInFront(vehicle, other) >= 0)
                .min(Comparator.comparingDouble(other -> distanceInFront(vehicle, other)))
                .orElse(null);
    }

    private void handleTooCloseFrontVehicle(Vehicle vehicle, Vehicle frontVehicle) {
        if (!isEmergency(frontVehicle)
                && isFrontVehicleTooSlow(vehicle, frontVehicle)
                && canOvertakeSafely(vehicle)) {
            soundManager.playHorn();
            soundManager.playTurnSignal();
            vehicle.nudgeAside(overtakeOffset(vehicle));
            keepVehicleOnRoad(vehicle);
            return;
        }

        vehicle.brake();
        blockVehicle(vehicle);
    }

    private boolean isFrontVehicleTooSlow(Vehicle vehicle, Vehicle frontVehicle) {
        return frontVehicle.getSpeed() + 0.5 < vehicle.getSpeed();
    }

    private void blockVehicle(Vehicle vehicle) {
        vehicle.setState(VehicleState.WAITING);
        if (!blockedVehicles.contains(vehicle)) {
            blockedVehicles.add(vehicle);
        }
    }

    private boolean canOvertakeSafely(Vehicle vehicle) {
        double offset = overtakeOffset(vehicle);
        double projectedX = vehicle.getPosition().getX();
        double projectedY = vehicle.getPosition().getY();

        switch (vehicle.getDirection()) {
            case NORTH:
            case SOUTH:
                projectedX += offset;
                break;
            case EAST:
            case WEST:
                projectedY += offset;
                break;
            default:
                break;
        }

        for (Vehicle other : vehicles) {
            if (other == vehicle) {
                continue;
            }
            double dx = projectedX - other.getPosition().getX();
            double dy = projectedY - other.getPosition().getY();
            if (Math.sqrt(dx * dx + dy * dy) < SAFE_DISTANCE * 0.85) {
                return false;
            }
        }
        return true;
    }

    private double overtakeOffset(Vehicle vehicle) {
        return vehicle.getId().hashCode() % 2 == 0 ? 3.0 : -3.0;
    }

    private double rightSideYieldOffset(Vehicle vehicle) {
        switch (vehicle.getDirection()) {
            case EAST:
            case NORTH:
                return 2.5;
            case WEST:
            case SOUTH:
                return -2.5;
            default:
                return 0;
        }
    }

    private void slowForEmergency(Vehicle vehicle) {
        if (vehicle.getSpeed() > 1.0) {
            vehicle.brake();
        }
    }

    private boolean shouldYieldToEmergency(Vehicle vehicle) {
        return vehicles.stream()
                .filter(this::isEmergency)
                .anyMatch(emergency -> emergency != vehicle && isEmergencyApproachingSameLane(vehicle, emergency));
    }

    private boolean isEmergencyApproachingSameLane(Vehicle vehicle, Vehicle emergency) {
        if (vehicle.getDirection() != emergency.getDirection()) {
            return false;
        }

        double lateralDistance = lateralDistanceBetween(vehicle, emergency);
        double frontDistanceFromEmergency = distanceInFront(emergency, vehicle);
        return lateralDistance <= 35
                && frontDistanceFromEmergency >= 0
                && frontDistanceFromEmergency < EMERGENCY_YIELD_DISTANCE;
    }

    private double lateralDistanceBetween(Vehicle first, Vehicle second) {
        switch (first.getDirection()) {
            case EAST:
            case WEST:
                return Math.abs(first.getPosition().getY() - second.getPosition().getY());
            case NORTH:
            case SOUTH:
                return Math.abs(first.getPosition().getX() - second.getPosition().getX());
            default:
                return Double.MAX_VALUE;
        }
    }

    private boolean isEmergency(Vehicle vehicle) {
        return vehicle instanceof Ambulance || vehicle instanceof FireTruck;
    }

    private double distanceInFront(Vehicle currentVehicle, Vehicle candidate) {
        switch (currentVehicle.getDirection()) {
            case NORTH:
                return currentVehicle.getPosition().getY() - candidate.getPosition().getY();
            case SOUTH:
                return candidate.getPosition().getY() - currentVehicle.getPosition().getY();
            case EAST:
                return candidate.getPosition().getX() - currentVehicle.getPosition().getX();
            case WEST:
                return currentVehicle.getPosition().getX() - candidate.getPosition().getX();
            default:
                return -1;
        }
    }

    private double distanceBetween(Vehicle first, Vehicle second) {
        double dx = first.getPosition().getX() - second.getPosition().getX();
        double dy = first.getPosition().getY() - second.getPosition().getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    private void applyTurnIfNeeded(Vehicle vehicle) {
        if (vehiclesThatTurned.contains(vehicle) || !isAtIntersectionCenter(vehicle)) {
            return;
        }

        TurnChoice turnChoice = getTurnChoice(vehicle);
        if (turnChoice == TurnChoice.STRAIGHT) {
            vehiclesThatTurned.add(vehicle);
            return;
        }

        if (turnChoice == TurnChoice.LEFT && !isEmergency(vehicle) && hasOpposingTrafficNearIntersection(vehicle)) {
            vehicle.brake();
            blockVehicle(vehicle);
            return;
        }

        Direction newDirection = directionAfterTurn(vehicle.getDirection(), turnChoice);
        double targetX = targetLaneX(vehicle, newDirection);
        double targetY = targetLaneY(vehicle, newDirection);
        if (!canTurnSafely(vehicle, targetX, targetY)) {
            vehicle.brake();
            blockVehicle(vehicle);
            return;
        }

        turningVehicles.put(vehicle, new PendingTurn(newDirection, targetX, targetY));
        soundManager.playTurnSignal();
    }

    private void slowWhenApproachingIntersection(Vehicle vehicle) {
        if (distanceToIntersection(vehicle) < 150 && vehicle.getSpeed() > 2.0) {
            vehicle.brake();
        }
    }

    private double distanceToIntersection(Vehicle vehicle) {
        switch (vehicle.getDirection()) {
            case EAST:
                return Math.max(0, CENTER_X - vehicle.getPosition().getX());
            case WEST:
                return Math.max(0, vehicle.getPosition().getX() - CENTER_X);
            case NORTH:
                return Math.max(0, vehicle.getPosition().getY() - CENTER_Y);
            case SOUTH:
                return Math.max(0, CENTER_Y - vehicle.getPosition().getY());
            default:
                return Double.MAX_VALUE;
        }
    }

    private boolean hasOpposingTrafficNearIntersection(Vehicle vehicle) {
        Direction opposingDirection = oppositeDirection(vehicle.getDirection());
        return vehicles.stream()
                .filter(other -> other != vehicle)
                .filter(other -> other.getDirection() == opposingDirection)
                .anyMatch(other -> distanceToIntersection(other) < 170);
    }

    private Direction oppositeDirection(Direction direction) {
        switch (direction) {
            case EAST:
                return Direction.WEST;
            case WEST:
                return Direction.EAST;
            case NORTH:
                return Direction.SOUTH;
            case SOUTH:
                return Direction.NORTH;
            default:
                return direction;
        }
    }

    private boolean isAtIntersectionCenter(Vehicle vehicle) {
        switch (vehicle.getDirection()) {
            case EAST:
                return vehicle.getPosition().getX() >= CENTER_X - RIGHT_LANE_OFFSET;
            case WEST:
                return vehicle.getPosition().getX() <= CENTER_X + RIGHT_LANE_OFFSET;
            case NORTH:
                return vehicle.getPosition().getY() <= CENTER_Y + RIGHT_LANE_OFFSET;
            case SOUTH:
                return vehicle.getPosition().getY() >= CENTER_Y - RIGHT_LANE_OFFSET;
            default:
                return false;
        }
    }

    private TurnChoice getTurnChoice(Vehicle vehicle) {
        TurnChoice existingChoice = turnChoices.get(vehicle);
        if (existingChoice != null) {
            return existingChoice;
        }

        int routeIndex = random.nextInt(TurnChoice.values().length);
        TurnChoice choice = TurnChoice.values()[routeIndex];
        turnChoices.put(vehicle, choice);
        return choice;
    }

    private Direction directionAfterTurn(Direction direction, TurnChoice turnChoice) {
        switch (direction) {
            case EAST:
                return turnChoice == TurnChoice.RIGHT ? Direction.SOUTH : Direction.NORTH;
            case WEST:
                return turnChoice == TurnChoice.RIGHT ? Direction.NORTH : Direction.SOUTH;
            case NORTH:
                return turnChoice == TurnChoice.RIGHT ? Direction.EAST : Direction.WEST;
            case SOUTH:
                return turnChoice == TurnChoice.RIGHT ? Direction.WEST : Direction.EAST;
            default:
                return direction;
        }
    }

    private void advanceTurn(Vehicle vehicle) {
        PendingTurn pendingTurn = turningVehicles.get(vehicle);
        if (pendingTurn == null) {
            return;
        }

        double nextX = vehicle.getPosition().getX()
                + (pendingTurn.targetX - vehicle.getPosition().getX()) / pendingTurn.remainingSteps;
        double nextY = vehicle.getPosition().getY()
                + (pendingTurn.targetY - vehicle.getPosition().getY()) / pendingTurn.remainingSteps;
        vehicle.getPosition().setX(nextX);
        vehicle.getPosition().setY(nextY);
        pendingTurn.remainingSteps--;

        if (pendingTurn.remainingSteps <= 0) {
            vehicle.getPosition().setX(pendingTurn.targetX);
            vehicle.getPosition().setY(pendingTurn.targetY);
            vehicle.setDirection(pendingTurn.targetDirection);
            turningVehicles.remove(vehicle);
            vehiclesThatTurned.add(vehicle);
        }
    }

    private boolean canTurnSafely(Vehicle vehicle, double targetX, double targetY) {
        for (Vehicle other : vehicles) {
            if (other == vehicle) {
                continue;
            }
            double dx = targetX - other.getPosition().getX();
            double dy = targetY - other.getPosition().getY();
            if (Math.sqrt(dx * dx + dy * dy) < SAFE_DISTANCE * 0.75) {
                return false;
            }
        }
        return true;
    }

    private double targetLaneX(Vehicle vehicle, Direction direction) {
        switch (direction) {
            case EAST:
                return CENTER_X + RIGHT_LANE_OFFSET;
            case WEST:
                return CENTER_X - RIGHT_LANE_OFFSET;
            case NORTH:
            case SOUTH:
                return laneCenterFor(vehicle, direction);
            default:
                return CENTER_X;
        }
    }

    private double targetLaneY(Vehicle vehicle, Direction direction) {
        switch (direction) {
            case EAST:
            case WEST:
                return laneCenterFor(vehicle, direction);
            case NORTH:
                return CENTER_Y - RIGHT_LANE_OFFSET;
            case SOUTH:
                return CENTER_Y + RIGHT_LANE_OFFSET;
            default:
                return CENTER_Y;
        }
    }

    private void preventCollisions() {
        for (int i = 0; i < vehicles.size(); i++) {
            Vehicle vehicleA = vehicles.get(i);

            for (int j = i + 1; j < vehicles.size(); j++) {
                Vehicle vehicleB = vehicles.get(j);

                if (collisionDetector.isColliding(vehicleA, vehicleB)) {
                    vehicleA.brake();
                    vehicleB.brake();
                    separateVehicles(vehicleA, vehicleB);
                    soundManager.playHorn();
                }
            }
        }
    }

    private void separateVehicles(Vehicle vehicleA, Vehicle vehicleB) {
        vehicleA.nudgeAside(3.0);
        vehicleB.nudgeAside(-3.0);
        keepVehicleOnRoad(vehicleA);
        keepVehicleOnRoad(vehicleB);
    }

    private void keepVehicleOnRoad(Vehicle vehicle) {
        switch (vehicle.getDirection()) {
            case EAST:
            case WEST:
                vehicle.getPosition().setY(clamp(
                        vehicle.getPosition().getY(),
                        rightLaneCenter(vehicle) - LANE_KEEP_MARGIN,
                        rightLaneCenter(vehicle) + LANE_KEEP_MARGIN));
                break;
            case NORTH:
            case SOUTH:
                vehicle.getPosition().setX(clamp(
                        vehicle.getPosition().getX(),
                        rightLaneCenter(vehicle) - LANE_KEEP_MARGIN,
                        rightLaneCenter(vehicle) + LANE_KEEP_MARGIN));
                break;
            default:
                break;
        }
    }

    private double rightLaneCenter(Vehicle vehicle) {
        return laneCenterFor(vehicle, vehicle.getDirection());
    }

    private double laneCenterFor(Vehicle vehicle, Direction direction) {
        double offset = laneOffsetFor(vehicle);
        switch (direction) {
            case EAST:
                return CENTER_Y + offset;
            case WEST:
                return CENTER_Y - offset;
            case NORTH:
                return CENTER_X + offset;
            case SOUTH:
                return CENTER_X - offset;
            default:
                return 0;
        }
    }

    private double laneOffsetFor(Vehicle vehicle) {
        if (vehicle instanceof Bicycle) {
            return INNER_LANE_OFFSET;
        }
        if (vehicle instanceof Motorbike) {
            return MIDDLE_LANE_OFFSET;
        }
        return OUTER_LANE_OFFSET;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void removeVehiclesOutsideMap() {

        Iterator<Vehicle> iterator = vehicles.iterator();

        while (iterator.hasNext()) {
            Vehicle vehicle = iterator.next();
            double x = vehicle.getPosition().getX();
            double y = vehicle.getPosition().getY();

 
            if (x < -140 || x > 1040 || y < -140 || y > 840) {
                iterator.remove();
                turnChoices.remove(vehicle);
                turningVehicles.remove(vehicle);
                vehiclesThatTurned.remove(vehicle);
            }
        }
    }
}