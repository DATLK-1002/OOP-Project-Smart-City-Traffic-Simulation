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
        double w = (isBus() ? 40 : 25) * scale;
        double h = (isBus() ? 15 : 13) * scale;
        Vector2D position = vehicle.getPosition();
        graphics.save();
        graphics.translate(position.getX(), position.getY());
        graphics.rotate(vehicle.getVisualHeadingDegrees());

        if (displayMode == DisplayMode.BASIC) {
            graphics.setFill(getVehicleColor());
            graphics.fillRoundRect(-w / 2, -h / 2, w, h, 6 * scale, 6 * scale);
            graphics.setFill(Color.WHITE);
            graphics.fillRect(w / 2 - 4 * scale, -h / 2 + 2 * scale, 3 * scale, h - 4 * scale);
        } else {
            graphics.setFill(getVehicleColor());
            graphics.fillRoundRect(-w / 2, -h / 2, w, h, 10 * scale, 10 * scale);
            graphics.setFill(Color.web("#111111"));
            graphics.fillOval(-w / 3, -h / 2 - 3 * scale, 6 * scale, 6 * scale);
            graphics.fillOval(w / 4, -h / 2 - 3 * scale, 6 * scale, 6 * scale);
            graphics.fillOval(-w / 3, h / 2 - 3 * scale, 6 * scale, 6 * scale);
            graphics.fillOval(w / 4, h / 2 - 3 * scale, 6 * scale, 6 * scale);
            graphics.setFill(Color.WHITE);
            graphics.fillRect(w / 2 - 4 * scale, -h / 2 + 2 * scale, 3 * scale, h - 4 * scale);
            if (isEmergencyVehicle() && tick % 6 < 3) {
                graphics.setFill(Color.RED);
                graphics.fillOval(-4 * scale, -4 * scale, 8 * scale, 8 * scale);
            }
        }

        drawTurnIndicators(graphics, w, h);
        graphics.restore();
        // Không vẽ label lên xe để map gọn và tránh cảm giác xe bị ngược đầu.
        // Mũi trắng ở đầu xe cho biết hướng di chuyển.
    }

    private void drawTurnIndicators(GraphicsContext graphics, double w, double h) {
        if ((tick / 4) % 2 != 0) {
            return;
        }
        if (!vehicle.isLeftIndicatorOn() && !vehicle.isRightIndicatorOn()) {
            return;
        }

        double radius = Math.max(2.0, 2.4 * scale);
        double frontX = w * 0.36;
        double rearX = -w * 0.36;
        double sideY = h * 0.42;
        graphics.setFill(Color.web("#ffb000"));

        if (vehicle.isLeftIndicatorOn()) {
            graphics.fillOval(frontX - radius, -sideY - radius, radius * 2, radius * 2);
            graphics.fillOval(rearX - radius, -sideY - radius, radius * 2, radius * 2);
        }
        if (vehicle.isRightIndicatorOn()) {
            graphics.fillOval(frontX - radius, sideY - radius, radius * 2, radius * 2);
            graphics.fillOval(rearX - radius, sideY - radius, radius * 2, radius * 2);
        }
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

       
    private boolean isBus() {
        return vehicle.getId().startsWith("BUS");
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
        if (id.startsWith("BUS")) return Color.LIMEGREEN;
        if (id.startsWith("AMB")) return Color.DEEPSKYBLUE;
        if (id.startsWith("FIRE")) return Color.CRIMSON;
        if (id.startsWith("MOTO")) return Color.DODGERBLUE;
        if (id.startsWith("BIKE")) return Color.LIGHTPINK;
        return Color.ORANGE;
    }
}