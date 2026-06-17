package model.map;

public enum TrafficMapType {
    T_JUNCTION("Ngã ba", 1.25),
    CROSS_JUNCTION("Ngã tư", 1.25),
    FIVE_WAY_JUNCTION("Ngã năm", 1.18),
    ROAD_NETWORK("Mạng lưới rộng", 0.72);

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