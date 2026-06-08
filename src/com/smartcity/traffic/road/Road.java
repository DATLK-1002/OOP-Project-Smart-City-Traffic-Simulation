package com.smartcity.traffic.road;

import java.util.*;

/**
 * Lớp Road đại diện cho một con đường trong mạng lưới giao thông.
 * Mỗi Road chứa nhiều Lane (làn đường).
 */
public class Road {
    private String id;
    private String name;
    private double length;           // Chiều dài con đường (pixel)
    private double width;            // Chiều rộng con đường (pixel)
    private List<Lane> lanes;        // Danh sách các làn đường
    private Location startLocation;  // Vị trí bắt đầu
    private Location endLocation;    // Vị trí kết thúc

    /**
     * Khởi tạo Road.
     *
     * @param id             ID của con đường
     * @param name           tên con đường
     * @param length         chiều dài
     * @param width          chiều rộng
     * @param startLocation  vị trí bắt đầu
     * @param endLocation    vị trí kết thúc
     */
    public Road(String id, String name, double length, double width,
                Location startLocation, Location endLocation) {
        this.id = id;
        this.name = name;
        this.length = length;
        this.width = width;
        this.startLocation = startLocation;
        this.endLocation = endLocation;
        this.lanes = new ArrayList<>();
    }

    /**
     * Thêm một làn đường vào con đường.
     *
     * @param lane làn đường cần thêm
     */
    public void addLane(Lane lane) {
        if (lane != null && !lanes.contains(lane)) {
            lanes.add(lane);
        }
    }

    /**
     * Xóa một làn đường khỏi con đường.
     *
     * @param lane làn đường cần xóa
     */
    public void removeLane(Lane lane) {
        lanes.remove(lane);
    }

    /**
     * Lấy danh sách các làn đường.
     *
     * @return danh sách làn
     */
    public List<Lane> getLanes() {
        return new ArrayList<>(lanes);
    }

    /**
     * Lấy số lượng làn đường.
     *
     * @return số lượng làn
     */
    public int getLaneCount() {
        return lanes.size();
    }

    /**
     * Lấy một làn đường theo chỉ số.
     *
     * @param index chỉ số làn
     * @return làn đường
     */
    public Lane getLaneByIndex(int index) {
        if (index >= 0 && index < lanes.size()) {
            return lanes.get(index);
        }
        return null;
    }

    /**
     * Lấy tổng số phương tiện trên toàn bộ con đường.
     *
     * @return tổng số phương tiện
     */
    public int getTotalVehicleCount() {
        return lanes.stream().mapToInt(Lane::getVehicleCount).sum();
    }

    /**
     * Tính mức độ tắc nghẽn trung bình của con đường.
     *
     * @return mức độ tắc nghẽn (0.0 - 1.0)
     */
    public double getAverageCongestion() {
        if (lanes.isEmpty()) {
            return 0.0;
        }
        return lanes.stream()
                .mapToDouble(Lane::getCongestionLevel)
                .average()
                .orElse(0.0);
    }

    // Getter và Setter
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLength() {
        return length;
    }

    public double getWidth() {
        return width;
    }

    public Location getStartLocation() {
        return startLocation;
    }

    public Location getEndLocation() {
        return endLocation;
    }

    @Override
    public String toString() {
        return String.format("Road(id=%s, name=%s, length=%.2f, lanes=%d, vehicles=%d)",
                id, name, length, lanes.size(), getTotalVehicleCount());
    }
}
