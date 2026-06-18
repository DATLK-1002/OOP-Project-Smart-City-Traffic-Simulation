package view.renderer;

import javafx.scene.canvas.GraphicsContext;
import view.renderstate.VehicleRenderState;

public interface VehicleGraphicsRenderer {
    void render(GraphicsContext graphics, VehicleRenderState state);
}
