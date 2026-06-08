# Smart City Traffic Simulation - Road & Map System

## Giới thiệu

Đây là phần **Road & Map System** của dự án **Smart City Traffic Simulation**. Module này quản lý toàn bộ cơ sở hạ tầng giao thông tĩnh bao gồm các con đường, làn đường, ngã rẽ và cơ chế sinh phương tiện.

## Cấu trúc thư mục

```
smartcity/
├── src/
│   └── com/smartcity/traffic/road/
│       ├── Location.java           # Quản lý tọa độ và tỷ lệ hiển thị
│       ├── Lane.java               # Lớp làn đường
│       ├── Road.java               # Lớp con đường
│       ├── Junction.java           # Lớp ngã rẽ
│       ├── VehicleSpawner.java     # Lớp sinh phương tiện
│       ├── MapManager.java         # Lớp quản lý bản đồ (Singleton)
│       └── RoadMapExample.java     # Ví dụ sử dụng
├── docs/
│   ├── Road_Map_System_Report.md   # Báo cáo (Markdown)
│   ├── Road_Map_System_Report.docx # Báo cáo (Word)
│   └── Road_Map_Plan.md            # Kế hoạch chi tiết
└── bin/
    └── (compiled Java classes)
```

## Các lớp chính

### MapManager (Singleton)
Quản lý toàn bộ bản đồ giao thông. Cung cấp các phương thức để thêm/xóa con đường, ngã rẽ và spawner.

```java
MapManager mapManager = MapManager.getInstance();
mapManager.addRoad(road);
mapManager.addJunction(junction);
```

### Road
Đại diện cho một con đường, chứa nhiều Lane.

```java
Road road = new Road("R1", "Đường Đông", 500, 80, startLoc, endLoc);
road.addLane(lane);
```

### Lane
Đại diện cho một làn đường với hướng di chuyển cố định.

```java
Lane lane = new Lane("L1", Direction.EAST, 60, 40, 500);
lane.addVehicle(vehicle);
```

### Junction
Quản lý các ngã rẽ (ngã ba, ngã tư, ngã năm).

```java
Junction junction = new Junction("J1", JunctionType.FOUR_WAY, location);
junction.addConnectedRoad(road1);
junction.addConnectedRoad(road2);
```

### VehicleSpawner
Sinh ra các phương tiện tại các điểm đầu vào.

```java
VehicleSpawner spawner = new VehicleSpawner("S1", lane, 2.0);
spawner.addVehicleType("Car");
spawner.startSpawning();
```

## Cách biên dịch và chạy

### Biên dịch
```bash
cd smartcity
mkdir -p bin
javac -d bin src/com/smartcity/traffic/road/*.java
```

### Chạy ví dụ
```bash
java -cp bin com.smartcity.traffic.road.RoadMapExample
```

## Tính năng chính

- ✓ Quản lý mạng lưới đường bộ phức tạp
- ✓ Hỗ trợ ngã ba, ngã tư, ngã năm
- ✓ Sinh phương tiện tự động với tỷ lệ tùy chỉnh
- ✓ Tính toán mức độ tắc nghẽn
- ✓ Hỗ trợ zoom in/out động tại ngã rẽ
- ✓ Thiết kế mở rộng dễ dàng

## Các kỹ thuật OOP áp dụng

- **Encapsulation**: Che giấu dữ liệu nội bộ
- **Composition**: Xây dựng cấu trúc phân cấp
- **Singleton Pattern**: Đảm bảo một phiên bản bản đồ duy nhất
- **Enum**: Định nghĩa các loại hướng, loại ngã rẽ

## Hướng phát triển tương lai

1. Tích hợp với Traffic Light System để điều tiết giao thông
2. Kết nối với Vehicle System để quản lý phương tiện
3. Thêm hỗ trợ cho các loại ngã rẽ phức tạp hơn (vòng xuyến, ngã sáu)
4. Cải thiện thuật toán định tuyến phương tiện

## Tác giả

**Tiến Đạt** - Phần Road & Map System

---

*Dự án Smart City Traffic Simulation - Hệ thống mô phỏng giao thông đô thị*
