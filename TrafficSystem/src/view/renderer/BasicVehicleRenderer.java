package view.renderer;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import view.renderstate.VehicleRenderState;
import view.renderstate.VehicleRenderType;

/**
 * Renderer chỉ vẽ từ VehicleRenderState, không biết gì về mô phỏng tọa độ.
 */
public class BasicVehicleRenderer implements VehicleGraphicsRenderer {

    @Override
    public void render(GraphicsContext graphics, VehicleRenderState state) {
        double w = state.getBodyLength();
        double h = state.getBodyWidth();

        graphics.save();
        graphics.translate(state.getX(), state.getY());
        graphics.rotate(state.getHeadingDegrees());

        graphics.setFill(colorFor(state.getVehicleType()));
        graphics.fillRoundRect(-w / 2, -h / 2, w, h, 6 * state.getScale(), 6 * state.getScale());

        graphics.setFill(Color.WHITE);
        graphics.fillRoundRect(-w * 0.05, -h * 0.35, w * 0.28, h * 0.70, 3, 3);
        graphics.fillRect(w / 2 - 4 * state.getScale(), -h / 2 + 2 * state.getScale(), 3 * state.getScale(), h - 4 * state.getScale());

        if (state.shouldFlashEmergencyLight()) {
            double lightW = 3.8 * state.getScale();
            double lightH = 5.0 * state.getScale();
            double lightX = 1.2 * state.getScale();

            graphics.setFill(Color.RED);
            graphics.fillOval(lightX - lightW / 2, -6.2 * state.getScale(), lightW, lightH);
            graphics.setFill(Color.DODGERBLUE);
            graphics.fillOval(lightX - lightW / 2, -0.8 * state.getScale(), lightW, lightH);
        }

        drawIndicators(graphics, state, w, h);
        graphics.restore();
    }

    private void drawIndicators(GraphicsContext graphics, VehicleRenderState state, double w, double h) {
        if (!state.shouldBlinkIndicator()) return;
        if (!state.isLeftIndicatorOn() && !state.isRightIndicatorOn()) return;

        double radius = Math.max(2.0, 2.3 * state.getScale());
        double frontX = w * 0.36;
        double rearX = -w * 0.36;
        double sideY = h * 0.42;
        graphics.setFill(Color.web("#ffb000"));

        if (state.isLeftIndicatorOn()) {
            graphics.fillOval(frontX - radius, -sideY - radius, radius * 2, radius * 2);
            graphics.fillOval(rearX - radius, -sideY - radius, radius * 2, radius * 2);
        }
        if (state.isRightIndicatorOn()) {
            graphics.fillOval(frontX - radius, sideY - radius, radius * 2, radius * 2);
            graphics.fillOval(rearX - radius, sideY - radius, radius * 2, radius * 2);
        }
    }

    private Color colorFor(VehicleRenderType type) {
        switch (type) {
            case BUS:
                return Color.LIMEGREEN;
            case AMBULANCE:
                return Color.WHITE;
            case FIRE_TRUCK:
                return Color.CRIMSON;
            case MOTORBIKE:
                return Color.DODGERBLUE;
            case BICYCLE:
                return Color.LIGHTPINK;
            case CAR:
            default:
                return Color.ORANGE;
        }
    }
}
