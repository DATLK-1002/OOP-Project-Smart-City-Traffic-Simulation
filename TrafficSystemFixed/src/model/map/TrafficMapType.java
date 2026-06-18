package model.map;

public enum TrafficMapType {
    T_JUNCTION("Ngã ba", 0.78),
    CROSS_JUNCTION("Ngã tư", 0.78),
    FIVE_WAY_JUNCTION("Ngã năm", 0.74),
    ROAD_NETWORK("Mạng lưới rộng", 0.58);

    private final String label;
    private final double vehicleScale;

    TrafficMapType(String label, double vehicleScale) {
        this.label = label;
        this.vehicleScale = vehicleScale;
    }

    public String getLabel() {
        return label;
    }

    public double getVehicleScale() {
        return vehicleScale;
    }

    @Override
    public String toString() {
        return label;
    }
}