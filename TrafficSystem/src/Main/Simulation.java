package Main;

import controller.SimulationController;
import model.trafficlight.BasicTrafficLight;
import model.vehicle.Vehicle;
import model.vehicle.VehicleFactory;
import strategy.EmergencyDriver;
import strategy.NormalDriver;
import util.Direction;
import util.Vector2D;
import view.component.TrafficLightView;
import view.component.VehicleView;
import view.screen.MainScreen;
import view.screen.SimulationScreen;

public class Simulation {

        private static final int DEFAULT_MAX_TICKS = 120;

    private SimulationController controller;

    public Simulation() {

        controller =
                new SimulationController();

        setupDemoScenario();
    }

    public void start() {

        start(DEFAULT_MAX_TICKS);
    }

    public void start(int maxTicks) {

        new MainScreen().show();
        new SimulationScreen().show();

        controller.start();

        int tick = 0;

        while(controller.isRunning() && tick < maxTicks) {

            controller.update();
            render(tick);
            tick++;

            try {

                Thread.sleep(100);

            } catch(InterruptedException e) {


                Thread.currentThread().interrupt();
                controller.stop();
            }
        }

        controller.stop();
    }

    private void setupDemoScenario() {

        BasicTrafficLight trafficLight = new BasicTrafficLight("TL-01");
        controller.getTrafficController().addTrafficLight(trafficLight);

        Vehicle car = VehicleFactory.createVehicle(
                "CAR", "CAR-01", new Vector2D(100, 500), Direction.EAST);
        car.setStrategy(new NormalDriver());
        controller.getVehicleController().addVehicle(car);

        Vehicle ambulance = VehicleFactory.createVehicle(
                "AMBULANCE", "AMB-01", new Vector2D(500, 900), Direction.NORTH);
        ambulance.setStrategy(new EmergencyDriver());
        controller.getVehicleController().addVehicle(ambulance);
    }

    private void render(int tick) {

        if (tick % 10 != 0) {
            return;
        }

        System.out.println("--- Tick " + tick + " ---");

        controller.getTrafficController().getTrafficLights()
                .forEach(light -> new TrafficLightView(light).render());
        controller.getVehicleController().getVehicles()
                .forEach(vehicle -> new VehicleView(vehicle).render());
    }

}