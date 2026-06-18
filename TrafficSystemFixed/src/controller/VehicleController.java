package controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.HashSet;
import java.util.Set;

import model.trafficlight.TrafficLight;
import model.vehicle.Ambulance;
import model.vehicle.Bicycle;
import model.vehicle.Bus;
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
        private final Direction sourceDirection;
        private final Direction targetDirection;
        private final TurnChoice turnChoice;
        private final double startX;
        private final double startY;
        private final double control1X;
        private final double control1Y;
        private final double control2X;
        private final double control2Y;
        private final double targetX;
        private final double targetY;
        private final int totalSteps;
        private int currentStep;
        private int slowTick;

        private PendingTurn(Direction sourceDirection,
                            Direction targetDirection,
                            TurnChoice turnChoice,
                            double startX,
                            double startY,
                            double control1X,
                            double control1Y,
                            double control2X,
                            double control2Y,
                            double targetX,
                            double targetY,
                            int totalSteps) {
            this.sourceDirection = sourceDirection;
            this.targetDirection = targetDirection;
            this.turnChoice = turnChoice;
            this.startX = startX;
            this.startY = startY;
            this.control1X = control1X;
            this.control1Y = control1Y;
            this.control2X = control2X;
            this.control2Y = control2Y;
            this.targetX = targetX;
            this.targetY = targetY;
            this.totalSteps = totalSteps;
            this.currentStep = 0;
            this.slowTick = 0;
        }
    }

    private static class PendingLaneChange {
        private final Direction direction;
        private double startLateral;
        private double targetLateral;
        private final double sourceOffset;
        private double targetOffset;
        private double longitudinalLength;
        private double travelled;
        private int blockedTicks;
        private boolean returningToSource;

        private PendingLaneChange(Direction direction,
                                  double startLateral,
                                  double targetLateral,
                                  double sourceOffset,
                                  double targetOffset,
                                  double longitudinalLength) {
            this.direction = direction;
            this.startLateral = startLateral;
            this.targetLateral = targetLateral;
            this.sourceOffset = sourceOffset;
            this.targetOffset = targetOffset;
            this.longitudinalLength = longitudinalLength;
            this.travelled = 0;
            this.blockedTicks = 0;
            this.returningToSource = false;
        }
    }

    private static final double SAFE_DISTANCE = 54;
    private static final double PROXIMITY_SLOW_DISTANCE = 96;
    private static final double PROXIMITY_STOP_DISTANCE = 38;
    private static final double JUNCTION_SLOW_DISTANCE = 82;
    private static final double EMERGENCY_YIELD_DISTANCE = 130;
    private static final double CENTER_X = 450;
    private static final double CENTER_Y = 350;
    private static final double STOP_LINE_OFFSET = 216;
    private static final double RIGHT_LANE_OFFSET = 45;
    private static final double TURN_EXIT_OFFSET = 152;
    private static final double LANE_KEEP_MARGIN = 8;
    private static final double LEFT_TURN_LANE_OFFSET = 18;
    private static final double OUTER_LANE_OFFSET = 26;
    private static final double MIDDLE_LANE_OFFSET = 47;
    private static final double INNER_LANE_OFFSET = 68;
    private static final double RIGHT_TURN_LANE_OFFSET = 72;
    private static final double BUS_LANE_OFFSET = 82;
    private static final double TURN_POCKET_DISTANCE = 260;
    private static final double LEFT_TURN_START_DISTANCE = 28;
    private static final double RIGHT_TURN_START_DISTANCE = 46;
    private static final double CONFLICT_RESERVATION_DISTANCE = 178;
    private static final double LANE_CHANGE_LENGTH = 105;
    private static final double LANE_CHANGE_FRONT_GAP = 72;
    private static final double LANE_CHANGE_REAR_GAP = 58;
    private static final double TURN_COMPLETE_TOLERANCE = 7;
    private static final double INTERSECTION_RESERVATION_DISTANCE = 168;
    private static final double INTERSECTION_RELEASE_DISTANCE = 176;
    private static final int LANE_CHANGE_BLOCKED_RECOVERY_TICKS = 28;

    private final List<Vehicle> vehicles;
    private final CollisionDetector collisionDetector;
    private final SoundManager soundManager;
    private final List<Vehicle> blockedVehicles;
    private final Map<Vehicle, TurnChoice> turnChoices;
    private final Map<Vehicle, PendingTurn> turningVehicles;
    private final Map<Vehicle, PendingLaneChange> laneChangingVehicles;
    private final Map<Vehicle, Double> assignedLaneOffsets;
    private final List<Vehicle> vehiclesThatTurned;
    private final Set<Vehicle> vehiclesPastStopLine;
    private final Map<Vehicle, Integer> stalledTicks;
    private Vehicle intersectionOwner;
    private int intersectionOwnerIdleTicks;
    private final Random random;
    private List<TrafficLight> trafficLights;

    public VehicleController() {
        vehicles = new ArrayList<>();
        collisionDetector = new CollisionDetector();
        soundManager = new SoundManager();
        blockedVehicles = new ArrayList<>();
        turnChoices = new HashMap<>();
        turningVehicles = new HashMap<>();
        laneChangingVehicles = new HashMap<>();
        assignedLaneOffsets = new HashMap<>();
        vehiclesThatTurned = new ArrayList<>();
        vehiclesPastStopLine = new HashSet<>();
        stalledTicks = new HashMap<>();
        intersectionOwner = null;
        intersectionOwnerIdleTicks = 0;
        random = new Random();
        trafficLights = Collections.emptyList();
    }

    public void addVehicle(Vehicle vehicle) {


        if (vehicle != null && !vehicles.contains(vehicle)) {
            vehicles.add(vehicle);
            assignedLaneOffsets.put(vehicle,
                    vehicle instanceof Bicycle ? INNER_LANE_OFFSET : nearestLaneOffsetForPosition(vehicle));
        }
    }

    public void removeVehicle(Vehicle vehicle) {

        vehicles.remove(vehicle);
        turnChoices.remove(vehicle);
        turningVehicles.remove(vehicle);
        laneChangingVehicles.remove(vehicle);
        assignedLaneOffsets.remove(vehicle);
        vehicle.clearIndicators();
        vehiclesThatTurned.remove(vehicle);
        vehiclesPastStopLine.remove(vehicle);
        stalledTicks.remove(vehicle);
        if (intersectionOwner == vehicle) {
            intersectionOwner = null;
            intersectionOwnerIdleTicks = 0;
        }
    }

    public List<Vehicle> getVehicles() {


        return Collections.unmodifiableList(vehicles);
    }

    public void setTrafficLights(List<TrafficLight> trafficLights) {
        this.trafficLights = trafficLights == null ? Collections.emptyList() : trafficLights;
    }

    public void updateVehicles() {
        blockedVehicles.clear();
        refreshIntersectionReservation();
        applyTrafficRules();

        for (Vehicle vehicle : new ArrayList<>(vehicles)) {
            if (!blockedVehicles.contains(vehicle)) {
                if (turningVehicles.containsKey(vehicle)) {
                    advanceTurn(vehicle);
                } else if (laneChangingVehicles.containsKey(vehicle)) {
                    advanceLaneChange(vehicle);
                } else if (isNextStraightMoveSafe(vehicle)) {
                    vehicle.update();
                    applyTurnIfNeeded(vehicle);
                    keepVehicleOnRoad(vehicle);
                } else {
                    vehicle.brake();
                    blockVehicle(vehicle);
                }
            }
        }

        // Không còn teleport xe lùi về sau khi va chạm.
        // Xe sẽ giảm tốc từ xa, còn nếu quá sát thì dừng mềm tại chỗ.
        preventCollisions();
        recoverStalledVehicles();
        refreshIntersectionReservation();
        removeVehiclesOutsideMap();
    }

    private void applyTrafficRules() {
        for (Vehicle vehicle : vehicles) {
            updateStopLineCommitment(vehicle);

            // A vehicle already following a turn arc must finish the arc. A vehicle that
            // is only changing lanes still obeys its signal and the intersection reservation,
            // otherwise it may enter the junction from a red approach and deadlock every road.
            if (turningVehicles.containsKey(vehicle)) {
                if (isEmergency(vehicle)) {
                    soundManager.playSiren();
                }
                continue;
            }
            if (laneChangingVehicles.containsKey(vehicle)) {
                if (isEmergency(vehicle)) {
                    soundManager.playSiren();
                }
                // Xe đã vượt vạch dừng của chính hướng mình phải tiếp tục đi qua
                // giao lộ, kể cả khi đang đổi làn. Không được dừng lại ở zebra đối diện.
                if (!hasCommittedToIntersection(vehicle)
                        && (mustStopAtRedLight(vehicle) || mustStopForIntersectionConflict(vehicle))) {
                    vehicle.brake();
                    blockVehicle(vehicle);
                }
                continue;
            }

            if (isEmergency(vehicle)) {
                soundManager.playSiren();
            }

            if (mustStopAtRedLight(vehicle)) {
                vehicle.stop();
                blockVehicle(vehicle);
                continue;
            }

            // Xe đã qua vạch dừng của chính hướng mình phải thoát khỏi giao lộ trước.
            // Đặt nhánh này trước kiểm tra quyền giao lộ để tránh xe bị dừng lại
            // giữa ngã tư hoặc trước zebra phía đối diện.
            if (hasCommittedToIntersection(vehicle)) {
                keepCommittedVehicleMoving(vehicle);
                soundManager.playVehicleSound(vehicle);
                continue;
            }

            if (mustStopForIntersectionConflict(vehicle)) {
                vehicle.brake();
                blockVehicle(vehicle);
                continue;
            }

            // If the light is green and the vehicle was previously stopped at red, wake it
            // immediately instead of waiting in STOPPED/WAITING forever.
            TrafficLight ownLight = getTrafficLightForDirection(vehicle.getDirection());
            if (ownLight != null && !ownLight.isRed()
                    && (vehicle.getState() == VehicleState.WAITING || vehicle.getSpeed() < 0.20)) {
                vehicle.setState(VehicleState.MOVING);
                vehicle.accelerate();
                vehicle.accelerate();
            }

            slowWhenApproachingIntersection(vehicle);

            // Xử lý xe cùng làn trước để cứu thương/cứu hỏa có cơ hội bật xi-nhan
            // chuyển làn, thay vì bị dừng cứng bởi kiểm tra khoảng cách chung.
            Vehicle frontVehicle = findNearestFrontVehicle(vehicle);
            if (frontVehicle != null && distanceInFront(vehicle, frontVehicle) < PROXIMITY_SLOW_DISTANCE) {
                handleTooCloseFrontVehicle(vehicle, frontVehicle);
                if (blockedVehicles.contains(vehicle) || laneChangingVehicles.containsKey(vehicle)) {
                    continue;
                }
            }

            Vehicle nearestRisk = findNearestRiskVehicle(vehicle);
            if (nearestRisk != null && nearestRisk != frontVehicle) {
                double riskDistance = distanceBetween(vehicle, nearestRisk);
                if (riskDistance < PROXIMITY_STOP_DISTANCE) {
                    vehicle.stop();
                    blockVehicle(vehicle);
                    continue;
                }
                if (riskDistance < PROXIMITY_SLOW_DISTANCE) {
                    limitSpeedNearVehicle(vehicle, nearestRisk, riskDistance);
                }
            }

            if (shouldYieldToEmergency(vehicle)) {
                if (!tryStartRightYieldLaneChange(vehicle)) {
                    slowForEmergency(vehicle);
                }
            }

            if (vehicle.getState() == VehicleState.WAITING) {
                vehicle.setState(VehicleState.STOPPED);
            }

            wakeVehicleWhenPathIsClear(vehicle);
            soundManager.playVehicleSound(vehicle);
        }
    }


    private boolean hasCommittedToIntersection(Vehicle vehicle) {
        return vehiclesPastStopLine.contains(vehicle)
                || vehiclesThatTurned.contains(vehicle)
                || turningVehicles.containsKey(vehicle)
                || isInsideConflictBox(vehicle);
    }

    private void keepCommittedVehicleMoving(Vehicle vehicle) {
        Vehicle frontVehicle = findNearestFrontVehicle(vehicle);
        if (frontVehicle != null) {
            double gap = distanceInFront(vehicle, frontVehicle);
            if (gap >= 0 && gap < PROXIMITY_SLOW_DISTANCE) {
                handleTooCloseFrontVehicle(vehicle, frontVehicle);
                if (blockedVehicles.contains(vehicle) || laneChangingVehicles.containsKey(vehicle)) {
                    return;
                }
            }
        }

        // A vehicle that already crossed its own stop line must clear the junction.
        // Wake it up after a previous red-light/conflict wait instead of letting it
        // remain STOPPED at the zebra crossing on the opposite side.
        if (vehicle.getState() == VehicleState.WAITING || vehicle.getSpeed() < 0.35) {
            vehicle.setState(VehicleState.MOVING);
            vehicle.accelerate();
            vehicle.accelerate();
        } else {
            vehicle.accelerate();
        }
    }

    private boolean mustStopForIntersectionConflict(Vehicle vehicle) {
        if (hasCommittedToIntersection(vehicle)) {
            if (intersectionOwner == null) {
                intersectionOwner = vehicle;
                intersectionOwnerIdleTicks = 0;
            }
            return intersectionOwner != vehicle;
        }

        double distance = distanceToIntersection(vehicle);
        if (distance > INTERSECTION_RESERVATION_DISTANCE) {
            return false;
        }

        if (intersectionOwner == null) {
            TrafficLight ownLight = getTrafficLightForDirection(vehicle.getDirection());
            if (ownLight == null || !ownLight.isRed()) {
                intersectionOwner = vehicle;
                intersectionOwnerIdleTicks = 0;
                return false;
            }
            return true;
        }

        return intersectionOwner != vehicle;
    }

    private void refreshIntersectionReservation() {
        if (intersectionOwner != null) {
            if (!vehicles.contains(intersectionOwner) || hasClearedIntersection(intersectionOwner)) {
                intersectionOwner = null;
                intersectionOwnerIdleTicks = 0;
            } else if (intersectionOwner.getSpeed() < 0.05) {
                intersectionOwnerIdleTicks++;
                // A stale owner must not freeze every approach forever.
                if (intersectionOwnerIdleTicks > 45
                        && !isInsideConflictBox(intersectionOwner)
                        && !turningVehicles.containsKey(intersectionOwner)) {
                    intersectionOwner = null;
                    intersectionOwnerIdleTicks = 0;
                }
            } else {
                intersectionOwnerIdleTicks = 0;
            }
        }

        if (intersectionOwner == null) {
            intersectionOwner = vehicles.stream()
                    .filter(v -> turningVehicles.containsKey(v)
                            || isInsideConflictBox(v)
                            || vehiclesPastStopLine.contains(v))
                    .min(Comparator
                            .comparing((Vehicle v) -> !isEmergency(v))
                            .thenComparingDouble(this::distanceToIntersection))
                    .orElse(null);
        }
    }

    private boolean hasClearedIntersection(Vehicle vehicle) {
        if (turningVehicles.containsKey(vehicle) || isInsideConflictBox(vehicle)) {
            return false;
        }
        double dx = Math.abs(vehicle.getPosition().getX() - CENTER_X);
        double dy = Math.abs(vehicle.getPosition().getY() - CENTER_Y);
        return Math.max(dx, dy) > INTERSECTION_RELEASE_DISTANCE
                && vehiclesPastStopLine.contains(vehicle);
    }

    private boolean isInsideConflictBox(Vehicle vehicle) {
        double x = vehicle.getPosition().getX();
        double y = vehicle.getPosition().getY();
        return x > CENTER_X - 132 && x < CENTER_X + 132
                && y > CENTER_Y - 132 && y < CENTER_Y + 132;
    }

    private boolean pathsMayConflict(Vehicle vehicle, Vehicle other) {
        TurnChoice a = getTurnChoice(vehicle);
        TurnChoice b = getTurnChoice(other);
        Direction aExit = a == TurnChoice.STRAIGHT ? vehicle.getDirection() : directionAfterTurn(vehicle.getDirection(), a);
        Direction bExit = b == TurnChoice.STRAIGHT ? other.getDirection() : directionAfterTurn(other.getDirection(), b);

        if (vehicle.getDirection() == other.getDirection() && aExit == bExit) {
            return lateralDistanceBetween(vehicle, other) < SAFE_DISTANCE;
        }
        if (a == TurnChoice.RIGHT && b == TurnChoice.RIGHT && aExit != bExit) {
            return false;
        }
        return vehicle.getDirection() != other.getDirection() || aExit == bExit;
    }

    private boolean shouldYield(Vehicle vehicle, Vehicle other) {
        if (isEmergency(vehicle)) return false;
        if (isEmergency(other)) return true;
        if (vehicle instanceof Bus && !(other instanceof Bus)) return false;
        if (other instanceof Bus && !(vehicle instanceof Bus)) return true;
        double myDistance = distanceToIntersection(vehicle);
        double otherDistance = distanceToIntersection(other);
        if (turningVehicles.containsKey(other) || isInsideConflictBox(other)) return true;
        return myDistance > otherDistance;
    }

    private boolean mustStopAtRedLight(Vehicle vehicle) {
        // Once the front of the vehicle has crossed its own stop line, it is committed.
        // It must clear the junction and must never stop at the opposite zebra crossing.
        if (hasCommittedToIntersection(vehicle)) {
            return false;
        }

        TrafficLight light = getTrafficLightForDirection(vehicle.getDirection());
        if (light == null || !light.isRed()) {
            return false;
        }

        if (hasCrossedOwnStopLine(vehicle)) {
            vehiclesPastStopLine.add(vehicle);
            return false;
        }

        double distanceToStopCenter = distanceToOwnStopCenter(vehicle);

        // Never teleport a vehicle backwards after it has already overshot the correct
        // stopping centre. Treat it as committed and let it clear the junction. The larger
        // dynamic trigger below prevents this overshoot on subsequent approaches.
        if (distanceToStopCenter < -0.5) {
            vehiclesPastStopLine.add(vehicle);
            return false;
        }

        // Braking distance grows with speed. Stop early enough that faster vehicles do not
        // jump across the stop line in a single frame.
        double triggerDistance = Math.max(14.0, vehicle.getSpeed() * 2.4 + 8.0);
        boolean shouldStop = distanceToStopCenter <= triggerDistance;
        if (shouldStop) {
            clampVehicleBehindOwnStopLine(vehicle);
        }
        return shouldStop;
    }

    private void updateStopLineCommitment(Vehicle vehicle) {
        if (vehiclesPastStopLine.contains(vehicle)) {
            return;
        }
        if (turningVehicles.containsKey(vehicle)
                || isInsideConflictBox(vehicle)
                || hasCrossedOwnStopLine(vehicle)) {
            vehiclesPastStopLine.add(vehicle);
        }
    }

    private boolean hasCrossedOwnStopLine(Vehicle vehicle) {
        double halfLength = vehicleHalfLength(vehicle);
        switch (vehicle.getDirection()) {
            case EAST: {
                double line = CENTER_X - STOP_LINE_OFFSET;
                return vehicle.getPosition().getX() + halfLength >= line - 0.5;
            }
            case WEST: {
                double line = CENTER_X + STOP_LINE_OFFSET;
                return vehicle.getPosition().getX() - halfLength <= line + 0.5;
            }
            case NORTH: {
                double line = CENTER_Y + STOP_LINE_OFFSET;
                return vehicle.getPosition().getY() - halfLength <= line + 0.5;
            }
            case SOUTH: {
                double line = CENTER_Y - STOP_LINE_OFFSET;
                return vehicle.getPosition().getY() + halfLength >= line - 0.5;
            }
            default:
                return false;
        }
    }

    private double distanceToOwnStopCenter(Vehicle vehicle) {
        double halfLength = vehicleHalfLength(vehicle);
        double margin = 4.0;
        switch (vehicle.getDirection()) {
            case EAST: {
                double targetX = CENTER_X - STOP_LINE_OFFSET - halfLength - margin;
                return targetX - vehicle.getPosition().getX();
            }
            case WEST: {
                double targetX = CENTER_X + STOP_LINE_OFFSET + halfLength + margin;
                return vehicle.getPosition().getX() - targetX;
            }
            case NORTH: {
                double targetY = CENTER_Y + STOP_LINE_OFFSET + halfLength + margin;
                return vehicle.getPosition().getY() - targetY;
            }
            case SOUTH: {
                double targetY = CENTER_Y - STOP_LINE_OFFSET - halfLength - margin;
                return targetY - vehicle.getPosition().getY();
            }
            default:
                return Double.MAX_VALUE;
        }
    }

    private void clampVehicleBehindOwnStopLine(Vehicle vehicle) {
        double halfLength = vehicleHalfLength(vehicle);
        double margin = 4.0;
        switch (vehicle.getDirection()) {
            case EAST: {
                double targetX = CENTER_X - STOP_LINE_OFFSET - halfLength - margin;
                if (vehicle.getPosition().getX() > targetX) {
                    vehicle.getPosition().setX(targetX);
                }
                break;
            }
            case WEST: {
                double targetX = CENTER_X + STOP_LINE_OFFSET + halfLength + margin;
                if (vehicle.getPosition().getX() < targetX) {
                    vehicle.getPosition().setX(targetX);
                }
                break;
            }
            case NORTH: {
                double targetY = CENTER_Y + STOP_LINE_OFFSET + halfLength + margin;
                if (vehicle.getPosition().getY() < targetY) {
                    vehicle.getPosition().setY(targetY);
                }
                break;
            }
            case SOUTH: {
                double targetY = CENTER_Y - STOP_LINE_OFFSET - halfLength - margin;
                if (vehicle.getPosition().getY() > targetY) {
                    vehicle.getPosition().setY(targetY);
                }
                break;
            }
            default:
                break;
        }
    }

    private double vehicleHalfLength(Vehicle vehicle) {
        if (vehicle instanceof Bus || vehicle instanceof FireTruck) {
            return 31.0;
        }
        if (vehicle instanceof Ambulance) {
            return 25.0;
        }
        if (vehicle instanceof Motorbike) {
            return 18.0;
        }
        if (vehicle instanceof Bicycle) {
            return 16.0;
        }
        return 22.0;
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
        double gap = distanceInFront(vehicle, frontVehicle);
        if (gap < 0) {
            return;
        }

        // Nếu xe phía trước đang dạt khỏi làn để nhường, xe sau không được đổi
        // vào chính làn đích đó. Chờ nó rời hành lang hiện tại rồi đi tiếp.
        PendingLaneChange frontChange = laneChangingVehicles.get(frontVehicle);
        if (frontChange != null) {
            limitSpeedNearVehicle(vehicle, frontVehicle, gap);
            return;
        }

        // Mọi xe, đặc biệt cứu thương/cứu hỏa, phải chuyển làn thật sự thay vì
        // dịch ngang tức thời. Chỉ chuyển khi làn bên cạnh có khoảng trống an toàn.
        boolean shouldChangeLane = gap < PROXIMITY_SLOW_DISTANCE
                && (isEmergency(vehicle) || isFrontVehicleTooSlow(vehicle, frontVehicle));
        if (shouldChangeLane && tryStartAdjacentLaneChange(vehicle, frontVehicle)) {
            soundManager.playHorn();
            return;
        }

        if (gap < PROXIMITY_STOP_DISTANCE) {
            vehicle.stop();
            blockVehicle(vehicle);
        } else {
            limitSpeedNearVehicle(vehicle, frontVehicle, gap);
        }
    }

    private boolean tryStartAdjacentLaneChange(Vehicle vehicle, Vehicle frontVehicle) {
        if (vehicle instanceof Bicycle
                || turningVehicles.containsKey(vehicle)
                || laneChangingVehicles.containsKey(vehicle)
                || isInsideConflictBox(vehicle)
                || distanceToIntersection(vehicle) < 78) {
            return false;
        }

        double currentOffset = assignedLaneOffsets.getOrDefault(vehicle, nearestLaneOffsetForPosition(vehicle));
        double[] candidates;
        if (currentOffset <= (OUTER_LANE_OFFSET + MIDDLE_LANE_OFFSET) / 2.0) {
            candidates = new double[] {MIDDLE_LANE_OFFSET, INNER_LANE_OFFSET};
        } else if (currentOffset <= (MIDDLE_LANE_OFFSET + INNER_LANE_OFFSET) / 2.0) {
            candidates = new double[] {INNER_LANE_OFFSET, OUTER_LANE_OFFSET};
        } else {
            candidates = new double[] {MIDDLE_LANE_OFFSET, OUTER_LANE_OFFSET};
        }

        for (double targetOffset : candidates) {
            if (Math.abs(targetOffset - currentOffset) > 24.0) {
                continue; // chỉ đổi từng làn một
            }
            double targetLateral = laneCenterForOffset(targetOffset, vehicle.getDirection());
            if (!isLaneChangeGapSafe(vehicle, targetLateral)) {
                continue;
            }

            double currentLateral = lateralCoordinate(vehicle);
            double length = LANE_CHANGE_LENGTH + Math.abs(targetLateral - currentLateral) * 1.10;
            if (vehicle instanceof Bus || vehicle instanceof FireTruck) {
                length += 28;
            } else if (vehicle instanceof Ambulance) {
                length += 12;
            }
            laneChangingVehicles.put(vehicle,
                    new PendingLaneChange(vehicle.getDirection(), currentLateral,
                            targetLateral, currentOffset, targetOffset, length));
            setLaneChangeIndicator(vehicle, currentOffset, targetOffset);
            soundManager.playTurnSignal();
            if (vehicle.getSpeed() < 1.0) {
                vehicle.setSpeed(1.0);
            }
            return true;
        }
        return false;
    }

    private boolean tryStartRightYieldLaneChange(Vehicle vehicle) {
        if (vehicle instanceof Bicycle
                || turningVehicles.containsKey(vehicle)
                || laneChangingVehicles.containsKey(vehicle)
                || isInsideConflictBox(vehicle)
                || distanceToIntersection(vehicle) < 78) {
            return false;
        }

        double currentOffset = assignedLaneOffsets.getOrDefault(vehicle, nearestLaneOffsetForPosition(vehicle));
        double targetOffset;
        if (currentOffset < (OUTER_LANE_OFFSET + MIDDLE_LANE_OFFSET) / 2.0) {
            targetOffset = MIDDLE_LANE_OFFSET;
        } else if (currentOffset < (MIDDLE_LANE_OFFSET + INNER_LANE_OFFSET) / 2.0) {
            targetOffset = INNER_LANE_OFFSET;
        } else {
            return false;
        }

        double targetLateral = laneCenterForOffset(targetOffset, vehicle.getDirection());
        if (!isLaneChangeGapSafe(vehicle, targetLateral)) {
            return false;
        }

        double currentLateral = lateralCoordinate(vehicle);
        laneChangingVehicles.put(vehicle,
                new PendingLaneChange(vehicle.getDirection(), currentLateral,
                        targetLateral, currentOffset, targetOffset,
                        LANE_CHANGE_LENGTH + Math.abs(targetLateral - currentLateral)));
        setLaneChangeIndicator(vehicle, currentOffset, targetOffset);
        soundManager.playTurnSignal();
        return true;
    }

    private void setLaneChangeIndicator(Vehicle vehicle, double currentOffset, double targetOffset) {
        vehicle.clearIndicators();
        if (targetOffset > currentOffset) {
            vehicle.setRightIndicatorOn(true);
        } else if (targetOffset < currentOffset) {
            vehicle.setLeftIndicatorOn(true);
        }
    }

    private boolean isLaneChangeGapSafe(Vehicle vehicle, double targetLateral) {
        for (Map.Entry<Vehicle, PendingLaneChange> entry : laneChangingVehicles.entrySet()) {
            Vehicle otherVehicle = entry.getKey();
            PendingLaneChange otherChange = entry.getValue();
            if (otherVehicle == vehicle || otherVehicle.getDirection() != vehicle.getDirection()) {
                continue;
            }
            if (Math.abs(otherChange.targetLateral - targetLateral) < 10.0
                    && Math.abs(distanceInFront(vehicle, otherVehicle)) < 115.0) {
                return false;
            }
        }

        for (Vehicle other : vehicles) {
            if (other == vehicle || other.getDirection() != vehicle.getDirection()) {
                continue;
            }
            double allowedLateral = (vehicleWidth(vehicle) + vehicleWidth(other)) / 2.0 + 3.0;
            if (Math.abs(lateralCoordinate(other) - targetLateral) > allowedLateral) {
                continue;
            }

            double frontGap = distanceInFront(vehicle, other);
            if (frontGap >= 0 && frontGap < LANE_CHANGE_FRONT_GAP + vehicleLength(other) * 0.35) {
                return false;
            }
            double rearGap = distanceInFront(other, vehicle);
            if (rearGap >= 0 && rearGap < LANE_CHANGE_REAR_GAP + vehicleLength(vehicle) * 0.25) {
                return false;
            }
        }
        return true;
    }

    private void advanceLaneChange(Vehicle vehicle) {
        PendingLaneChange change = laneChangingVehicles.get(vehicle);
        if (change == null) {
            return;
        }

        if (!isLaneChangeGapSafe(vehicle, change.targetLateral)) {
            change.blockedTicks++;

            // Do not freeze the whole direction while waiting for the adjacent lane.
            // Continue slowly in the current corridor when the forward step is safe.
            if (distanceToIntersection(vehicle) > 92 && isNextStraightMoveSafe(vehicle)) {
                if (vehicle.getSpeed() < 0.75) {
                    vehicle.setSpeed(0.75);
                }
                vehicle.update();
                vehicle.setVisualHeadingDegrees(headingForDirection(change.direction));
            } else {
                vehicle.brake();
            }

            // Close to the junction, return smoothly to the original lane instead of
            // remaining forever on a lane divider.
            if (change.blockedTicks >= LANE_CHANGE_BLOCKED_RECOVERY_TICKS
                    && distanceToIntersection(vehicle) <= 112
                    && !change.returningToSource) {
                change.startLateral = lateralCoordinate(vehicle);
                change.targetLateral = laneCenterForOffset(change.sourceOffset, change.direction);
                change.targetOffset = change.sourceOffset;
                change.longitudinalLength = 72 + Math.abs(change.targetLateral - change.startLateral) * 1.15;
                change.travelled = 0;
                change.blockedTicks = 0;
                change.returningToSource = true;
                setLaneChangeIndicator(vehicle,
                        nearestLaneOffsetForPosition(vehicle), change.sourceOffset);
            }
            return;
        }

        change.blockedTicks = 0;
        double oldX = vehicle.getPosition().getX();
        double oldY = vehicle.getPosition().getY();
        vehicle.update();

        double nextTravelled = change.travelled + Math.max(0.65, vehicle.getSpeed());
        double t = Math.min(1.0, nextTravelled / change.longitudinalLength);

        // Boomerang/S-curve: starts parallel to the old lane, bends toward the
        // adjacent lane, then becomes parallel again without a lateral teleport.
        double curve = 0.5 - 0.5 * Math.cos(Math.PI * t);
        double lateral = change.startLateral
                + (change.targetLateral - change.startLateral) * curve;
        setLateralCoordinate(vehicle, lateral);

        if (!isPositionSafeForVehicleAt(vehicle,
                vehicle.getPosition().getX(), vehicle.getPosition().getY())) {
            vehicle.getPosition().setX(oldX);
            vehicle.getPosition().setY(oldY);
            vehicle.brake();
            change.blockedTicks++;
            return;
        }

        change.travelled = nextTravelled;
        double dx = vehicle.getPosition().getX() - oldX;
        double dy = vehicle.getPosition().getY() - oldY;
        if (Math.hypot(dx, dy) > 0.001) {
            vehicle.setVisualHeadingDegrees(Math.toDegrees(Math.atan2(dy, dx)));
        }

        if (t >= 1.0) {
            setLateralCoordinate(vehicle, change.targetLateral);
            assignedLaneOffsets.put(vehicle, change.targetOffset);
            laneChangingVehicles.remove(vehicle);
            vehicle.clearIndicators();
            vehicle.setVisualHeadingDegrees(headingForDirection(change.direction));
            vehicle.setState(VehicleState.MOVING);
            if (vehicle.getSpeed() < 0.9) {
                vehicle.setSpeed(0.9);
            }
            vehicle.accelerate();
        }
    }

    private boolean isNextStraightMoveSafe(Vehicle vehicle) {
        double travel = Math.max(0.75,
                Math.min(vehicle.getMaxSpeed(), vehicle.getSpeed() + (isEmergency(vehicle) ? 2.0 : 1.0)));
        double x = vehicle.getPosition().getX() + dirX(vehicle.getDirection()) * travel;
        double y = vehicle.getPosition().getY() + dirY(vehicle.getDirection()) * travel;
        return isPositionSafeForVehicleAt(vehicle, x, y);
    }

    private boolean isPositionSafeForVehicleAt(Vehicle vehicle, double x, double y) {
        for (Vehicle other : vehicles) {
            if (other == vehicle) {
                continue;
            }

            if (other.getDirection() == vehicle.getDirection()) {
                double lateralGap;
                double longitudinalGap;
                if (vehicle.getDirection() == Direction.EAST || vehicle.getDirection() == Direction.WEST) {
                    lateralGap = Math.abs(y - other.getPosition().getY());
                    longitudinalGap = Math.abs(x - other.getPosition().getX());
                } else {
                    lateralGap = Math.abs(x - other.getPosition().getX());
                    longitudinalGap = Math.abs(y - other.getPosition().getY());
                }
                double minLateral = (vehicleWidth(vehicle) + vehicleWidth(other)) / 2.0 + 2.0;
                double minLongitudinal = (vehicleLength(vehicle) + vehicleLength(other)) / 2.0 + 7.0;
                double candidateFrontGap = distanceInFrontFromPosition(vehicle, other, x, y);
                // A rear vehicle must not freeze the vehicle in front. Moving forward increases
                // that gap. Only an object ahead (or exactly alongside) blocks the candidate step.
                if (candidateFrontGap >= -0.5
                        && lateralGap < minLateral
                        && longitudinalGap < minLongitudinal) {
                    return false;
                }
                continue;
            }

            boolean junctionArea = isInsideConflictBox(vehicle)
                    || isInsideConflictBox(other)
                    || isPointInsideConflictBox(x, y)
                    || turningVehicles.containsKey(vehicle)
                    || turningVehicles.containsKey(other);
            if (!junctionArea) {
                continue;
            }

            double dx = x - other.getPosition().getX();
            double dy = y - other.getPosition().getY();
            double minDistance = Math.max(24.0,
                    (vehicleWidth(vehicle) + vehicleWidth(other)) * 0.85);
            if (Math.hypot(dx, dy) < minDistance) {
                return false;
            }
        }
        return true;
    }

    private boolean isPointInsideConflictBox(double x, double y) {
        return x > CENTER_X - 132 && x < CENTER_X + 132
                && y > CENTER_Y - 132 && y < CENTER_Y + 132;
    }

    private double vehicleLength(Vehicle vehicle) {
        if (vehicle instanceof Bus || vehicle instanceof FireTruck) return 48.0;
        if (vehicle instanceof Ambulance) return 40.0;
        if (vehicle instanceof Motorbike) return 25.0;
        if (vehicle instanceof Bicycle) return 21.0;
        return 31.0;
    }

    private double vehicleWidth(Vehicle vehicle) {
        if (vehicle instanceof Bus || vehicle instanceof FireTruck) return 17.0;
        if (vehicle instanceof Ambulance) return 16.0;
        if (vehicle instanceof Motorbike) return 9.0;
        if (vehicle instanceof Bicycle) return 7.0;
        return 15.0;
    }

    private double lateralCoordinate(Vehicle vehicle) {
        switch (vehicle.getDirection()) {
            case EAST:
            case WEST:
                return vehicle.getPosition().getY();
            case NORTH:
            case SOUTH:
                return vehicle.getPosition().getX();
            default:
                return 0;
        }
    }

    private void setLateralCoordinate(Vehicle vehicle, double value) {
        switch (vehicle.getDirection()) {
            case EAST:
            case WEST:
                vehicle.getPosition().setY(value);
                break;
            case NORTH:
            case SOUTH:
                vehicle.getPosition().setX(value);
                break;
            default:
                break;
        }
    }

    private double nearestLaneOffsetForPosition(Vehicle vehicle) {
        double raw;
        switch (vehicle.getDirection()) {
            case EAST:
                raw = vehicle.getPosition().getY() - CENTER_Y;
                break;
            case WEST:
                raw = CENTER_Y - vehicle.getPosition().getY();
                break;
            case NORTH:
                raw = vehicle.getPosition().getX() - CENTER_X;
                break;
            case SOUTH:
                raw = CENTER_X - vehicle.getPosition().getX();
                break;
            default:
                return normalLaneOffsetFor(vehicle);
        }
        double best = OUTER_LANE_OFFSET;
        if (Math.abs(raw - MIDDLE_LANE_OFFSET) < Math.abs(raw - best)) best = MIDDLE_LANE_OFFSET;
        if (Math.abs(raw - INNER_LANE_OFFSET) < Math.abs(raw - best)) best = INNER_LANE_OFFSET;
        return best;
    }

    private void limitSpeedNearVehicle(Vehicle vehicle, Vehicle other, double distance) {
        // Bus được ưu tiên khi nhập cùng làn, xe khác sẽ nhường bus.
        if (!(vehicle instanceof Bus) && other instanceof Bus && distance < PROXIMITY_SLOW_DISTANCE) {
            vehicle.setSpeed(Math.min(vehicle.getSpeed(), 0.7));
            return;
        }

        double targetSpeed;
        if (distance < 50) {
            targetSpeed = 0.7;
        } else if (distance < 70) {
            targetSpeed = 1.2;
        } else {
            targetSpeed = 1.8;
        }
        vehicle.setSpeed(Math.min(vehicle.getSpeed(), targetSpeed));
    }

    private Vehicle findNearestRiskVehicle(Vehicle vehicle) {
        Vehicle nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Vehicle other : vehicles) {
            if (other == vehicle) {
                continue;
            }

            double distance = distanceBetween(vehicle, other);
            if (distance >= PROXIMITY_SLOW_DISTANCE) {
                continue;
            }

            boolean sameLaneAhead = vehicle.getDirection() == other.getDirection()
                    && lateralDistanceBetween(vehicle, other) < 24
                    && distanceInFront(vehicle, other) >= 0;
            boolean junctionRisk = (isInsideConflictBox(vehicle) || isInsideConflictBox(other)
                    || turningVehicles.containsKey(vehicle) || turningVehicles.containsKey(other))
                    && distance < JUNCTION_SLOW_DISTANCE
                    && shouldYieldForMerge(vehicle, other);
            boolean targetMergeRisk = willMergeIntoSameLane(vehicle, other)
                    && distance < JUNCTION_SLOW_DISTANCE
                    && shouldYieldForMerge(vehicle, other);

            if ((sameLaneAhead || junctionRisk || targetMergeRisk) && distance < nearestDistance) {
                nearest = other;
                nearestDistance = distance;
            }
        }
        return nearest;
    }


    private boolean shouldYieldForMerge(Vehicle vehicle, Vehicle other) {
        if (isEmergency(vehicle)) return false;
        if (isEmergency(other)) return true;
        if (vehicle instanceof Bus && !(other instanceof Bus)) return false;
        if (other instanceof Bus && !(vehicle instanceof Bus)) return true;

        Direction myExit = exitDirectionFor(vehicle);
        Direction otherExit = exitDirectionFor(other);
        if (myExit == otherExit) {
            double myLaneError = distanceToExitLane(vehicle, myExit);
            double otherLaneError = distanceToExitLane(other, otherExit);
            if (Math.abs(myLaneError - otherLaneError) > 5) {
                // Xe nào đã gần đúng làn nhập hơn thì được đi trước, xe kia nhường.
                return myLaneError > otherLaneError;
            }
        }

        double myProgress = progressToExit(vehicle, myExit);
        double otherProgress = progressToExit(other, otherExit);
        if (Math.abs(myProgress - otherProgress) > 8) {
            return myProgress < otherProgress;
        }

        return vehicle.getId().compareTo(other.getId()) > 0;
    }

    private Direction exitDirectionFor(Vehicle vehicle) {
        TurnChoice choice = getTurnChoice(vehicle);
        return choice == TurnChoice.STRAIGHT ? vehicle.getDirection() : directionAfterTurn(vehicle.getDirection(), choice);
    }

    private double distanceToExitLane(Vehicle vehicle, Direction exitDirection) {
        double lane = laneCenterForExit(vehicle, exitDirection);
        switch (exitDirection) {
            case EAST:
            case WEST:
                return Math.abs(vehicle.getPosition().getY() - lane);
            case NORTH:
            case SOUTH:
                return Math.abs(vehicle.getPosition().getX() - lane);
            default:
                return Double.MAX_VALUE;
        }
    }

    private double progressToExit(Vehicle vehicle, Direction exitDirection) {
        switch (exitDirection) {
            case EAST:
                return vehicle.getPosition().getX();
            case WEST:
                return -vehicle.getPosition().getX();
            case SOUTH:
                return vehicle.getPosition().getY();
            case NORTH:
                return -vehicle.getPosition().getY();
            default:
                return 0;
        }
    }

    private boolean willMergeIntoSameLane(Vehicle vehicle, Vehicle other) {
        TurnChoice myChoice = getTurnChoice(vehicle);
        TurnChoice otherChoice = getTurnChoice(other);
        Direction myExit = myChoice == TurnChoice.STRAIGHT ? vehicle.getDirection() : directionAfterTurn(vehicle.getDirection(), myChoice);
        Direction otherExit = otherChoice == TurnChoice.STRAIGHT ? other.getDirection() : directionAfterTurn(other.getDirection(), otherChoice);
        if (myExit != otherExit) {
            return false;
        }
        return Math.abs(laneCenterForExit(vehicle, myExit) - laneCenterForExit(other, otherExit)) < 26;
    }

    private boolean isFrontVehicleTooSlow(Vehicle vehicle, Vehicle frontVehicle) {
        return frontVehicle.getSpeed() + 0.5 < vehicle.getSpeed();
    }

    private void wakeVehicleWhenPathIsClear(Vehicle vehicle) {
        if (blockedVehicles.contains(vehicle)
                || turningVehicles.containsKey(vehicle)
                || laneChangingVehicles.containsKey(vehicle)) {
            return;
        }
        TrafficLight light = getTrafficLightForDirection(vehicle.getDirection());
        if (!hasCommittedToIntersection(vehicle) && light != null && light.isRed()) {
            return;
        }
        Vehicle front = findNearestFrontVehicle(vehicle);
        if (front != null) {
            double gap = distanceInFront(vehicle, front);
            if (gap >= 0 && gap < SAFE_DISTANCE) {
                return;
            }
        }
        if (vehicle.getSpeed() < 0.25) {
            vehicle.setState(VehicleState.MOVING);
            vehicle.accelerate();
            vehicle.accelerate();
        }
    }

    private void recoverStalledVehicles() {
        for (Vehicle vehicle : vehicles) {
            if (vehicle.getSpeed() < 0.05) {
                int ticks = stalledTicks.merge(vehicle, 1, Integer::sum);
                if (ticks < 12) {
                    continue;
                }
                TrafficLight light = getTrafficLightForDirection(vehicle.getDirection());
                boolean redStop = !hasCommittedToIntersection(vehicle)
                        && light != null && light.isRed()
                        && distanceToOwnStopCenter(vehicle) >= -0.5;
                Vehicle front = findNearestFrontVehicle(vehicle);
                boolean frontBlocked = front != null
                        && distanceInFront(vehicle, front) >= 0
                        && distanceInFront(vehicle, front) < SAFE_DISTANCE;
                boolean reservationBlocked = intersectionOwner != null
                        && intersectionOwner != vehicle
                        && distanceToIntersection(vehicle) < INTERSECTION_RESERVATION_DISTANCE;
                if (!redStop && !frontBlocked && !reservationBlocked
                        && !turningVehicles.containsKey(vehicle)
                        && !laneChangingVehicles.containsKey(vehicle)) {
                    blockedVehicles.remove(vehicle);
                    vehicle.setState(VehicleState.MOVING);
                    vehicle.accelerate();
                    vehicle.accelerate();
                    stalledTicks.put(vehicle, 0);
                }
            } else {
                stalledTicks.put(vehicle, 0);
            }
        }
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

    private double distanceInFrontFromPosition(Vehicle currentVehicle,
                                               Vehicle candidate,
                                               double currentX,
                                               double currentY) {
        switch (currentVehicle.getDirection()) {
            case NORTH:
                return currentY - candidate.getPosition().getY();
            case SOUTH:
                return candidate.getPosition().getY() - currentY;
            case EAST:
                return candidate.getPosition().getX() - currentX;
            case WEST:
                return currentX - candidate.getPosition().getX();
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
        if (vehiclesThatTurned.contains(vehicle)) {
            return;
        }

        TurnChoice turnChoice = getTurnChoice(vehicle);
        if (turnChoice == TurnChoice.STRAIGHT) {
            if (hasPassedIntersection(vehicle)) {
                vehiclesPastStopLine.add(vehicle);
                vehiclesThatTurned.add(vehicle);
                vehicle.setState(VehicleState.MOVING);
                if (vehicle.getSpeed() < 1.0) {
                    vehicle.setSpeed(1.0);
                }
            }
            return;
        }

        if (!isAtSteeringPoint(vehicle, turnChoice)) {
            return;
        }

        if (turnChoice == TurnChoice.LEFT && !isEmergency(vehicle) && hasOpposingTrafficNearIntersection(vehicle)) {
            vehicle.brake();
            blockVehicle(vehicle);
            return;
        }

        Direction newDirection = directionAfterTurn(vehicle.getDirection(), turnChoice);
        double targetX = targetLaneX(vehicle, newDirection, turnChoice);
        double targetY = targetLaneY(vehicle, newDirection, turnChoice);
        if (!canTurnSafely(vehicle, targetX, targetY)) {
            vehicle.brake();
            blockVehicle(vehicle);
            return;
        }

        double startX = vehicle.getPosition().getX();
        double startY = vehicle.getPosition().getY();
        double leadIn = turnChoice == TurnChoice.LEFT ? 74 : 46;
        double leadOut = turnChoice == TurnChoice.LEFT ? 82 : 58;
        if (vehicle instanceof Bus) {
            leadIn += 18;
            leadOut += 22;
        }
        double control1X = startX + dirX(vehicle.getDirection()) * leadIn;
        double control1Y = startY + dirY(vehicle.getDirection()) * leadIn;
        double control2X = targetX - dirX(newDirection) * leadOut;
        double control2Y = targetY - dirY(newDirection) * leadOut;
        int steps = turnStepsFor(vehicle, turnChoice);

        // Cubic Bezier: đi thẳng tới điểm đánh lái -> quay đầu xe từ từ -> trả lái đúng làn ra.
        // Mỗi làn/loại xe có start và target lane riêng nên không còn dồn vào một đường cong.
        vehiclesPastStopLine.add(vehicle);
        turningVehicles.put(vehicle, new PendingTurn(vehicle.getDirection(), newDirection,
                turnChoice, startX, startY,
                control1X, control1Y, control2X, control2Y, targetX, targetY, steps));
        vehicle.clearIndicators();
        if (turnChoice == TurnChoice.LEFT) {
            vehicle.setLeftIndicatorOn(true);
        } else if (turnChoice == TurnChoice.RIGHT) {
            vehicle.setRightIndicatorOn(true);
        }
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

    private boolean isAtSteeringPoint(Vehicle vehicle, TurnChoice choice) {
        // Rẽ phải: đánh lái trước mép giao lộ như khi gương phải trùng mép đường.
        // Rẽ trái: đi sâu hơn vào ngã tư rồi mới đánh lái, tránh cắt sang làn ngược chiều.
        double trigger = choice == TurnChoice.RIGHT ? RIGHT_TURN_START_DISTANCE : LEFT_TURN_START_DISTANCE;
        switch (vehicle.getDirection()) {
            case EAST:
                return vehicle.getPosition().getX() >= CENTER_X - trigger;
            case WEST:
                return vehicle.getPosition().getX() <= CENTER_X + trigger;
            case NORTH:
                return vehicle.getPosition().getY() <= CENTER_Y + trigger;
            case SOUTH:
                return vehicle.getPosition().getY() >= CENTER_Y - trigger;
            default:
                return false;
        }
    }

    private boolean hasPassedIntersection(Vehicle vehicle) {
        switch (vehicle.getDirection()) {
            case EAST:
                return vehicle.getPosition().getX() > CENTER_X + CONFLICT_RESERVATION_DISTANCE;
            case WEST:
                return vehicle.getPosition().getX() < CENTER_X - CONFLICT_RESERVATION_DISTANCE;
            case NORTH:
                return vehicle.getPosition().getY() < CENTER_Y - CONFLICT_RESERVATION_DISTANCE;
            case SOUTH:
                return vehicle.getPosition().getY() > CENTER_Y + CONFLICT_RESERVATION_DISTANCE;
            default:
                return false;
        }
    }

    private TurnChoice getTurnChoice(Vehicle vehicle) {
        TurnChoice existingChoice = turnChoices.get(vehicle);
        if (existingChoice != null) {
            return existingChoice;
        }

        int routeIndex = random.nextInt(100);
        TurnChoice choice;
        if (routeIndex < 45) {
            choice = TurnChoice.STRAIGHT;
        } else if (routeIndex < 72) {
            choice = TurnChoice.RIGHT;
        } else {
            choice = TurnChoice.LEFT;
        }
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

        int nextStep = pendingTurn.currentStep + 1;
        double t = Math.min(1.0, (double) nextStep / pendingTurn.totalSteps);
        double theta = t * Math.PI / 2.0;
        double dx = pendingTurn.targetX - pendingTurn.startX;
        double dy = pendingTurn.targetY - pendingTurn.startY;

        double nextX;
        double nextY;
        double tangentX;
        double tangentY;

        // Cung 1/4 ellipse nằm hoàn toàn trong hình chữ nhật tạo bởi điểm đầu/điểm cuối.
        // Không có control point nằm ngoài mặt đường nên xe không thể cua xuyên lề/tường.
        if (pendingTurn.sourceDirection == Direction.EAST
                || pendingTurn.sourceDirection == Direction.WEST) {
            nextX = pendingTurn.startX + dx * Math.sin(theta);
            nextY = pendingTurn.startY + dy * (1.0 - Math.cos(theta));
            tangentX = dx * Math.cos(theta);
            tangentY = dy * Math.sin(theta);
        } else {
            nextX = pendingTurn.startX + dx * (1.0 - Math.cos(theta));
            nextY = pendingTurn.startY + dy * Math.sin(theta);
            tangentX = dx * Math.sin(theta);
            tangentY = dy * Math.cos(theta);
        }

        if (!isPositionSafeForVehicleAt(vehicle, nextX, nextY)) {
            vehicle.brake();
            vehicle.setState(VehicleState.WAITING);
            return;
        }

        pendingTurn.currentStep = nextStep;
        vehicle.getPosition().setX(nextX);
        vehicle.getPosition().setY(nextY);
        vehicle.setState(VehicleState.MOVING);

        if (Math.hypot(tangentX, tangentY) > 0.001) {
            vehicle.setVisualHeadingDegrees(Math.toDegrees(Math.atan2(tangentY, tangentX)));
        }

        boolean reachedExit = t >= 0.995
                || (Math.abs(nextX - pendingTurn.targetX) <= TURN_COMPLETE_TOLERANCE
                    && Math.abs(nextY - pendingTurn.targetY) <= TURN_COMPLETE_TOLERANCE);
        if (reachedExit) {
            finishTurn(vehicle, pendingTurn);
        }
    }

    private void finishTurn(Vehicle vehicle, PendingTurn pendingTurn) {
        vehicle.getPosition().setX(pendingTurn.targetX);
        vehicle.getPosition().setY(pendingTurn.targetY);
        vehicle.setDirection(pendingTurn.targetDirection);
        vehicle.setVisualHeadingDegrees(headingForDirection(pendingTurn.targetDirection));
        vehicle.clearIndicators();
        assignedLaneOffsets.put(vehicle, normalLaneOffsetFor(vehicle));
        turningVehicles.remove(vehicle);
        laneChangingVehicles.remove(vehicle);
        vehiclesPastStopLine.add(vehicle);
        vehiclesThatTurned.add(vehicle);
        vehicle.setState(VehicleState.MOVING);
        if (vehicle.getSpeed() < 1.2) {
            vehicle.setSpeed(1.2);
        }
        vehicle.accelerate();
    }

    private boolean canTurnSafely(Vehicle vehicle, double targetX, double targetY) {
        for (Vehicle other : vehicles) {
            if (other == vehicle) {
                continue;
            }
            double dx = targetX - other.getPosition().getX();
            double dy = targetY - other.getPosition().getY();
            if (Math.sqrt(dx * dx + dy * dy) < SAFE_DISTANCE * 1.10) {
                if (vehicle instanceof Bus && !(other instanceof Bus) && !isEmergency(other)) {
                    other.brake();
                    blockVehicle(other);
                    return true;
                }
                return false;
            }
        }
        return true;
    }

    private double dirX(Direction direction) {
        switch (direction) {
            case EAST:
                return 1;
            case WEST:
                return -1;
            default:
                return 0;
        }
    }

    private double dirY(Direction direction) {
        switch (direction) {
            case SOUTH:
                return 1;
            case NORTH:
                return -1;
            default:
                return 0;
        }
    }

    private int turnStepsFor(Vehicle vehicle, TurnChoice turnChoice) {
        int baseSteps = turnChoice == TurnChoice.LEFT ? 56 : 44;
        if (vehicle instanceof Bus) {
            baseSteps += 10;
        }
        return baseSteps;
    }

    private double targetLaneX(Vehicle vehicle, Direction direction, TurnChoice turnChoice) {
        double exitDistance = turnChoice == TurnChoice.RIGHT ? 182 : TURN_EXIT_OFFSET;
        switch (direction) {
            case EAST:
                return CENTER_X + exitDistance;
            case WEST:
                return CENTER_X - exitDistance;
            case NORTH:
            case SOUTH:
                return laneCenterForExit(vehicle, direction);
            default:
                return CENTER_X;
        }
    }

    private double targetLaneY(Vehicle vehicle, Direction direction, TurnChoice turnChoice) {
        double exitDistance = turnChoice == TurnChoice.RIGHT ? 182 : TURN_EXIT_OFFSET;
        switch (direction) {
            case EAST:
            case WEST:
                return laneCenterForExit(vehicle, direction);
            case NORTH:
                return CENTER_Y - exitDistance;
            case SOUTH:
                return CENTER_Y + exitDistance;
            default:
                return CENTER_Y;
        }
    }

    private void preventCollisions() {
        for (int i = 0; i < vehicles.size(); i++) {
            Vehicle vehicleA = vehicles.get(i);

            for (int j = i + 1; j < vehicles.size(); j++) {
                Vehicle vehicleB = vehicles.get(j);

                if (areVehiclesActuallyOverlapping(vehicleA, vehicleB)) {
                    if (isInsideConflictBox(vehicleA) || isInsideConflictBox(vehicleB)
                            || turningVehicles.containsKey(vehicleA) || turningVehicles.containsKey(vehicleB)) {
                        resolveJunctionCollision(vehicleA, vehicleB);
                    } else {
                        vehicleA.brake();
                        vehicleB.brake();
                        separateVehicles(vehicleA, vehicleB);
                    }
                    soundManager.playHorn();
                }
            }
        }
    }


    private boolean areVehiclesActuallyOverlapping(Vehicle first, Vehicle second) {
        if (first.getDirection() == second.getDirection()) {
            double lateralGap = lateralDistanceBetween(first, second);
            double longitudinalGap;
            if (first.getDirection() == Direction.EAST || first.getDirection() == Direction.WEST) {
                longitudinalGap = Math.abs(first.getPosition().getX() - second.getPosition().getX());
            } else {
                longitudinalGap = Math.abs(first.getPosition().getY() - second.getPosition().getY());
            }
            return lateralGap < (vehicleWidth(first) + vehicleWidth(second)) / 2.0
                    && longitudinalGap < (vehicleLength(first) + vehicleLength(second)) / 2.0;
        }

        if (!(isInsideConflictBox(first) || isInsideConflictBox(second)
                || turningVehicles.containsKey(first) || turningVehicles.containsKey(second))) {
            return false;
        }
        double threshold = Math.max(18.0,
                (vehicleWidth(first) + vehicleWidth(second)) / 2.0 + 4.0);
        return distanceBetween(first, second) < threshold;
    }

    private void resolveJunctionCollision(Vehicle vehicleA, Vehicle vehicleB) {
        Vehicle yielding = chooseYieldingVehicle(vehicleA, vehicleB);
        Vehicle priority = yielding == vehicleA ? vehicleB : vehicleA;

        yielding.stop();
        yielding.setState(VehicleState.WAITING);
        // Không đẩy/teleport xe lùi về sau nữa. Xe ưu tiên đi tiếp, xe nhường dừng mềm tại chỗ.
        if (priority.getSpeed() < 1.0) {
            priority.accelerate();
        }
    }

    private Vehicle chooseYieldingVehicle(Vehicle vehicleA, Vehicle vehicleB) {
        // Bus không có làn riêng nữa, nhưng được ưu tiên hơn ô tô/xe máy khi nhập cùng làn.
        if (vehicleA instanceof Bus && !(vehicleB instanceof Bus) && !isEmergency(vehicleB)) {
            return vehicleB;
        }
        if (vehicleB instanceof Bus && !(vehicleA instanceof Bus) && !isEmergency(vehicleA)) {
            return vehicleA;
        }
        if (willMergeIntoSameLane(vehicleA, vehicleB)) {
            return shouldYieldForMerge(vehicleA, vehicleB) ? vehicleA : vehicleB;
        }
        if (turningVehicles.containsKey(vehicleA) && !turningVehicles.containsKey(vehicleB)) {
            return vehicleB;
        }
        if (turningVehicles.containsKey(vehicleB) && !turningVehicles.containsKey(vehicleA)) {
            return vehicleA;
        }
        double distanceA = Math.hypot(vehicleA.getPosition().getX() - CENTER_X, vehicleA.getPosition().getY() - CENTER_Y);
        double distanceB = Math.hypot(vehicleB.getPosition().getX() - CENTER_X, vehicleB.getPosition().getY() - CENTER_Y);
        return distanceA <= distanceB ? vehicleB : vehicleA;
    }

    private void moveBackward(Vehicle vehicle, double amount) {
        switch (vehicle.getDirection()) {
            case EAST:
                vehicle.getPosition().setX(vehicle.getPosition().getX() - amount);
                break;
            case WEST:
                vehicle.getPosition().setX(vehicle.getPosition().getX() + amount);
                break;
            case NORTH:
                vehicle.getPosition().setY(vehicle.getPosition().getY() + amount);
                break;
            case SOUTH:
                vehicle.getPosition().setY(vehicle.getPosition().getY() - amount);
                break;
            default:
                break;
        }
    }

    private void separateVehicles(Vehicle vehicleA, Vehicle vehicleB) {
        // Không đẩy ngang/teleport khi va chạm. Xe phía sau/phải nhường sẽ dừng mềm, xe ưu tiên đi tiếp.
        Vehicle yielding = chooseYieldingVehicle(vehicleA, vehicleB);
        yielding.stop();
        blockVehicle(yielding);
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
        return laneCenterForOffset(laneOffsetFor(vehicle), direction);
    }

    private double laneCenterForExit(Vehicle vehicle, Direction direction) {
        return laneCenterForOffset(normalLaneOffsetFor(vehicle), direction);
    }

    private double laneCenterForOffset(double offset, Direction direction) {
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
        // Never snap a vehicle sideways into a turn pocket. The current lane is
        // changed only by PendingLaneChange, which produces the boomerang curve.
        return assignedLaneOffsets.getOrDefault(vehicle, normalLaneOffsetFor(vehicle));
    }

    private double normalLaneOffsetFor(Vehicle vehicle) {
        // Bus đã bỏ làn riêng, đi chung làn ngoài với ô tô.
        if (vehicle instanceof Bicycle) {
            return INNER_LANE_OFFSET;
        }
        if (vehicle instanceof Motorbike) {
            return MIDDLE_LANE_OFFSET;
        }
        return OUTER_LANE_OFFSET;
    }

    private double headingForDirection(Direction direction) {
        switch (direction) {
            case SOUTH:
                return 90;
            case WEST:
                return 180;
            case NORTH:
                return 270;
            case EAST:
            default:
                return 0;
        }
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
                laneChangingVehicles.remove(vehicle);
                assignedLaneOffsets.remove(vehicle);
                vehicle.clearIndicators();
                vehiclesThatTurned.remove(vehicle);
                vehiclesPastStopLine.remove(vehicle);
                stalledTicks.remove(vehicle);
                if (intersectionOwner == vehicle) {
                    intersectionOwner = null;
                    intersectionOwnerIdleTicks = 0;
                }
            }
        }
    }
}