package com.smartcity.traffic.road;

import java.util.*;

/**
 * Lớp RoadMapExample minh họa cách sử dụng Road & Map System.
 * Tạo một bản đồ giao thông đơn giản với các con đường, ngã rẽ và spawner.
 */
public class RoadMapExample {

    public static void main(String[] args) {
        // Lấy instance của MapManager (Singleton)
        MapManager mapManager = MapManager.getInstance();
        mapManager.setMapWidth(1600);
        mapManager.setMapHeight(900);

        System.out.println("=== Smart City Traffic Simulation - Road & Map System ===\n");

        // 1. Tạo các con đường
        System.out.println("1. Tạo các con đường:");
        Road roadEast = new Road("R1", "Đường Đông", 500, 80,
                new Location(0, 450),
                new Location(500, 450));
        Road roadWest = new Road("R2", "Đường Tây", 500, 80,
                new Location(1600, 450),
                new Location(1100, 450));
        Road roadNorth = new Road("R3", "Đường Bắc", 400, 80,
                new Location(800, 0),
                new Location(800, 400));
        Road roadSouth = new Road("R4", "Đường Nam", 400, 80,
                new Location(800, 900),
                new Location(800, 500));

        mapManager.addRoad(roadEast);
        mapManager.addRoad(roadWest);
        mapManager.addRoad(roadNorth);
        mapManager.addRoad(roadSouth);

        System.out.println("✓ Tạo 4 con đường thành công");
        System.out.println("  - " + roadEast);
        System.out.println("  - " + roadWest);
        System.out.println("  - " + roadNorth);
        System.out.println("  - " + roadSouth);

        // 2. Tạo các làn đường cho mỗi con đường
        System.out.println("\n2. Tạo các làn đường:");
        Lane laneEast1 = new Lane("L1", Lane.Direction.EAST, 60, 40, 500);
        Lane laneEast2 = new Lane("L2", Lane.Direction.EAST, 60, 40, 500);
        roadEast.addLane(laneEast1);
        roadEast.addLane(laneEast2);

        Lane laneWest1 = new Lane("L3", Lane.Direction.WEST, 60, 40, 500);
        Lane laneWest2 = new Lane("L4", Lane.Direction.WEST, 60, 40, 500);
        roadWest.addLane(laneWest1);
        roadWest.addLane(laneWest2);

        Lane laneNorth1 = new Lane("L5", Lane.Direction.SOUTH, 60, 40, 400);
        Lane laneNorth2 = new Lane("L6", Lane.Direction.SOUTH, 60, 40, 400);
        roadNorth.addLane(laneNorth1);
        roadNorth.addLane(laneNorth2);

        Lane laneSouth1 = new Lane("L7", Lane.Direction.NORTH, 60, 40, 400);
        Lane laneSouth2 = new Lane("L8", Lane.Direction.NORTH, 60, 40, 400);
        roadSouth.addLane(laneSouth1);
        roadSouth.addLane(laneSouth2);

        System.out.println("✓ Tạo 8 làn đường thành công");
        System.out.println("  - Đường Đông: " + roadEast.getLaneCount() + " làn");
        System.out.println("  - Đường Tây: " + roadWest.getLaneCount() + " làn");
        System.out.println("  - Đường Bắc: " + roadNorth.getLaneCount() + " làn");
        System.out.println("  - Đường Nam: " + roadSouth.getLaneCount() + " làn");

        // 3. Tạo ngã rẽ (Ngã tư)
        System.out.println("\n3. Tạo ngã rẽ:");
        Junction junction1 = new Junction("J1", Junction.JunctionType.FOUR_WAY,
                new Location(800, 450));
        junction1.addConnectedRoad(roadEast);
        junction1.addConnectedRoad(roadWest);
        junction1.addConnectedRoad(roadNorth);
        junction1.addConnectedRoad(roadSouth);

        mapManager.addJunction(junction1);
        System.out.println("✓ Tạo 1 ngã tư thành công");
        System.out.println("  - " + junction1);
        System.out.println("  - Hợp lệ: " + junction1.isValid());

        // 4. Tạo spawner để sinh ra phương tiện
        System.out.println("\n4. Tạo spawner:");
        VehicleSpawner spawner1 = new VehicleSpawner("S1", laneEast1, 2.0);  // 2 xe/giây
        spawner1.addVehicleType("Car");
        spawner1.addVehicleType("Motorbike");
        spawner1.addVehicleType("Bicycle");

        VehicleSpawner spawner2 = new VehicleSpawner("S2", laneNorth1, 1.5);  // 1.5 xe/giây
        spawner2.addVehicleType("Car");
        spawner2.addVehicleType("Ambulance");
        spawner2.addVehicleType("FireTruck");

        mapManager.addSpawner(spawner1);
        mapManager.addSpawner(spawner2);

        System.out.println("✓ Tạo 2 spawner thành công");
        System.out.println("  - " + spawner1);
        System.out.println("  - " + spawner2);

        // 5. Bắt đầu sinh phương tiện
        System.out.println("\n5. Bắt đầu sinh phương tiện:");
        spawner1.startSpawning();
        spawner2.startSpawning();
        System.out.println("✓ Bắt đầu sinh phương tiện");

        // 6. Mô phỏng vòng lặp (3 giây thực tế = 180 frame @ 60 FPS)
        // Spawner S1: 2 xe/giây → sinh mỗi 0.5s → ~6 xe trong 3 giây
        // Spawner S2: 1.5 xe/giây → sinh mỗi 0.67s → ~4 xe trong 3 giây
        System.out.println("\n6. Mô phỏng 180 frame (3 giây @ 60 FPS):");
        double deltaTime = 0.016;  // ~60 FPS
        int spawnCount1 = 0, spawnCount2 = 0;
        for (int frame = 1; frame <= 180; frame++) {
            // updateSpawners() cập nhật timer; kết quả trả về được dùng trực tiếp
            for (VehicleSpawner sp : mapManager.getSpawners()) {
                boolean shouldSpawn = sp.update(deltaTime);
                if (shouldSpawn) {
                    String vehicleType = sp.spawnVehicle();
                    if (sp.getId().equals("S1")) {
                        spawnCount1++;
                        System.out.printf("  Frame %3d: [S1] Sinh %-12s | Tổng S1: %d xe\n",
                                frame, vehicleType, spawnCount1);
                    } else {
                        spawnCount2++;
                        System.out.printf("  Frame %3d: [S2] Sinh %-12s | Tổng S2: %d xe\n",
                                frame, vehicleType, spawnCount2);
                    }
                }
            }
        }
        System.out.printf("✓ Kết quả: S1 sinh %d xe, S2 sinh %d xe trong 3 giây\n",
                spawnCount1, spawnCount2);

        // 7. Thống kê bản đồ
        System.out.println("\n7. Thống kê bản đồ:");
        System.out.println("  - Tổng số con đường: " + mapManager.getRoadCount());
        System.out.println("  - Tổng số ngã rẽ: " + mapManager.getJunctionCount());
        System.out.println("  - Tổng số spawner: " + mapManager.getSpawnerCount());
        System.out.println("  - Tổng số phương tiện: " + mapManager.getTotalVehicleCount());
        System.out.println("  - Mức độ tắc nghẽn trung bình: " +
                String.format("%.2f%%", mapManager.getAverageCongestion() * 100));

        // 8. Hiển thị thông tin chi tiết
        System.out.println("\n8. Thông tin chi tiết:");
        System.out.println("  MapManager: " + mapManager);

        // 9. Dừng sinh phương tiện
        System.out.println("\n9. Dừng sinh phương tiện:");
        spawner1.stopSpawning();
        spawner2.stopSpawning();
        System.out.println("✓ Dừng sinh phương tiện");

        System.out.println("\n=== Kết thúc demo ===");
    }
}
