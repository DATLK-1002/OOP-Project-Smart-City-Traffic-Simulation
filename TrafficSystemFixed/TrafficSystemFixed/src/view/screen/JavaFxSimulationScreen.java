package view.screen;

import controller.SimulationController;
import controller.LightControlMode;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import model.map.TrafficMapType;
import model.trafficlight.TrafficLight;
import model.vehicle.Vehicle;
import view.component.JavaFxRoadView;
import view.component.JavaFxTrafficLightView;
import view.component.JavaFxVehicleView;
import view.renderer.DisplayMode;

public class JavaFxSimulationScreen extends BorderPane {

    public static final double WIDTH = 900;
    public static final double HEIGHT = 700;

    private final SimulationController controller;

    private final Canvas canvas;

    private final Label statusLabel;

    private final JavaFxRoadView roadView;
    private static final double[][] LIGHT_POSITIONS = {
            {290, 465},
            {290, 125},
            {565, 125},
            {565, 465}
    };
    private TrafficMapType mapType;
    private DisplayMode displayMode;

    public JavaFxSimulationScreen(SimulationController controller, Label statusLabel) {
        this.controller = controller;
        this.statusLabel = statusLabel;
        this.canvas = new Canvas(WIDTH, HEIGHT);
        this.roadView = new JavaFxRoadView(WIDTH, HEIGHT);

        this.mapType = TrafficMapType.CROSS_JUNCTION;
        this.displayMode = DisplayMode.BASIC;
        setCenter(canvas);
        configureManualLightClicks();
    }

    public void setMapType(TrafficMapType mapType) {
        this.mapType = mapType == null ? TrafficMapType.CROSS_JUNCTION : mapType;
        roadView.setMapType(this.mapType);
    }

    public void setDisplayMode(DisplayMode displayMode) {
        this.displayMode = displayMode == null ? DisplayMode.BASIC : displayMode;
    }

    public void render(int tick) {
        GraphicsContext graphics = canvas.getGraphicsContext2D();
        graphics.setFill(Color.web("#20242a"));
        graphics.fillRect(0, 0, WIDTH, HEIGHT);

        roadView.render(graphics);
        renderTrafficLights(graphics);
        renderVehicles(graphics, tick);
        updateStatus(tick);
    }

    private void renderTrafficLights(GraphicsContext graphics) {

        int index = 0;

        for (TrafficLight light : controller.getTrafficController().getTrafficLights()) {
           double[] position = LIGHT_POSITIONS[index % LIGHT_POSITIONS.length];
            new JavaFxTrafficLightView(light).render(graphics, position[0], position[1]);
            index++;
        }
    }

    private void renderVehicles(GraphicsContext graphics, int tick) {
        for (Vehicle vehicle : controller.getVehicleController().getVehicles()) {
            new JavaFxVehicleView(vehicle, displayMode, mapType.getVehicleScale(), tick).render(graphics);
        }
    }

    private void configureManualLightClicks() {
        canvas.setOnMouseClicked(event -> {
            int index = 0;
            for (TrafficLight light : controller.getTrafficController().getTrafficLights()) {
                double[] position = LIGHT_POSITIONS[index % LIGHT_POSITIONS.length];
                double x = position[0];
                double y = position[1];
                if (event.getX() >= x && event.getX() <= x + 42
                         && event.getY() >= y && event.getY() <= y + 112
                        && controller.getTrafficController().getControlMode() == LightControlMode.MANUAL) {
                    controller.getTrafficController().switchLight(light);
                    render(0);
                    return;
                }
                index++;
            }
        });
    }

    private void updateStatus(int tick) {
        statusLabel.setText(String.format(
                "Tick: %d | Vehicles: %d | Lights: %d | %s | %s | %s",
                tick,
                controller.getVehicleController().getVehicles().size(),
                controller.getTrafficController().getTrafficLights().size(),
                controller.isRunning() ? "Running" : "Paused",
                controller.getTrafficController().getControlMode(),
                mapType));
    }
}