package controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import model.map.TrafficMapType;
import model.trafficlight.TrafficLight;
import model.vehicle.Ambulance;
import model.vehicle.Bicycle;
import model.vehicle.Bus;
import model.vehicle.FireTruck;
import model.vehicle.Motorbike;
import model.vehicle.Vehicle;
import model.vehicle.VehicleState;
import util.Direction;
import util.Vector2D;

/**
 * Route based movement engine used by the T junction, five-way junction and
 * the wide road network.  The original VehicleController is intentionally
 * retained for the detailed four-way intersection simulation.
 */
public class MapRouteEngine {

    private static final double SAMPLE_STEP = 5.0;
    /** Khoảng trống tối thiểu giữa đuôi xe trước và đầu xe sau (pixel). */
    private static final double MIN_BUMPER_GAP = 28.0;
    private static final double LARGE_VEHICLE_BUMPER_GAP = 36.0;
    private static final double REACTION_DISTANCE_FACTOR = 2.35;
    private static final double FOLLOWING_SLOW_BUFFER = 64.0;
    private static final double SAME_LANE_CORRIDOR = 21.0;
    private static final double BRAKING_DISTANCE = 112.0;
    private static final double STOP_DISTANCE = 10.0;

    /**
     * Xe chỉ đổi sang tuyến đi thẳng khi còn ở trước điểm bắt đầu rẽ.
     * Nhờ vậy xe không bị dịch chuyển đột ngột sau khi đã vào giữa cung cua.
     */
    private static final double STRAIGHT_FALLBACK_LOOKAHEAD = 58.0;
    private static final double TURN_PATH_CHECK_DISTANCE = 82.0;
    private static final double FALLBACK_PATH_CHECK_DISTANCE = 86.0;
    private static final double PATH_CHECK_STEP = 8.0;
    private static final double T_JUNCTION_TURN_GAP_LOOKAHEAD = 126.0;
    private static final double T_JUNCTION_TURN_LANE_TOLERANCE = 20.0;
    private static final double T_JUNCTION_TURN_ACCELERATION = 0.82;
    private static final int T_JUNCTION_TURN_BOOST_TICKS = 16;
    /**
     * Xe ưu tiên chỉ đổi sang đi thẳng khi vẫn còn ở trước điểm bắt đầu cua.
     * Khoảng nhìn này đủ để đổi route êm, không làm xe nhảy vị trí giữa giao lộ.
     */
    private static final double T_JUNCTION_EMERGENCY_FALLBACK_LOOKAHEAD = 176.0;
    private static final double T_JUNCTION_EMERGENCY_TURN_CHECK_DISTANCE = 104.0;

    private static final class Gate {
        private final double progress;
        private final int lightIndex;

        private Gate(double progress, int lightIndex) {
            this.progress = progress;
            this.lightIndex = lightIndex;
        }
    }

    private static final class GateSpec {
        private final double x;
        private final double y;
        private final int lightIndex;

        private GateSpec(double x, double y, int lightIndex) {
            this.x = x;
            this.y = y;
            this.lightIndex = lightIndex;
        }
    }

    private static final class FallbackSpec {
        private final double x;
        private final double y;
        private final String straightRouteId;

        private FallbackSpec(double x, double y, String straightRouteId) {
            this.x = x;
            this.y = y;
            this.straightRouteId = straightRouteId;
        }
    }

    private static final class StraightFallback {
        private final double decisionProgress;
        private final String straightRouteId;

        private StraightFallback(double decisionProgress, String straightRouteId) {
            this.decisionProgress = decisionProgress;
            this.straightRouteId = straightRouteId;
        }
    }

    private static final class RouteDefinition {
        private final String id;
        private final List<Vector2D> points;
        private final double[] cumulative;
        private final double totalLength;
        private final List<Gate> gates;
        private final List<StraightFallback> straightFallbacks;

        private RouteDefinition(String id,
                                List<Vector2D> points,
                                List<GateSpec> gateSpecs,
                                List<FallbackSpec> fallbackSpecs) {
            this.id = id;
            this.points = points;
            this.cumulative = new double[points.size()];
            double total = 0.0;
            for (int i = 1; i < points.size(); i++) {
                total += points.get(i - 1).distance(points.get(i));
                cumulative[i] = total;
            }
            this.totalLength = total;
            List<Gate> resolved = new ArrayList<>();
            for (GateSpec spec : gateSpecs) {
                resolved.add(new Gate(nearestProgress(spec.x, spec.y), spec.lightIndex));
            }
            resolved.sort((a, b) -> Double.compare(a.progress, b.progress));
            this.gates = Collections.unmodifiableList(resolved);

            List<StraightFallback> resolvedFallbacks = new ArrayList<>();
            for (FallbackSpec spec : fallbackSpecs) {
                resolvedFallbacks.add(new StraightFallback(
                        nearestProgress(spec.x, spec.y), spec.straightRouteId));
            }
            resolvedFallbacks.sort((a, b) -> Double.compare(a.decisionProgress, b.decisionProgress));
            this.straightFallbacks = Collections.unmodifiableList(resolvedFallbacks);
        }

        private double nearestProgress(double x, double y) {
            double bestDistance = Double.MAX_VALUE;
            double bestProgress = 0.0;
            for (int i = 0; i < points.size(); i++) {
                Vector2D point = points.get(i);
                double dx = point.getX() - x;
                double dy = point.getY() - y;
                double distance = dx * dx + dy * dy;
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestProgress = cumulative[i];
                }
            }
            return bestProgress;
        }

        private Vector2D pointAt(double progress) {
            if (points.isEmpty()) {
                return new Vector2D(0, 0);
            }
            double p = clamp(progress, 0.0, totalLength);
            int low = 0;
            int high = cumulative.length - 1;
            while (low < high) {
                int mid = (low + high + 1) >>> 1;
                if (cumulative[mid] <= p) {
                    low = mid;
                } else {
                    high = mid - 1;
                }
            }
            int index = Math.min(low, points.size() - 2);
            double segmentStart = cumulative[index];
            double segmentEnd = cumulative[index + 1];
            double t = segmentEnd <= segmentStart ? 0.0 : (p - segmentStart) / (segmentEnd - segmentStart);
            Vector2D a = points.get(index);
            Vector2D b = points.get(index + 1);
            return new Vector2D(
                    a.getX() + (b.getX() - a.getX()) * t,
                    a.getY() + (b.getY() - a.getY()) * t);
        }

