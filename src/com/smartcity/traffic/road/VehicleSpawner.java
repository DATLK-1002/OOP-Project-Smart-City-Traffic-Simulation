package com.smartcity.traffic.road;

import java.util.*;

/**
 * Lớp VehicleSpawner chịu trách nhiệm sinh ra các phương tiện tại các điểm đầu vào
 * của mạng lưới giao thông theo tỷ lệ nhất định.
 */
public class VehicleSpawner {
    private String id;
    private Lane spawnLane;              // Làn đường nơi sinh ra phương tiện
    private double spawnRate;            // Tỷ lệ sinh (phương tiện/giây)
    private boolean isActive;            // Trạng thái hoạt động
    private double timeSinceLastSpawn;   // Thời gian kể từ lần sinh cuối cùng
    private List<String> vehicleTypes;   // Danh sách loại phương tiện có thể sinh ra
    private Random random;

    /**
     * Khởi tạo VehicleSpawner.
     *
     * @param id        ID của spawner
     * @param spawnLane làn đường nơi sinh ra phương tiện
     * @param spawnRate tỷ lệ sinh (phương tiện/giây)
     */
    public VehicleSpawner(String id, Lane spawnLane, double spawnRate) {
        this.id = id;
        this.spawnLane = spawnLane;
        this.spawnRate = Math.max(0, spawnRate);
        this.isActive = false;
        this.timeSinceLastSpawn = 0;
        this.vehicleTypes = new ArrayList<>();
        this.random = new Random();
    }

    /**
     * Thêm loại phương tiện có thể sinh ra.
     *
     * @param vehicleType loại phương tiện (ví dụ: "Car", "Motorbike", "Ambulance")
     */
    public void addVehicleType(String vehicleType) {
        if (vehicleType != null && !vehicleTypes.contains(vehicleType)) {
            vehicleTypes.add(vehicleType);
        }
    }

    /**
     * Xóa loại phương tiện.
     *
     * @param vehicleType loại phương tiện cần xóa
     */
    public void removeVehicleType(String vehicleType) {
        vehicleTypes.remove(vehicleType);
    }

    /**
     * Lấy danh sách các loại phương tiện.
     *
     * @return danh sách loại phương tiện
     */
    public List<String> getVehicleTypes() {
        return new ArrayList<>(vehicleTypes);
    }

    /**
     * Bắt đầu sinh ra phương tiện.
     */
    public void startSpawning() {
        this.isActive = true;
        this.timeSinceLastSpawn = 0;
    }

    /**
     * Dừng sinh ra phương tiện.
     */
    public void stopSpawning() {
        this.isActive = false;
    }

    /**
     * Kiểm tra xem spawner có đang hoạt động không.
     *
     * @return true nếu đang hoạt động
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * Cập nhật trạng thái spawner và quyết định có nên sinh ra phương tiện không.
     * Phương thức này được gọi trong mỗi vòng lặp mô phỏng.
     *
     * @param deltaTime thời gian kể từ frame trước (giây)
     * @return true nếu đã sinh ra phương tiện
     */
    public boolean update(double deltaTime) {
        if (!isActive || spawnLane == null || vehicleTypes.isEmpty()) {
            return false;
        }

        timeSinceLastSpawn += deltaTime;

        // Tính thời gian giữa hai lần sinh
        double timeBetweenSpawns = 1.0 / spawnRate;

        if (timeSinceLastSpawn >= timeBetweenSpawns) {
            timeSinceLastSpawn = 0;
            return true;  // Nên sinh ra phương tiện
        }

        return false;
    }

    /**
     * Sinh ra một phương tiện ngẫu nhiên từ danh sách loại phương tiện.
     *
     * @return loại phương tiện được chọn
     */
    public String spawnVehicle() {
        if (vehicleTypes.isEmpty()) {
            return null;
        }
        return vehicleTypes.get(random.nextInt(vehicleTypes.size()));
    }

    /**
     * Sinh ra một phương tiện với loại cụ thể.
     *
     * @param vehicleType loại phương tiện
     * @return loại phương tiện nếu có trong danh sách, null nếu không
     */
    public String spawnVehicle(String vehicleType) {
        if (vehicleTypes.contains(vehicleType)) {
            return vehicleType;
        }
        return null;
    }

    // Getter và Setter
    public String getId() {
        return id;
    }

    public Lane getSpawnLane() {
        return spawnLane;
    }

    public void setSpawnLane(Lane spawnLane) {
        this.spawnLane = spawnLane;
    }

    public double getSpawnRate() {
        return spawnRate;
    }

    public void setSpawnRate(double spawnRate) {
        this.spawnRate = Math.max(0, spawnRate);
    }

    @Override
    public String toString() {
        return String.format("VehicleSpawner(id=%s, spawnRate=%.2f, active=%s, types=%s)",
                id, spawnRate, isActive, vehicleTypes);
    }
}
