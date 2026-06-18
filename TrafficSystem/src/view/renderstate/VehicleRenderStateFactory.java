package view.renderstate;

import model.vehicle.Vehicle;
import util.Vector2D;

/**
 * Chịu trách nhiệm duy nhất cho việc chuyển trạng thái logic của xe
 * sang dữ liệu phục vụ vẽ. Không chứa bất kỳ lệnh vẽ JavaFX nào.
 */
public final class VehicleRenderStateFactory {

    public VehicleRenderState create(Vehicle vehicle, double scale, int tick) {
        Vector2D position = vehicle.getPosition();
        return new VehicleRenderState(
                position.getX(),
                position.getY(),
                vehicle.getVisualHeadingDegrees(),
                scale,
                tick,
                vehicle.isLeftIndicatorOn(),
                vehicle.isRightIndicatorOn(),
                detectType(vehicle.getId()));
    }

    private VehicleRenderType detectType(String id) {
        if (id == null) {
            return VehicleRenderType.CAR;
        }
        if (id.startsWith("BUS")) return VehicleRenderType.BUS;
        if (id.startsWith("AMB")) return VehicleRenderType.AMBULANCE;
        if (id.startsWith("FIRE")) return VehicleRenderType.FIRE_TRUCK;
        if (id.startsWith("MOTO")) return VehicleRenderType.MOTORBIKE;
        if (id.startsWith("BIKE")) return VehicleRenderType.BICYCLE;
        return VehicleRenderType.CAR;
    }
}
