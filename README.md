# 🗺️ Người 4 — Road & Map System

**Module:** Road & Map System
**Công việc:** Road, lane, junction, spawning

## Cấu trúc thư mục

```
src/com/smartcity/traffic/road/      ← Toàn bộ code Java
docs/Road_Map_System_Report.md       ← Báo cáo
```

## Các lớp đã hoàn thành ✅

| Lớp | Vai trò |
|---|---|
| `MapManager.java` | Singleton — quản lý toàn bộ bản đồ |
| `Road.java` | Quản lý con đường |
| `Lane.java` | Quản lý làn đường |
| `Junction.java` | Quản lý ngã ba/tư/năm |
| `VehicleSpawner.java` | Sinh phương tiện tự động |
| `Location.java` | Tọa độ và tỷ lệ hiển thị |
| `RoadMapExample.java` | Demo chạy thử |

## Chạy demo

```bash
javac -d bin src/com/smartcity/traffic/road/*.java
java -cp bin com.smartcity.traffic.road.RoadMapExample
```
