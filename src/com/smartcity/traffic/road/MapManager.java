package com.smartcity.traffic.road;

import java.util.*;

/**
 * Lớp MapManager quản lý toàn bộ mạng lưới đường bộ trong hệ thống mô phỏng.
 * Được thiết kế theo mẫu Singleton để đảm bảo chỉ có một instance duy nhất.
 * MapManager chứa danh sách các Road, Junction và VehicleSpawner.
 */
public class MapManager {
    private static MapManager instance;  // Instance duy nhất
    private Map<String, Road> roads;     // Danh sách con đường theo ID
    private Map<String, Junction> junctions;  // Danh sách ngã rẽ theo ID
    private Map<String, VehicleSpawner> spawners;  // Danh sách spawner theo ID
    private double mapWidth;             // Chiều rộng bản đồ
    private double mapHeight;            // Chiều cao bản đồ

    /**
     * Khởi tạo private để ngăn chặn việc tạo instance từ bên ngoài.
     */
    private MapManager() {
        this.roads = new LinkedHashMap<>();
        this.junctions = new LinkedHashMap<>();
        this.spawners = new LinkedHashMap<>();
        this.mapWidth = 1600;  // Mặc định 1600 pixel
        this.mapHeight = 900;  // Mặc định 900 pixel
    }

    /**
     * Lấy instance duy nhất của MapManager (Singleton Pattern).
     *
     * @return instance của MapManager
     */
    public static synchronized MapManager getInstance() {
        if (instance == null) {
            instance = new MapManager();
        }
        return instance;
    }

    /**
     * Thêm một con đường vào bản đồ.
     *
     * @param road con đường cần thêm
     */
    public void addRoad(Road road) {
        if (road != null) {
            roads.put(road.getId(), road);
        }
    }

    /**
     * Xóa một con đường khỏi bản đồ.
     *
     * @param roadId ID của con đường
     */
    public void removeRoad(String roadId) {
        roads.remove(roadId);
    }

    /**
     * Lấy một con đường theo ID.
     *
     * @param roadId ID của con đường
     * @return con đường, hoặc null nếu không tìm thấy
     */
    public Road getRoad(String roadId) {
        return roads.get(roadId);
    }

    /**
     * Lấy danh sách tất cả các con đường.
     *
     * @return danh sách con đường
     */
    public List<Road> getRoads() {
        return new ArrayList<>(roads.values());
    }

    /**
     * Lấy số lượng con đường.
     *
     * @return số lượng con đường
     */
    public int getRoadCount() {
        return roads.size();
    }

    /**
     * Thêm một ngã rẽ vào bản đồ.
     *
     * @param junction ngã rẽ cần thêm
     */
    public void addJunction(Junction junction) {
        if (junction != null) {
            junctions.put(junction.getId(), junction);
        }
    }

    /**
     * Xóa một ngã rẽ khỏi bản đồ.
     *
     * @param junctionId ID của ngã rẽ
     */
    public void removeJunction(String junctionId) {
        junctions.remove(junctionId);
    }

    /**
     * Lấy một ngã rẽ theo ID.
     *
     * @param junctionId ID của ngã rẽ
     * @return ngã rẽ, hoặc null nếu không tìm thấy
     */
    public Junction getJunction(String junctionId) {
        return junctions.get(junctionId);
    }

    /**
     * Lấy danh sách tất cả các ngã rẽ.
     *
     * @return danh sách ngã rẽ
     */
    public List<Junction> getJunctions() {
        return new ArrayList<>(junctions.values());
    }

    /**
     * Lấy số lượng ngã rẽ.
     *
     * @return số lượng ngã rẽ
     */
    public int getJunctionCount() {
        return junctions.size();
    }

    /**
     * Thêm một spawner vào bản đồ.
     *
     * @param spawner spawner cần thêm
     */
    public void addSpawner(VehicleSpawner spawner) {
        if (spawner != null) {
            spawners.put(spawner.getId(), spawner);
        }
    }

    /**
     * Xóa một spawner khỏi bản đồ.
     *
     * @param spawnerId ID của spawner
     */
    public void removeSpawner(String spawnerId) {
        spawners.remove(spawnerId);
    }

    /**
     * Lấy một spawner theo ID.
     *
     * @param spawnerId ID của spawner
     * @return spawner, hoặc null nếu không tìm thấy
     */
    public VehicleSpawner getSpawner(String spawnerId) {
        return spawners.get(spawnerId);
    }

    /**
     * Lấy danh sách tất cả các spawner.
     *
     * @return danh sách spawner
     */
    public List<VehicleSpawner> getSpawners() {
        return new ArrayList<>(spawners.values());
    }

    /**
     * Lấy số lượng spawner.
     *
     * @return số lượng spawner
     */
    public int getSpawnerCount() {
        return spawners.size();
    }

    /**
     * Lấy tổng số phương tiện trên toàn bộ bản đồ.
     *
     * @return tổng số phương tiện
     */
    public int getTotalVehicleCount() {
        return roads.values().stream()
                .mapToInt(Road::getTotalVehicleCount)
                .sum();
    }

    /**
     * Lấy mức độ tắc nghẽn trung bình của toàn bộ bản đồ.
     *
     * @return mức độ tắc nghẽn (0.0 - 1.0)
     */
    public double getAverageCongestion() {
        if (roads.isEmpty()) {
            return 0.0;
        }
        return roads.values().stream()
                .mapToDouble(Road::getAverageCongestion)
                .average()
                .orElse(0.0);
    }

    /**
     * Cập nhật tất cả các spawner.
     *
     * @param deltaTime thời gian kể từ frame trước (giây)
     */
    public void updateSpawners(double deltaTime) {
        for (VehicleSpawner spawner : spawners.values()) {
            spawner.update(deltaTime);
        }
    }

    /**
     * Xóa toàn bộ dữ liệu bản đồ (Reset).
     */
    public void clear() {
        roads.clear();
        junctions.clear();
        spawners.clear();
    }

    // Getter và Setter
    public double getMapWidth() {
        return mapWidth;
    }

    public void setMapWidth(double mapWidth) {
        this.mapWidth = Math.max(100, mapWidth);
    }

    public double getMapHeight() {
        return mapHeight;
    }

    public void setMapHeight(double mapHeight) {
        this.mapHeight = Math.max(100, mapHeight);
    }

    @Override
    public String toString() {
        return String.format("MapManager(roads=%d, junctions=%d, spawners=%d, vehicles=%d, congestion=%.2f%%)",
                roads.size(), junctions.size(), spawners.size(), getTotalVehicleCount(),
                getAverageCongestion() * 100);
    }
}
