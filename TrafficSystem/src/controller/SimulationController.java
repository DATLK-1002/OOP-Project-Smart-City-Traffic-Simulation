package controller;

public class SimulationController {

    private VehicleController vehicleController;

    private TrafficController trafficController;

    private InputController inputController;

    private boolean running;

    public SimulationController() {

        vehicleController = new VehicleController();

        trafficController = new TrafficController();

        inputController = new InputController();

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

        if(!running) {
            return;
        }

        vehicleController.updateVehicles();

        trafficController.updateLights();
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
}