package view.component;

import javafx.scene.canvas.GraphicsContext;
import model.vehicle.Vehicle;
import view.renderer.BasicVehicleRenderer;
import view.renderer.DisplayMode;
import view.renderer.SpriteVehicleRenderer;
import view.renderer.VehicleGraphicsRenderer;
import view.renderstate.VehicleRenderState;
import view.renderstate.VehicleRenderStateFactory;

/**
 * Lớp này chỉ phối hợp giữa dữ liệu logic (Vehicle) và renderer phù hợp.
 * Tính toán tọa độ/hướng nằm trong VehicleRenderStateFactory,
 * còn việc vẽ hoàn toàn nằm trong các renderer.
 */
public class JavaFxVehicleView {

    private static final VehicleRenderStateFactory STATE_FACTORY = new VehicleRenderStateFactory();
    private static final VehicleGraphicsRenderer BASIC_RENDERER = new BasicVehicleRenderer();
    private static final VehicleGraphicsRenderer SPRITE_RENDERER = new SpriteVehicleRenderer();

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
        VehicleRenderState state = STATE_FACTORY.create(vehicle, scale, tick);
        rendererFor(displayMode).render(graphics, state);
    }

    private VehicleGraphicsRenderer rendererFor(DisplayMode mode) {
        return mode == DisplayMode.SPRITE ? SPRITE_RENDERER : BASIC_RENDERER;
    }
}
