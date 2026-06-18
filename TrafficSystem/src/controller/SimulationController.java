package controller;

import model.map.TrafficMapType;
import model.vehicle.Vehicle;

public class SimulationController {

    private final VehicleController vehicleController;
    private final TrafficController trafficController;
    private final InputController inputController;
    private final MapRouteEngine mapRouteEngine;

    private boolean running;
    private TrafficMapType mapType;

    public SimulationController() {
        vehicleController = new VehicleController();
        trafficController = new TrafficController();
        vehicleController.setTrafficLights(trafficController.getTrafficLights());
        inputController = new InputController();
        mapRouteEngine = new MapRouteEngine(vehicleController, trafficController);
        mapType = TrafficMapType.CROSS_JUNCTION;
        running = false;
    }

    public void start() {
        running = true;
    }

    public void stop() {
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    public void update() {
        if (!running) {
            return;
        }

        if (mapType == TrafficMapType.CROSS_JUNCTION) {
            vehicleController.updateVehicles();
        } else {
            mapRouteEngine.updateVehicles();
        }
        trafficController.updateLights();
    }

    public void setMapType(TrafficMapType mapType) {
        this.mapType = mapType == null ? TrafficMapType.CROSS_JUNCTION : mapType;
        mapRouteEngine.configure(this.mapType);
        trafficController.configureForMap(this.mapType);
    }

    public TrafficMapType getMapType() {
        return mapType;
    }

    public void addVehicle(Vehicle vehicle, int routeIndex, int queueIndex) {
        vehicleController.addVehicle(vehicle);
        if (mapType != TrafficMapType.CROSS_JUNCTION) {
            mapRouteEngine.registerVehicle(vehicle, routeIndex, queueIndex);
        }
    }

    public VehicleController getVehicleController() {
        return vehicleController;
    }

    public TrafficController getTrafficController() {
        return trafficController;
    }

    public InputController getInputController() {
        return inputController;
    }

    public MapRouteEngine getMapRouteEngine() {
        return mapRouteEngine;
    }
}
