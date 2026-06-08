package com.smartcity.traffic.road;

import java.util.*;

/**
 * Lớp Lane đại diện cho một làn đường cụ thể trên một con đường.
 * Mỗi Lane chứa danh sách các phương tiện đang di chuyển trên nó.
 */
public class Lane {
    private String id;
    private Direction direction;  // Hướng di chuyển: NORTH, SOUTH, EAST, WEST
    private double speedLimit;    // Giới hạn tốc độ (km/h)
    private List<Object> vehicles;  // Danh sách phương tiện trên làn này (Object để tương thích với Vehicle)
    private double width;         // Chiều rộng làn đường (pixel)
    private double length;        // Chiều dài làn đường (pixel)

    /**
     * Enum định nghĩa các hướng di chuyển.
     */
    public enum Direction {
        NORTH, SOUTH, EAST, WEST
    }

    /**
     * Khởi tạo Lane.
     *
     * @param id          ID của làn
     * @param direction   hướng di chuyển
     * @param speedLimit  giới hạn tốc độ
     * @param width       chiều rộng làn
     * @param length      chiều dài làn
     */
    public Lane(String id, Direction direction, double speedLimit, double width, double length) {
        this.id = id;
        this.direction = direction;
        this.speedLimit = speedLimit;
        this.width = width;
        this.length = length;
        this.vehicles = new ArrayList<>();
    }

    /**
     * Thêm phương tiện vào làn.
     *
     * @param vehicle phương tiện cần thêm
     */
    public void addVehicle(Object vehicle) {
        if (vehicle != null && !vehicles.contains(vehicle)) {
            vehicles.add(vehicle);
        }
    }

    /**
     * Xóa phương tiện khỏi làn.
     *
     * @param vehicle phương tiện cần xóa
     */
    public void removeVehicle(Object vehicle) {
        vehicles.remove(vehicle);
    }

    /**
     * Lấy danh sách các phương tiện trên làn.
     *
     * @return danh sách phương tiện
     */
    public List<Object> getVehicles() {
        return new ArrayList<>(vehicles);
    }

    /**
     * Lấy số lượng phương tiện trên làn.
     *
     * @return số lượng phương tiện
     */
    public int getVehicleCount() {
        return vehicles.size();
    }

    /**
     * Kiểm tra xem làn có trống không.
     *
     * @return true nếu làn trống
     */
    public boolean isEmpty() {
        return vehicles.isEmpty();
    }

    /**
     * Lấy mức độ tắc nghẽn của làn (0.0 - 1.0).
     *
     * @return mức độ tắc nghẽn
     */
    public double getCongestionLevel() {
        // Giả sử mỗi phương tiện chiếm 50 pixel
        double maxVehicles = length / 50.0;
        return Math.min(1.0, vehicles.size() / maxVehicles);
    }

    // Getter và Setter
    public String getId() {
        return id;
    }

    public Direction getDirection() {
        return direction;
    }

    public double getSpeedLimit() {
        return speedLimit;
    }

    public void setSpeedLimit(double speedLimit) {
        this.speedLimit = Math.max(0, speedLimit);
    }

    public double getWidth() {
        return width;
    }

    public double getLength() {
        return length;
    }

    @Override
    public String toString() {
        return String.format("Lane(id=%s, direction=%s, speedLimit=%.2f, vehicles=%d)",
                id, direction, speedLimit, vehicles.size());
    }
}
