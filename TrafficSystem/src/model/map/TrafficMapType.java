package model.map;

public enum TrafficMapType {
    T_JUNCTION("Ngã ba", 1.10, 3),
    CROSS_JUNCTION("Ngã tư", 1.10, 4),
    ROAD_NETWORK("Mạng lưới rộng", 0.55, 8);

    private final String label;
    private final double vehicleScale;
    private final int trafficLightCount;

    TrafficMapType(String label, double vehicleScale, int trafficLightCount) {
        this.label = label;
        this.vehicleScale = vehicleScale;
        this.trafficLightCount = trafficLightCount;
    }

    public String getLabel() {
        return label;
    }

    public double getVehicleScale() {
        return vehicleScale;
    }

    public int getTrafficLightCount() {
        return trafficLightCount;
    }

    public boolean isWideArea() {
        return this == ROAD_NETWORK;
    }

    @Override
    public String toString() {
        return label;
    }
}
