package view.renderstate;

public class VehicleRenderState {

    private final double x;
    private final double y;
    private final double headingDegrees;
    private final double scale;
    private final int tick;
    private final boolean leftIndicatorOn;
    private final boolean rightIndicatorOn;
    private final VehicleRenderType vehicleType;

    public VehicleRenderState(double x,
                              double y,
                              double headingDegrees,
                              double scale,
                              int tick,
                              boolean leftIndicatorOn,
                              boolean rightIndicatorOn,
                              VehicleRenderType vehicleType) {
        this.x = x;
        this.y = y;
        this.headingDegrees = headingDegrees;
        this.scale = scale;
        this.tick = tick;
        this.leftIndicatorOn = leftIndicatorOn;
        this.rightIndicatorOn = rightIndicatorOn;
        this.vehicleType = vehicleType;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getHeadingDegrees() {
        return headingDegrees;
    }

    public double getScale() {
        return scale;
    }

    public int getTick() {
        return tick;
    }

    public boolean isLeftIndicatorOn() {
        return leftIndicatorOn;
    }

    public boolean isRightIndicatorOn() {
        return rightIndicatorOn;
    }

    public VehicleRenderType getVehicleType() {
        return vehicleType;
    }

    public boolean isAmbulance() {
        return vehicleType == VehicleRenderType.AMBULANCE;
    }

    public boolean isEmergencyVehicle() {
        return vehicleType == VehicleRenderType.AMBULANCE || vehicleType == VehicleRenderType.FIRE_TRUCK;
    }

    public boolean isBusLike() {
        return vehicleType == VehicleRenderType.BUS || vehicleType == VehicleRenderType.FIRE_TRUCK;
    }

    public boolean shouldBlinkIndicator() {
        return (tick / 4) % 2 == 0;
    }

    public boolean shouldFlashEmergencyLight() {
        return isEmergencyVehicle() && (tick / 3) % 2 == 0;
    }

    public double getBodyLength() {
        switch (vehicleType) {
            case BUS:
            case FIRE_TRUCK:
                return 40.0 * scale;
            case MOTORBIKE:
                return 24.0 * scale;
            case BICYCLE:
                return 22.0 * scale;
            default:
                return 28.0 * scale;
        }
    }

    public double getBodyWidth() {
        switch (vehicleType) {
            case BUS:
            case FIRE_TRUCK:
                return 15.0 * scale;
            case MOTORBIKE:
                return 10.0 * scale;
            case BICYCLE:
                return 8.0 * scale;
            default:
                return 13.0 * scale;
        }
    }
}
