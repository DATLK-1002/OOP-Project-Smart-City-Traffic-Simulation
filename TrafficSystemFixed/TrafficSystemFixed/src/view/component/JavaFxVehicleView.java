package view.component;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import model.vehicle.Vehicle;
import util.Direction;
import util.Vector2D;
import view.renderer.DisplayMode;

public class JavaFxVehicleView {

    private final Vehicle vehicle;
    private final DisplayMode displayMode;
    private final double scale;
    private final int tick;

    public JavaFxVehicleView(Vehicle vehicle) {
        this(vehicle, DisplayMode.BASIC, 1.0, 0);
    }

    public JavaFxVehicleView(Vehicle vehicle, DisplayMode displayMode, double scale, int tick) {
        this.vehicle = vehicle;
        this.displayMode = displayMode == null ? DisplayMode.BASIC : displayMode;
        this.scale = scale;
        this.tick = tick;
    }

    public void render(GraphicsContext graphics) {
        double w = 34 * scale;
        double h = 18 * scale;
        Vector2D position = vehicle.getPosition();
        graphics.save();
        graphics.translate(position.getX(), position.getY());
        graphics.rotate(rotationFor(vehicle.getDirection()));

        if (displayMode == DisplayMode.BASIC) {
            graphics.setFill(getVehicleColor());
            graphics.fillRect(-w / 2, -h / 2, w, h);
        } else {
            graphics.setFill(getVehicleColor());
            graphics.fillRoundRect(-w / 2, -h / 2, w, h, 10 * scale, 10 * scale);
            graphics.setFill(Color.web("#111111"));
            graphics.fillOval(-w / 3, -h / 2 - 3 * scale, 6 * scale, 6 * scale);
            graphics.fillOval(w / 4, -h / 2 - 3 * scale, 6 * scale, 6 * scale);
            graphics.fillOval(-w / 3, h / 2 - 3 * scale, 6 * scale, 6 * scale);
            graphics.fillOval(w / 4, h / 2 - 3 * scale, 6 * scale, 6 * scale);
            if (isEmergencyVehicle() && tick % 6 < 3) {
                graphics.setFill(Color.RED);
                graphics.fillOval(-4 * scale, -4 * scale, 8 * scale, 8 * scale);
            }
        }

  
        graphics.restore();
        graphics.setFill(Color.WHITE);
        graphics.fillText(shortLabel(), position.getX() - 18 * scale, position.getY() - 16 * scale);
    }

    private double rotationFor(Direction direction) {
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

       
    private boolean isEmergencyVehicle() {
        String id = vehicle.getId();
        return id.startsWith("AMB") || id.startsWith("FIRE");
    }

    private String shortLabel() {
        String id = vehicle.getId();
        int dash = id.indexOf('-');
        return dash > 0 ? id.substring(0, dash) : id;
    }

    private Color getVehicleColor() {
        String id = vehicle.getId();
        if (id.startsWith("AMB")) return Color.DEEPSKYBLUE;
        if (id.startsWith("FIRE")) return Color.CRIMSON;
        if (id.startsWith("MOTO")) return Color.ORANGE;
        if (id.startsWith("BIKE")) return Color.LIGHTPINK;
        return Color.LIMEGREEN;
    }
}