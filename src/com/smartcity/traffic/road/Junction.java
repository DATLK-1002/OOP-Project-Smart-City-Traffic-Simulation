package com.smartcity.traffic.road;

import java.util.*;

/**
 * Lớp Junction đại diện cho các ngã rẽ (Ngã 3, Ngã 4, Ngã 5) trong mạng lưới giao thông.
 * Mỗi Junction chứa danh sách các Road kết nối tới và quản lý luồng giao thông tại điểm này.
 */
public class Junction {
    private String id;
    private JunctionType type;           // Loại ngã rẽ: THREE_WAY, FOUR_WAY, FIVE_WAY
    private Location location;           // Vị trí của ngã rẽ
    private List<Road> connectedRoads;   // Danh sách các con đường kết nối
    private Object trafficLight;         // Đèn giao thông (Object để tương thích với TrafficLight)
    private double scale;                // Tỷ lệ hiển thị tại ngã rẽ (thường > 1.0 để zoom in)

    /**
     * Enum định nghĩa các loại ngã rẽ.
     */
    public enum JunctionType {
        THREE_WAY,   // Ngã ba
        FOUR_WAY,    // Ngã tư
        FIVE_WAY     // Ngã năm
    }

    /**
     * Khởi tạo Junction.
     *
     * @param id       ID của ngã rẽ
     * @param type     loại ngã rẽ
     * @param location vị trí của ngã rẽ
     */
    public Junction(String id, JunctionType type, Location location) {
        this.id = id;
        this.type = type;
        this.location = location;
        this.connectedRoads = new ArrayList<>();
        this.trafficLight = null;
        this.scale = 1.5;  // Mặc định zoom in 1.5x tại ngã rẽ
    }

    /**
     * Thêm một con đường kết nối tới ngã rẽ.
     *
     * @param road con đường cần kết nối
     */
    public void addConnectedRoad(Road road) {
        if (road != null && !connectedRoads.contains(road)) {
            connectedRoads.add(road);
        }
    }

    /**
     * Xóa một con đường khỏi danh sách kết nối.
     *
     * @param road con đường cần xóa
     */
    public void removeConnectedRoad(Road road) {
        connectedRoads.remove(road);
    }

    /**
     * Lấy danh sách các con đường kết nối.
     *
     * @return danh sách con đường
     */
    public List<Road> getConnectedRoads() {
        return new ArrayList<>(connectedRoads);
    }

    /**
     * Lấy số lượng con đường kết nối.
     *
     * @return số lượng con đường
     */
    public int getConnectedRoadCount() {
        return connectedRoads.size();
    }

    /**
     * Kiểm tra xem ngã rẽ có hợp lệ không (số con đường kết nối phù hợp với loại ngã rẽ).
     *
     * @return true nếu hợp lệ
     */
    public boolean isValid() {
        int expectedCount = switch (type) {
            case THREE_WAY -> 3;
            case FOUR_WAY -> 4;
            case FIVE_WAY -> 5;
        };
        return connectedRoads.size() == expectedCount;
    }

    /**
     * Định tuyến phương tiện từ một con đường này sang con đường khác.
     * (Phương thức này có thể được mở rộng để thực hiện logic định tuyến phức tạp hơn)
     *
     * @param fromRoad con đường nguồn
     * @param toRoad   con đường đích
     * @return true nếu định tuyến thành công
     */
    public boolean routeVehicle(Road fromRoad, Road toRoad) {
        return connectedRoads.contains(fromRoad) && connectedRoads.contains(toRoad);
    }

    /**
     * Lấy tổng số phương tiện tại ngã rẽ.
     *
     * @return tổng số phương tiện
     */
    public int getTotalVehicleCount() {
        return connectedRoads.stream()
                .mapToInt(Road::getTotalVehicleCount)
                .sum();
    }

    // Getter và Setter
    public String getId() {
        return id;
    }

    public JunctionType getType() {
        return type;
    }

    public Location getLocation() {
        return location;
    }

    public Object getTrafficLight() {
        return trafficLight;
    }

    public void setTrafficLight(Object trafficLight) {
        this.trafficLight = trafficLight;
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = Math.max(0.5, scale);  // Đảm bảo scale không quá nhỏ
    }

    @Override
    public String toString() {
        return String.format("Junction(id=%s, type=%s, roads=%d, vehicles=%d, scale=%.2f)",
                id, type, connectedRoads.size(), getTotalVehicleCount(), scale);
    }
}