        private double headingAt(double progress) {
            Vector2D a = pointAt(progress);
            Vector2D b = pointAt(Math.min(totalLength, progress + 12.0));
            if (a.distance(b) < 0.1) {
                b = pointAt(Math.max(0.0, progress - 12.0));
                return normalizeDegrees(Math.toDegrees(Math.atan2(a.getY() - b.getY(), a.getX() - b.getX())));
            }
            return normalizeDegrees(Math.toDegrees(Math.atan2(b.getY() - a.getY(), b.getX() - a.getX())));
        }
    }

    private static final class RouteState {
        private RouteDefinition route;
        private double progress;

        private RouteState(RouteDefinition route, double progress) {
            this.route = route;
            this.progress = progress;
        }
    }

    private static final class TurnYieldSpec {
        private final String routeId;
        private final String blockingRouteId;
        private final double decisionX;
        private final double decisionY;
        private final double laneY;
        private final double minX;
        private final double maxX;

        private TurnYieldSpec(String routeId,
                              String blockingRouteId,
                              double decisionX,
                              double decisionY,
                              double laneY,
                              double minX,
                              double maxX) {
            this.routeId = routeId;
            this.blockingRouteId = blockingRouteId;
            this.decisionX = decisionX;
            this.decisionY = decisionY;
            this.laneY = laneY;
            this.minX = minX;
            this.maxX = maxX;
        }
    }

    private enum FallbackAction {
        NONE,
        SWITCHED_TO_STRAIGHT,
        WAIT_FOR_CLEAR_PATH
    }

    private static final class RouteBuilder {
        private final String id;
        private final List<Vector2D> points = new ArrayList<>();
        private final List<GateSpec> gates = new ArrayList<>();
        private final List<FallbackSpec> fallbackSpecs = new ArrayList<>();

        private RouteBuilder(String id, double x, double y) {
            this.id = id;
            points.add(new Vector2D(x, y));
        }

        private RouteBuilder lineTo(double x, double y) {
            Vector2D start = points.get(points.size() - 1);
            double length = start.distance(new Vector2D(x, y));
            int samples = Math.max(1, (int) Math.ceil(length / SAMPLE_STEP));
            for (int i = 1; i <= samples; i++) {
                double t = i / (double) samples;
                points.add(new Vector2D(
                        start.getX() + (x - start.getX()) * t,
                        start.getY() + (y - start.getY()) * t));
            }
            return this;
        }

        private RouteBuilder quadTo(double controlX, double controlY, double x, double y) {
            Vector2D start = points.get(points.size() - 1);
            double estimate = start.distance(new Vector2D(controlX, controlY))
                    + new Vector2D(controlX, controlY).distance(new Vector2D(x, y));
            int samples = Math.max(8, (int) Math.ceil(estimate / SAMPLE_STEP));
            for (int i = 1; i <= samples; i++) {
                double t = i / (double) samples;
                double u = 1.0 - t;
                points.add(new Vector2D(
                        u * u * start.getX() + 2 * u * t * controlX + t * t * x,
                        u * u * start.getY() + 2 * u * t * controlY + t * t * y));
            }
            return this;
        }

        private RouteBuilder cubicTo(double c1x, double c1y, double c2x, double c2y, double x, double y) {
            Vector2D start = points.get(points.size() - 1);
            double estimate = start.distance(new Vector2D(c1x, c1y))
                    + new Vector2D(c1x, c1y).distance(new Vector2D(c2x, c2y))
                    + new Vector2D(c2x, c2y).distance(new Vector2D(x, y));
            int samples = Math.max(12, (int) Math.ceil(estimate / SAMPLE_STEP));
            for (int i = 1; i <= samples; i++) {
                double t = i / (double) samples;
                double u = 1.0 - t;
                points.add(new Vector2D(
                        u * u * u * start.getX()
                                + 3 * u * u * t * c1x
                                + 3 * u * t * t * c2x
                                + t * t * t * x,
                        u * u * u * start.getY()
                                + 3 * u * u * t * c1y
                                + 3 * u * t * t * c2y
                                + t * t * t * y));
            }
            return this;
        }

        private RouteBuilder gate(double x, double y, int lightIndex) {
            gates.add(new GateSpec(x, y, lightIndex));
            return this;
        }

        /**
         * Khai báo một điểm rẽ có tuyến đi thẳng thay thế.
         * straightRouteId phải trùng với id của một RouteDefinition khác.
         */
        private RouteBuilder straightFallback(double x, double y, String straightRouteId) {
            fallbackSpecs.add(new FallbackSpec(x, y, straightRouteId));
            return this;
        }

        private RouteDefinition build() {
            return new RouteDefinition(
                    id,
                    Collections.unmodifiableList(new ArrayList<>(points)),
                    gates,
                    fallbackSpecs);
        }
    }

    private final VehicleController vehicleController;
    private final TrafficController trafficController;
    private final Map<Vehicle, RouteState> states;
    private final Map<Vehicle, Integer> turnReleaseBoostTicks;
    private final Map<Vehicle, TurnYieldSpec> waitingTJunctionTurns;
    private List<RouteDefinition> routes;
    private TrafficMapType mapType;

    public MapRouteEngine(VehicleController vehicleController, TrafficController trafficController) {
        this.vehicleController = vehicleController;
        this.trafficController = trafficController;
        this.states = new LinkedHashMap<>();
        this.turnReleaseBoostTicks = new HashMap<>();
        this.waitingTJunctionTurns = new HashMap<>();
        configure(TrafficMapType.CROSS_JUNCTION);
    }

    public void configure(TrafficMapType mapType) {
        this.mapType = mapType == null ? TrafficMapType.CROSS_JUNCTION : mapType;
        this.states.clear();
        this.turnReleaseBoostTicks.clear();
        this.waitingTJunctionTurns.clear();
        switch (this.mapType) {
            case T_JUNCTION:
                routes = buildTJunctionRoutes();
                break;
            case ROAD_NETWORK:
                routes = buildNetworkRoutes();
                break;
            case CROSS_JUNCTION:
            default:
                routes = Collections.emptyList();
                break;
        }
    }

    public int getRouteCount() {
        return routes.size();
    }

    public String getRouteName(int routeIndex) {
        if (routes.isEmpty()) return "";
        return routes.get(Math.floorMod(routeIndex, routes.size())).id;
    }

    public Direction getInitialDirection(int routeIndex) {
        if (routes.isEmpty()) return Direction.EAST;
        RouteDefinition route = routes.get(Math.floorMod(routeIndex, routes.size()));
        return cardinalDirection(route.headingAt(0.0));
    }

    public Vector2D getInitialPosition(int routeIndex, int queueIndex) {
        if (routes.isEmpty()) return new Vector2D(0, 0);
        RouteDefinition route = routes.get(Math.floorMod(routeIndex, routes.size()));
        double progress = initialProgress(route, queueIndex);
        return route.pointAt(progress);
    }

    public void registerVehicle(Vehicle vehicle, int routeIndex, int queueIndex) {
        if (vehicle == null || routes.isEmpty()) return;
        RouteDefinition route = routes.get(Math.floorMod(routeIndex, routes.size()));
        double progress = findSafeInitialProgress(vehicle, route, initialProgress(route, queueIndex));
        RouteState state = new RouteState(route, progress);
        states.put(vehicle, state);
        updateVehiclePose(vehicle, state, true);
    }

    public void unregisterVehicle(Vehicle vehicle) {
        states.remove(vehicle);
    }

    public void updateVehicles() {
        if (routes.isEmpty()) return;
        purgeRemovedVehicles();

        Map<Vehicle, Double> desiredSpeeds = new HashMap<>();
        for (Map.Entry<Vehicle, RouteState> entry : states.entrySet()) {
            Vehicle vehicle = entry.getKey();
            RouteState state = entry.getValue();

            FallbackAction fallbackAction = evaluateEmergencyTJunctionStraightFallback(vehicle, state);
            if (fallbackAction == FallbackAction.NONE) {
                fallbackAction = evaluateStraightFallback(vehicle, state);
            }
            if (fallbackAction == FallbackAction.WAIT_FOR_CLEAR_PATH) {
                desiredSpeeds.put(vehicle, 0.0);
                continue;
            }

            double desired = vehicle.getMaxSpeed();
            desired = Math.min(desired, speedAllowedBySignal(vehicle, state));
            desired = Math.min(desired, speedAllowedByTJunctionTurn(vehicle, state));
            desired = Math.min(desired, speedAllowedByLeader(vehicle, state));
            desired = Math.min(desired, speedAllowedByNearbyRoute(vehicle, state));
            desiredSpeeds.put(vehicle, desired);
        }

        Map<Vehicle, Double> progressSnapshot = new HashMap<>();
        for (Map.Entry<Vehicle, RouteState> entry : states.entrySet()) {
            progressSnapshot.put(entry.getKey(), entry.getValue().progress);
        }

        // Cập nhật từng xe nhưng luôn kiểm tra vị trí dự kiến trước khi cho tiến lên.
        // Nhờ vậy xe ở hai route khác nhau nhưng đang dùng chung một đoạn đường
        // (ví dụ Tây → Đông và Tây → Nam) vẫn không thể chồng lên nhau.
        for (Map.Entry<Vehicle, RouteState> entry : states.entrySet()) {
            Vehicle vehicle = entry.getKey();
            RouteState state = entry.getValue();
            double desired = desiredSpeeds.getOrDefault(vehicle, 0.0);
            adjustSpeed(vehicle, desired);

            double advance = Math.min(vehicle.getSpeed(),
                    maximumSafeAdvanceOnSameRoute(vehicle, state, progressSnapshot));
            double nextProgress = wrapProgress(state.route, state.progress + Math.max(0.0, advance));

            if (advance > 0.0 && !isProjectedPositionSafe(vehicle, state, nextProgress)) {
                vehicle.setSpeed(0.0);
                vehicle.setState(VehicleState.WAITING);
                nextProgress = state.progress;
            }

            state.progress = nextProgress;
            updateVehiclePose(vehicle, state, false);
        }
    }

    private double initialProgress(RouteDefinition route, int queueIndex) {
        double firstGate = route.gates.isEmpty() ? route.totalLength * 0.45 : route.gates.get(0).progress;
        double spacing = 78.0;
        return clamp(firstGate - 118.0 - Math.max(0, queueIndex) * spacing, 0.0, route.totalLength - 1.0);
    }

    private void purgeRemovedVehicles() {
        Iterator<Vehicle> iterator = states.keySet().iterator();
        List<Vehicle> existing = vehicleController.getVehicles();
        while (iterator.hasNext()) {
            Vehicle vehicle = iterator.next();
            if (!existing.contains(vehicle)) {
                iterator.remove();
                turnReleaseBoostTicks.remove(vehicle);
                waitingTJunctionTurns.remove(vehicle);
            }
        }
    }

    /**
     * Chỉ dùng cho bản đồ lưới giao thông. Khi xe đang chuẩn bị rẽ nhưng đoạn
     * đường cong phía trước bị chiếm dụng, xe sẽ chuyển sang RouteDefinition
     * đi thẳng tương ứng. Nếu cả hướng rẽ lẫn hướng thẳng đều chưa an toàn,
     * xe dừng trước điểm rẽ và thử lại ở tick tiếp theo.
     */
    private FallbackAction evaluateStraightFallback(Vehicle vehicle, RouteState state) {
        if (mapType != TrafficMapType.ROAD_NETWORK || state.route.straightFallbacks.isEmpty()) {
            return FallbackAction.NONE;
        }

        for (StraightFallback fallback : state.route.straightFallbacks) {
            double distanceToDecision = distanceAhead(state, fallback.decisionProgress);
            if (distanceToDecision < 0.0 || distanceToDecision > STRAIGHT_FALLBACK_LOOKAHEAD) {
                continue;
            }

            double turnCheckStart = Math.min(
                    state.route.totalLength,
                    Math.max(state.progress + PATH_CHECK_STEP, fallback.decisionProgress + PATH_CHECK_STEP));
            double turnCheckEnd = Math.min(
                    state.route.totalLength,
                    fallback.decisionProgress + TURN_PATH_CHECK_DISTANCE);

            if (isRouteSegmentClear(vehicle, state.route, turnCheckStart, turnCheckEnd)) {
                return FallbackAction.NONE;
            }

            RouteDefinition straightRoute = findRouteById(fallback.straightRouteId);
            if (straightRoute == null) {
                return FallbackAction.NONE;
            }

            Vector2D currentPoint = state.route.pointAt(state.progress);
            double straightProgress = straightRoute.nearestProgress(
                    currentPoint.getX(), currentPoint.getY());
            double headingDifference = Math.abs(shortestAngleDelta(
                    state.route.headingAt(state.progress),
                    straightRoute.headingAt(straightProgress)));

            // Không nhảy sang một đoạn đường cắt ngang chỉ vì đó là điểm gần nhất.
            if (headingDifference > 42.0) {
                return FallbackAction.WAIT_FOR_CLEAR_PATH;
            }

            double fallbackCheckStart = Math.min(
                    straightRoute.totalLength, straightProgress + PATH_CHECK_STEP);
            double fallbackCheckEnd = Math.min(
                    straightRoute.totalLength, straightProgress + FALLBACK_PATH_CHECK_DISTANCE);
            if (!isRouteSegmentClear(vehicle, straightRoute, fallbackCheckStart, fallbackCheckEnd)) {
                return FallbackAction.WAIT_FOR_CLEAR_PATH;
            }

            state.route = straightRoute;
            state.progress = straightProgress;
            vehicle.clearIndicators();
            if (vehicle.getSpeed() < 0.65) {
                vehicle.setSpeed(0.65);
            }
            vehicle.setState(VehicleState.MOVING);
            updateVehiclePose(vehicle, state, true);
            return FallbackAction.SWITCHED_TO_STRAIGHT;
        }

        return FallbackAction.NONE;
    }

    /**
     * Quy tắc riêng cho ngã ba: xe cứu thương/cứu hỏa đang định rẽ từ trục
     * Đông - Tây xuống nhánh Nam sẽ không đứng chờ khi cung rẽ hoặc luồng cắt
     * ngang đang bận. Xe hủy rẽ và tiếp tục đi thẳng theo đúng chiều hiện tại.
     *
     * Xe chỉ đổi route trước khi bắt đầu cua. Sau khi đổi route, các kiểm tra
     * khoảng cách phía trước vẫn hoạt động để tránh xe chồng lên nhau.
     */
    private FallbackAction evaluateEmergencyTJunctionStraightFallback(Vehicle vehicle,
                                                                       RouteState state) {
        if (mapType != TrafficMapType.T_JUNCTION
                || !isEmergency(vehicle)
                || state == null
                || state.route == null
                || state.route.straightFallbacks.isEmpty()) {
            return FallbackAction.NONE;
        }

        for (StraightFallback fallback : state.route.straightFallbacks) {
            double distanceToDecision = distanceAhead(state, fallback.decisionProgress);
            if (distanceToDecision < 0.0
                    || distanceToDecision > T_JUNCTION_EMERGENCY_FALLBACK_LOOKAHEAD) {
                continue;
            }

            boolean conflictLaneBusy = false;
            TurnYieldSpec yieldSpec = tJunctionYieldSpec(state.route);
            if (yieldSpec != null) {
                conflictLaneBusy = isTJunctionConflictLaneBusy(vehicle, yieldSpec);
            }

            double turnCheckStart = Math.min(
                    state.route.totalLength,
                    Math.max(state.progress + PATH_CHECK_STEP,
                            fallback.decisionProgress + PATH_CHECK_STEP));
            double turnCheckEnd = Math.min(
                    state.route.totalLength,
                    fallback.decisionProgress + T_JUNCTION_EMERGENCY_TURN_CHECK_DISTANCE);
            boolean turnPathBusy = !isRouteSegmentClear(
                    vehicle, state.route, turnCheckStart, turnCheckEnd);

            if (!conflictLaneBusy && !turnPathBusy) {
                return FallbackAction.NONE;
            }

            RouteDefinition straightRoute = findRouteById(fallback.straightRouteId);
            if (straightRoute == null) {
                return FallbackAction.NONE;
            }

            Vector2D currentPoint = state.route.pointAt(state.progress);
            double straightProgress = straightRoute.nearestProgress(
                    currentPoint.getX(), currentPoint.getY());
            double headingDifference = Math.abs(shortestAngleDelta(
                    state.route.headingAt(state.progress),
                    straightRoute.headingAt(straightProgress)));

            // Chỉ chuyển sang route cùng hướng để xe không bị xoay/nhảy đột ngột.
            if (headingDifference > 24.0) {
                return FallbackAction.NONE;
            }

            state.route = straightRoute;
            state.progress = straightProgress;
            waitingTJunctionTurns.remove(vehicle);
            turnReleaseBoostTicks.put(vehicle, T_JUNCTION_TURN_BOOST_TICKS);
            vehicle.clearIndicators();
            if (vehicle.getSpeed() < 0.85) {
                vehicle.setSpeed(0.85);
            }
            vehicle.setState(VehicleState.MOVING);
            updateVehiclePose(vehicle, state, true);
            return FallbackAction.SWITCHED_TO_STRAIGHT;
        }

        return FallbackAction.NONE;
    }

    private boolean isRouteSegmentClear(Vehicle vehicle,
                                        RouteDefinition route,
                                        double startProgress,
                                        double endProgress) {
        if (endProgress <= startProgress) {
            return true;
        }
        RouteState probe = new RouteState(route, startProgress);
        for (double progress = startProgress; progress <= endProgress; progress += PATH_CHECK_STEP) {
            if (!isProjectedPositionSafe(vehicle, probe, progress)) {
                return false;
            }
        }
        return true;
    }

    private RouteDefinition findRouteById(String id) {
        if (id == null) return null;
        for (RouteDefinition route : routes) {
            if (id.equals(route.id)) {
                return route;
            }
        }
        return null;
    }

    private double speedAllowedBySignal(Vehicle vehicle, RouteState state) {
        Gate next = nextGate(state);
        if (next == null) return vehicle.getMaxSpeed();
        List<TrafficLight> lights = trafficController.getTrafficLights();
        if (lights.isEmpty()) return vehicle.getMaxSpeed();
        TrafficLight light = lights.get(Math.floorMod(next.lightIndex, lights.size()));

        // Ngã ba: xe Nam → Đông đang ở làn rẽ phải chuyên dụng được phép
        // rẽ phải khi đèn đỏ. Xe vẫn phải nhường luồng Tây → Đông thông qua
        // speedAllowedByTJunctionTurn(), nên không được cắt đầu xe đang đi ngang.
        if (light.isRed() && isTJunctionRightTurnOnRedLane(state)) {
            return vehicle.getMaxSpeed();
        }

        boolean mustStop = light.isRed() || (light.isYellow() && distanceAhead(state, next.progress) < 48.0);
        if (!mustStop) return vehicle.getMaxSpeed();

        double distance = distanceAhead(state, next.progress);
        double frontAllowance = vehicle.getId().startsWith("BUS") ? 23.0 : 15.0;
        double remaining = distance - frontAllowance;
        if (remaining <= STOP_DISTANCE) return 0.0;
        if (remaining < BRAKING_DISTANCE) {
            double ratio = clamp(remaining / BRAKING_DISTANCE, 0.0, 1.0);
            double minimumRollingSpeed = isEmergency(vehicle) ? 0.75 : 0.35;
            return Math.max(minimumRollingSpeed, vehicle.getMaxSpeed() * ratio * ratio);
        }
        return vehicle.getMaxSpeed();
    }

    private boolean isTJunctionRightTurnOnRedLane(RouteState state) {
        return mapType == TrafficMapType.T_JUNCTION
                && state != null
                && state.route != null
                && "Nam → Đông".equals(state.route.id);
    }

    private double speedAllowedByTJunctionTurn(Vehicle vehicle, RouteState state) {
        if (mapType != TrafficMapType.T_JUNCTION) {
            waitingTJunctionTurns.remove(vehicle);
            return vehicle.getMaxSpeed();
        }

        TurnYieldSpec spec = tJunctionYieldSpec(state.route);
        if (spec == null) {
            waitingTJunctionTurns.remove(vehicle);
            return vehicle.getMaxSpeed();
        }

        double decisionProgress = state.route.nearestProgress(spec.decisionX, spec.decisionY);
        double distanceToDecision = distanceAhead(state, decisionProgress);
        if (distanceToDecision < 0.0 || distanceToDecision > T_JUNCTION_TURN_GAP_LOOKAHEAD) {
            return vehicle.getMaxSpeed();
        }

        if (isTJunctionConflictLaneBusy(vehicle, spec)) {
            waitingTJunctionTurns.put(vehicle, spec);
            turnReleaseBoostTicks.remove(vehicle);
            return 0.0;
        }

        if (waitingTJunctionTurns.remove(vehicle) != null) {
            turnReleaseBoostTicks.put(vehicle, T_JUNCTION_TURN_BOOST_TICKS);
        }
        return vehicle.getMaxSpeed();
    }

    private TurnYieldSpec tJunctionYieldSpec(RouteDefinition route) {
        if (route == null) return null;
        switch (route.id) {
            case "Nam → Tây":
                return new TurnYieldSpec(route.id, "Đông → Tây", 474, 452, 332, 318, 620);
            case "Nam → Đông":
                // Rẽ phải được phép đi khi đèn đỏ, nhưng phải nhường xe Tây → Đông.
                return new TurnYieldSpec(route.id, "Tây → Đông", 537, 452, 368, 250, 610);
            case "Đông → Nam":
                // Đây là hướng rẽ trái cắt qua làn Tây → Đông, vì vậy phải
                // dừng trước vùng giao cắt và chỉ tăng tốc rẽ sau khi xe ngang đi qua.
                return new TurnYieldSpec(route.id, "Tây → Đông", 590, 332, 368, 140, 680);
            default:
                return null;
        }
    }

    private boolean isTJunctionConflictLaneBusy(Vehicle vehicle, TurnYieldSpec spec) {
        for (Map.Entry<Vehicle, RouteState> otherEntry : states.entrySet()) {
            Vehicle other = otherEntry.getKey();
            if (other == vehicle) continue;
            RouteState otherState = otherEntry.getValue();
            Vector2D otherPoint = otherState.route.pointAt(otherState.progress);

            if (spec.blockingRouteId.equals(otherState.route.id)
                    && Math.abs(otherPoint.getY() - spec.laneY) <= T_JUNCTION_TURN_LANE_TOLERANCE
                    && otherPoint.getX() >= spec.minX && otherPoint.getX() <= spec.maxX) {
                return true;
            }

            if (Math.abs(otherPoint.getY() - spec.laneY) <= 18.0
                    && Math.abs(otherPoint.getX() - spec.decisionX) <= 34.0) {
                return true;
            }
        }
        return false;
    }

    private double speedAllowedByLeader(Vehicle vehicle, RouteState state) {
        double closest = Double.MAX_VALUE;
        Vehicle leader = null;
        for (Map.Entry<Vehicle, RouteState> otherEntry : states.entrySet()) {
            if (otherEntry.getKey() == vehicle || otherEntry.getValue().route != state.route) continue;
            double gap = otherEntry.getValue().progress - state.progress;
            if (gap <= 0) gap += state.route.totalLength;
            if (gap < closest) {
                closest = gap;
                leader = otherEntry.getKey();
            }
        }
        return allowedFollowingSpeed(vehicle, leader, closest);
    }

    /**
     * Tìm xe trước mặt bằng tọa độ thực, không chỉ bằng đối tượng RouteDefinition.
     * Điều này xử lý các tuyến có đoạn đầu/đoạn cuối trùng nhau và các điểm nhập làn.
     */
    private double speedAllowedByNearbyRoute(Vehicle vehicle, RouteState state) {
        Vector2D position = state.route.pointAt(state.progress);
        double heading = state.route.headingAt(state.progress);
        double radians = Math.toRadians(heading);
        double ux = Math.cos(radians);
        double uy = Math.sin(radians);

        double closest = Double.MAX_VALUE;
        Vehicle leader = null;
        for (Map.Entry<Vehicle, RouteState> otherEntry : states.entrySet()) {
            Vehicle other = otherEntry.getKey();
            if (other == vehicle) continue;
            Vector2D otherPosition = otherEntry.getValue().route.pointAt(otherEntry.getValue().progress);
            double dx = otherPosition.getX() - position.getX();
            double dy = otherPosition.getY() - position.getY();
            double longitudinal = dx * ux + dy * uy;
            double lateral = Math.abs(dx * uy - dy * ux);
            double headingDifference = Math.abs(shortestAngleDelta(
                    heading, otherEntry.getValue().route.headingAt(otherEntry.getValue().progress)));

            if (longitudinal > 0.0
                    && lateral <= sameLaneCorridor(vehicle, other)
                    && headingDifference <= 38.0
                    && longitudinal < closest) {
                closest = longitudinal;
                leader = other;
            }
        }
        return allowedFollowingSpeed(vehicle, leader, closest);
    }

    private double allowedFollowingSpeed(Vehicle vehicle, Vehicle leader, double centerGap) {
        if (leader == null || centerGap == Double.MAX_VALUE) return vehicle.getMaxSpeed();
        double hardGap = minimumCenterGap(vehicle, leader);
        double desiredGap = desiredCenterGap(vehicle, leader);
        double slowGap = desiredGap + FOLLOWING_SLOW_BUFFER;

        if (centerGap <= hardGap + 1.0) return 0.0;
        if (centerGap < desiredGap) {
            double ratio = clamp((centerGap - hardGap) / Math.max(1.0, desiredGap - hardGap), 0.0, 1.0);
            return Math.min(leader.getSpeed(), vehicle.getMaxSpeed() * ratio * ratio);
        }
        if (centerGap < slowGap) {
            double ratio = clamp((centerGap - desiredGap) / Math.max(1.0, slowGap - desiredGap), 0.0, 1.0);
            double blended = leader.getSpeed() + (vehicle.getMaxSpeed() - leader.getSpeed()) * ratio;
            return Math.min(vehicle.getMaxSpeed(), Math.max(leader.getSpeed(), blended));
        }
        return vehicle.getMaxSpeed();
    }

    private double findSafeInitialProgress(Vehicle vehicle, RouteDefinition route, double nominalProgress) {
        double spacing = Math.max(72.0, desiredCenterGapForSpawn(vehicle));
        for (int attempt = 0; attempt < 28; attempt++) {
            double candidate = wrapProgress(route, nominalProgress - attempt * spacing);
            if (isSpawnPositionSafe(vehicle, route, candidate)) {
                return candidate;
            }
        }
        return nominalProgress;
    }

    private boolean isSpawnPositionSafe(Vehicle vehicle, RouteDefinition route, double progress) {
        Vector2D point = route.pointAt(progress);
        double heading = route.headingAt(progress);
        double radians = Math.toRadians(heading);
        double ux = Math.cos(radians);
        double uy = Math.sin(radians);
        for (Map.Entry<Vehicle, RouteState> entry : states.entrySet()) {
            Vehicle other = entry.getKey();
            Vector2D otherPoint = entry.getValue().route.pointAt(entry.getValue().progress);
            double dx = otherPoint.getX() - point.getX();
            double dy = otherPoint.getY() - point.getY();
            double longitudinal = dx * ux + dy * uy;
            double lateral = Math.abs(dx * uy - dy * ux);
            if (Math.hypot(dx, dy) < physicalClearance(vehicle, other)) return false;
            if (lateral <= sameLaneCorridor(vehicle, other)
                    && Math.abs(longitudinal) < desiredCenterGap(vehicle, other)) return false;
        }
        return true;
    }

    private double maximumSafeAdvanceOnSameRoute(Vehicle vehicle,
                                                  RouteState state,
                                                  Map<Vehicle, Double> progressSnapshot) {
        double currentProgress = progressSnapshot.getOrDefault(vehicle, state.progress);
        double closest = Double.MAX_VALUE;
        Vehicle leader = null;
        for (Map.Entry<Vehicle, RouteState> otherEntry : states.entrySet()) {
            Vehicle other = otherEntry.getKey();
            if (other == vehicle || otherEntry.getValue().route != state.route) continue;
            double otherProgress = progressSnapshot.getOrDefault(other, otherEntry.getValue().progress);
            double gap = otherProgress - currentProgress;
            if (gap <= 0.0) gap += state.route.totalLength;
            if (gap < closest) {
                closest = gap;
                leader = other;
            }
        }
        if (leader == null) return vehicle.getSpeed();
        return Math.max(0.0, Math.min(vehicle.getSpeed(), closest - minimumCenterGap(vehicle, leader)));
    }

    private boolean isProjectedPositionSafe(Vehicle vehicle, RouteState state, double candidateProgress) {
        Vector2D candidate = state.route.pointAt(candidateProgress);
        double heading = state.route.headingAt(candidateProgress);
        double radians = Math.toRadians(heading);
        double ux = Math.cos(radians);
        double uy = Math.sin(radians);

        for (Map.Entry<Vehicle, RouteState> otherEntry : states.entrySet()) {
            Vehicle other = otherEntry.getKey();
            if (other == vehicle) continue;
            Vector2D otherPoint = other.getPosition();
            double dx = otherPoint.getX() - candidate.getX();
            double dy = otherPoint.getY() - candidate.getY();
            double distance = Math.hypot(dx, dy);
            if (distance < physicalClearance(vehicle, other)) return false;

            double longitudinal = dx * ux + dy * uy;
            double lateral = Math.abs(dx * uy - dy * ux);
            double otherHeading = other.getVisualHeadingDegrees();
            double headingDifference = Math.abs(shortestAngleDelta(heading, otherHeading));
            if (longitudinal >= -0.5
                    && lateral <= sameLaneCorridor(vehicle, other)
                    && headingDifference <= 42.0
                    && longitudinal < minimumCenterGap(vehicle, other)) {
                return false;
            }
        }
        return true;
    }

    private double minimumCenterGap(Vehicle follower, Vehicle leader) {
        double bumperGap = isLargeVehicle(follower) || isLargeVehicle(leader)
                ? LARGE_VEHICLE_BUMPER_GAP : MIN_BUMPER_GAP;
        return vehicleLength(follower) * 0.5 + vehicleLength(leader) * 0.5 + bumperGap;
    }

    private double desiredCenterGap(Vehicle follower, Vehicle leader) {
        return minimumCenterGap(follower, leader)
                + Math.max(0.0, follower.getSpeed()) * REACTION_DISTANCE_FACTOR;
    }

    private double desiredCenterGapForSpawn(Vehicle vehicle) {
        double bumperGap = isLargeVehicle(vehicle) ? LARGE_VEHICLE_BUMPER_GAP : MIN_BUMPER_GAP;
        return vehicleLength(vehicle) + bumperGap + 16.0;
    }

    private double sameLaneCorridor(Vehicle first, Vehicle second) {
        return Math.max(SAME_LANE_CORRIDOR,
                (vehicleWidth(first) + vehicleWidth(second)) * 0.5 + 3.0);
    }

    private double physicalClearance(Vehicle first, Vehicle second) {
        return (vehicleWidth(first) + vehicleWidth(second)) * 0.5 + 7.0;
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

    private boolean isLargeVehicle(Vehicle vehicle) {
        return vehicle instanceof Bus || vehicle instanceof FireTruck;
    }

    private double wrapProgress(RouteDefinition route, double progress) {
        if (route.totalLength <= 0.0) return 0.0;
        double wrapped = progress % route.totalLength;
        return wrapped < 0.0 ? wrapped + route.totalLength : wrapped;
    }

    private Gate nextGate(RouteState state) {
        if (state.route.gates.isEmpty()) return null;
        Gate best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Gate gate : state.route.gates) {
            double distance = distanceAhead(state, gate.progress);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = gate;
            }
        }
        return best;
    }

    private double distanceAhead(RouteState state, double targetProgress) {
        double distance = targetProgress - state.progress;
        if (distance < 0) distance += state.route.totalLength;
        return distance;
    }

    private void adjustSpeed(Vehicle vehicle, double desiredSpeed) {
        double current = vehicle.getSpeed();
        int boostTicks = turnReleaseBoostTicks.getOrDefault(vehicle, 0);
        double accelerationStep = boostTicks > 0 ? T_JUNCTION_TURN_ACCELERATION : 0.34;

        if (desiredSpeed <= 0.05) {
            vehicle.setSpeed(Math.max(0.0, current - 0.62));
            turnReleaseBoostTicks.remove(vehicle);
        } else if (current < desiredSpeed) {
            vehicle.setSpeed(Math.min(desiredSpeed, current + accelerationStep));
            if (boostTicks > 0) {
                if (boostTicks <= 1) turnReleaseBoostTicks.remove(vehicle);
                else turnReleaseBoostTicks.put(vehicle, boostTicks - 1);
            }
        } else {
            vehicle.setSpeed(Math.max(desiredSpeed, current - 0.48));
            if (boostTicks > 0) {
                if (boostTicks <= 1) turnReleaseBoostTicks.remove(vehicle);
                else turnReleaseBoostTicks.put(vehicle, boostTicks - 1);
            }
        }
        vehicle.setState(vehicle.getSpeed() > 0.05 ? VehicleState.MOVING : VehicleState.STOPPED);
    }

    private void updateVehiclePose(Vehicle vehicle, RouteState state, boolean immediateHeading) {
        Vector2D point = state.route.pointAt(state.progress);
        vehicle.setPosition(new Vector2D(point.getX(), point.getY()));
        double targetHeading = state.route.headingAt(state.progress);
        double currentHeading = vehicle.getVisualHeadingDegrees();
        double delta = shortestAngleDelta(currentHeading, targetHeading);
        double nextHeading = immediateHeading ? targetHeading : normalizeDegrees(currentHeading + clamp(delta, -12.0, 12.0));
        vehicle.setVisualHeadingDegrees(nextHeading);

        Direction cardinal = cardinalDirection(targetHeading);
        if (vehicle.getDirection() != cardinal) {
            vehicle.setDirection(cardinal);
            vehicle.setVisualHeadingDegrees(nextHeading);
        }

        double futureHeading = state.route.headingAt(Math.min(state.route.totalLength, state.progress + 58.0));
        double turnDelta = shortestAngleDelta(targetHeading, futureHeading);
        vehicle.clearIndicators();
        if (Math.abs(turnDelta) > 10.0) {
            if (turnDelta > 0) vehicle.setRightIndicatorOn(true);
            else vehicle.setLeftIndicatorOn(true);
        }
    }

    private static Direction cardinalDirection(double heading) {
        double h = normalizeDegrees(heading);
        if (h >= 45 && h < 135) return Direction.SOUTH;
        if (h >= 135 && h < 225) return Direction.WEST;
        if (h >= 225 && h < 315) return Direction.NORTH;
        return Direction.EAST;
    }

    private static boolean isEmergency(Vehicle vehicle) {
        return vehicle instanceof Ambulance || vehicle instanceof FireTruck;
    }

    private static double shortestAngleDelta(double from, double to) {
        double delta = normalizeDegrees(to) - normalizeDegrees(from);
        while (delta > 180) delta -= 360;
        while (delta < -180) delta += 360;
        return delta;
    }

    private static double normalizeDegrees(double angle) {
        double normalized = angle % 360.0;
        return normalized < 0 ? normalized + 360.0 : normalized;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private List<RouteDefinition> buildTJunctionRoutes() {
        List<RouteDefinition> result = new ArrayList<>();

        // Nhánh Nam của ngã ba rộng bằng đường Đông - Tây và có 3 làn mỗi chiều.
        // Theo yêu cầu, phần xe đi từ Nam lên nút giao phải nằm bên phải dải phân cách
        // (đúng làn phải theo hướng lưu thông). Vì vậy nhánh đi xuống được chuyển sang bên trái.
        final double northboundInnerLaneX = 474;
        final double northboundOuterLaneX = 537;
        final double southboundInnerLaneX = 426;
        final double southboundOuterLaneX = 363;

        result.add(new RouteBuilder("Tây → Đông", -520, 368)
                .gate(286, 368, 0).lineTo(1280, 368).build());
        result.add(new RouteBuilder("Đông → Tây", 1420, 332)
                .gate(614, 332, 1).lineTo(-520, 332).build());

        // Rẽ trái từ Nam dùng làn sát dải phân cách; rẽ phải dùng làn
        // chuyên dụng bên phải và được phép rẽ khi đèn đỏ sau khi đã nhường xe ngang.
        result.add(new RouteBuilder("Nam → Tây", northboundInnerLaneX, 1180)
                .gate(northboundInnerLaneX, 570, 2).lineTo(northboundInnerLaneX, 452)
                .cubicTo(northboundInnerLaneX, 392, 400, 344, 338, 332)
                .lineTo(-520, 332).build());
        result.add(new RouteBuilder("Nam → Đông", northboundOuterLaneX, 1180)
                .gate(northboundOuterLaneX, 570, 2).lineTo(northboundOuterLaneX, 452)
                .cubicTo(northboundOuterLaneX, 400, 480, 368, 562, 368)
                .lineTo(1420, 368).build());

        // Xe đi xuống được nhập vào phần đường bên trái dải phân cách sau khi hoán đổi làn.
        result.add(new RouteBuilder("Tây → Nam", -520, 368)
                .gate(286, 368, 0).lineTo(338, 368)
                .straightFallback(338, 368, "Tây → Đông")
                .cubicTo(410, 368, southboundOuterLaneX, 405, southboundOuterLaneX, 472)
                .lineTo(southboundOuterLaneX, 1180).build());
        result.add(new RouteBuilder("Đông → Nam", 1420, 332)
                .gate(614, 332, 1).lineTo(562, 332)
                .straightFallback(562, 332, "Đông → Tây")
                .cubicTo(510, 332, southboundInnerLaneX, 405, southboundInnerLaneX, 472)
                .lineTo(southboundInnerLaneX, 1180).build());
        return Collections.unmodifiableList(result);
    }

    private List<RouteDefinition> buildNetworkRoutes() {
        List<RouteDefinition> result = new ArrayList<>();

        result.add(networkHorizontal("Đường trên: Tây → Đông", -560, 138, 1460, 0));
        result.add(networkHorizontalReverse("Đường trên: Đông → Tây", 1460, 114, -560, 0));
        result.add(networkHorizontal("Đường giữa: Tây → Đông", -560, 308, 1460, 0));
        result.add(networkHorizontalReverse("Đường giữa: Đông → Tây", 1460, 284, -560, 0));
        result.add(networkHorizontal("Đường dưới: Tây → Đông", -560, 478, 1460, 4));
        result.add(networkHorizontalReverse("Đường dưới: Đông → Tây", 1460, 454, -560, 4));
        result.add(networkVertical("Trục trái: Bắc → Nam", 168, -560, 1260, 1));
        result.add(networkVerticalReverse("Trục trái: Nam → Bắc", 144, 1260, -560, 1));
        result.add(networkVertical("Trục giữa: Bắc → Nam", 378, -560, 1260, 1));
        result.add(networkVertical("Trục phải: Bắc → Nam", 588, -560, 1260, 3));
        result.add(networkVerticalReverse("Trục phải: Nam → Bắc", 564, 1260, -560, 3));

        result.add(new RouteBuilder("Liên tuyến chữ L", -560, 308)
                .gate(330, 308, 0).lineTo(324, 308)
                .straightFallback(324, 308, "Đường giữa: Tây → Đông")
                .cubicTo(350, 308, 378, 330, 378, 360)
                .gate(378, 430, 5).lineTo(378, 430)
                .straightFallback(378, 430, "Trục giữa: Bắc → Nam")
                .cubicTo(378, 455, 400, 478, 430, 478)
                .gate(540, 478, 6).lineTo(1460, 478).build());

        result.add(new RouteBuilder("Liên tuyến ziczac", 1460, 454)
                .gate(750, 454, 6).lineTo(600, 454)
                .straightFallback(600, 454, "Đường dưới: Đông → Tây")
                .cubicTo(576, 454, 564, 430, 564, 405)
                .gate(564, 332, 3).lineTo(564, 320)
                .straightFallback(564, 320, "Trục phải: Nam → Bắc")
                .cubicTo(564, 296, 540, 284, 515, 284)
                .gate(330, 284, 2).lineTo(-560, 284).build());

        return Collections.unmodifiableList(result);
    }

    private RouteDefinition networkHorizontal(String id, double startX, double y, double endX, int firstLight) {
        RouteBuilder builder = new RouteBuilder(id, startX, y)
                .gate(116, y, firstLight)
                .gate(326, y, (firstLight + 2) % 8)
                .gate(536, y, (firstLight + 4) % 8)
                .gate(746, y, (firstLight + 6) % 8)
                .lineTo(endX, y);
        return builder.build();
    }

    private RouteDefinition networkHorizontalReverse(String id, double startX, double y, double endX, int firstLight) {
        RouteBuilder builder = new RouteBuilder(id, startX, y)
                .gate(826, y, (firstLight + 6) % 8)
                .gate(616, y, (firstLight + 4) % 8)
                .gate(406, y, (firstLight + 2) % 8)
                .gate(196, y, firstLight)
                .lineTo(endX, y);
        return builder.build();
    }

    private RouteDefinition networkVertical(String id, double x, double startY, double endY, int firstLight) {
        RouteBuilder builder = new RouteBuilder(id, x, startY)
                .gate(x, 86, firstLight)
                .gate(x, 256, (firstLight + 2) % 8)
                .gate(x, 426, (firstLight + 4) % 8)
                .gate(x, 596, (firstLight + 6) % 8)
                .lineTo(x, endY);
        return builder.build();
    }

    private RouteDefinition networkVerticalReverse(String id, double x, double startY, double endY, int firstLight) {
        RouteBuilder builder = new RouteBuilder(id, x, startY)
                .gate(x, 676, (firstLight + 6) % 8)
                .gate(x, 506, (firstLight + 4) % 8)
                .gate(x, 336, (firstLight + 2) % 8)
                .gate(x, 166, firstLight)
                .lineTo(x, endY);
        return builder.build();
    }
}
