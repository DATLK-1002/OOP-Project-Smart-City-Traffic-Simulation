package com.smartcity.core;

public class SimulationController {

    private SimulationModel model;

    private boolean running;

    public SimulationController(
            SimulationModel model) {

        this.model = model;
        this.running = false;
    }

    public void startSimulation() {

        running = true;

        System.out.println(
                "Simulation started");
    }

    public void stopSimulation() {

        running = false;

        System.out.println(
                "Simulation stopped");
    }

    public void update() {

        if (!running) {
            return;
        }

        // Update traffic lights

        // Update vehicles

        // Detect collisions

    }

    public boolean isRunning() {
        return running;
    }
}