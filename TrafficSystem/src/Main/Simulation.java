package Main;

import controller.SimulationController;

public class Simulation {

    private SimulationController controller;

    public Simulation() {

        controller =
                new SimulationController();
    }

    public void start() {

        controller.start();

        while(controller.isRunning()) {

            controller.update();

            try {

                Thread.sleep(100);

            } catch(Exception e) {

                e.printStackTrace();
            }
        }
    }
}