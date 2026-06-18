package view.screen;

import controller.LightControlMode;
import controller.SimulationController;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
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

    private static final class LightPlacement {
        private final double x;
        private final double y;
        private final boolean compact;

        private LightPlacement(double x, double y, boolean compact) {
            this.x = x;
            this.y = y;
            this.compact = compact;
        }
    }

    private static final LightPlacement[] CROSS_LIGHTS = {
            new LightPlacement(290, 465, false),
            new LightPlacement(290, 125, false),
            new LightPlacement(565, 125, false),
            new LightPlacement(565, 465, false)
    };

    private static final LightPlacement[] T_LIGHTS = {
            new LightPlacement(290, 465, false),
            new LightPlacement(565, 125, false),
            new LightPlacement(565, 465, false)
    };

    private static final LightPlacement[] NETWORK_LIGHTS = {
            new LightPlacement(337, 270, true),
            new LightPlacement(380, 300, true),
            new LightPlacement(547, 270, true),
            new LightPlacement(590, 300, true),
            new LightPlacement(337, 440, true),
            new LightPlacement(380, 470, true),
            new LightPlacement(547, 440, true),
            new LightPlacement(590, 470, true)
    };

    private final SimulationController controller;
    private final Canvas canvas;
    private final Label statusLabel;
    private final JavaFxRoadView roadView;

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
        renderMapBadge(graphics);
        updateStatus(tick);
    }

    private void renderTrafficLights(GraphicsContext graphics) {
        LightPlacement[] placements = placementsForMap();
        int index = 0;
        for (TrafficLight light : controller.getTrafficController().getTrafficLights()) {
            if (index >= placements.length) break;
            LightPlacement placement = placements[index];
            JavaFxTrafficLightView view = new JavaFxTrafficLightView(light);
            if (placement.compact) {
                view.renderCompact(graphics, placement.x, placement.y);
            } else {
                view.render(graphics, placement.x, placement.y);
            }
            index++;
        }
    }

    private void renderVehicles(GraphicsContext graphics, int tick) {
        for (Vehicle vehicle : controller.getVehicleController().getVehicles()) {
            new JavaFxVehicleView(vehicle, displayMode, mapType.getVehicleScale(), tick).render(graphics);
        }
    }

    private void renderMapBadge(GraphicsContext graphics) {
        graphics.setFill(Color.web("#00000099"));
        graphics.fillRoundRect(14, 12, mapType.isWideArea() ? 320 : 220, 34, 12, 12);
        graphics.setFill(Color.WHITE);
        graphics.setFont(Font.font(15));
        String scaleText = mapType.isWideArea() ? "toàn cảnh - xe thu nhỏ" : "cận cảnh - xe phóng lớn";
        graphics.fillText(mapType.getLabel() + " | " + scaleText, 27, 34);
    }

    private void configureManualLightClicks() {
        canvas.setOnMouseClicked(event -> {
            if (controller.getTrafficController().getControlMode() != LightControlMode.MANUAL) {
                return;
            }
            LightPlacement[] placements = placementsForMap();
            int index = 0;
            for (TrafficLight light : controller.getTrafficController().getTrafficLights()) {
                if (index >= placements.length) break;
                LightPlacement placement = placements[index];
                double width = placement.compact
                        ? JavaFxTrafficLightView.COMPACT_WIDTH
                        : JavaFxTrafficLightView.WIDTH;
                double height = placement.compact
                        ? JavaFxTrafficLightView.COMPACT_HEIGHT
                        : JavaFxTrafficLightView.HEIGHT;
                if (event.getX() >= placement.x && event.getX() <= placement.x + width
                        && event.getY() >= placement.y && event.getY() <= placement.y + height) {
                    controller.getTrafficController().switchLight(light);
                    render(0);
                    return;
                }
                index++;
            }
        });
    }

    private LightPlacement[] placementsForMap() {
        switch (mapType) {
            case T_JUNCTION:
                return T_LIGHTS;
            case ROAD_NETWORK:
                return NETWORK_LIGHTS;
            case CROSS_JUNCTION:
            default:
                return CROSS_LIGHTS;
        }
    }

    private void updateStatus(int tick) {
        statusLabel.setText(String.format(
                "Tick: %d | Xe: %d | Đèn: %d | %s | %s | %s | Tỷ lệ xe: %.0f%%",
                tick,
                controller.getVehicleController().getVehicles().size(),
                controller.getTrafficController().getTrafficLights().size(),
                controller.isRunning() ? "Đang chạy" : "Tạm dừng",
                controller.getTrafficController().getControlMode(),
                mapType,
                mapType.getVehicleScale() * 100));
    }
}
