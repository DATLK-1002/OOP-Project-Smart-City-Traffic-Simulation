package controller;

public enum TrafficDensity {
    LIGHT("Ít", 7),
    HEAVY("Đông đúc", 18);

    private final String label;
    private final int vehicleCount;

    TrafficDensity(String label, int vehicleCount) {
        this.label = label;
        this.vehicleCount = vehicleCount;
    }

    public int getVehicleCount() {
        return vehicleCount;
    }

    @Override
    public String toString() {
        return label;
    }
}