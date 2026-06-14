# 🏗️ Người 6 — Architecture & Integration

**Module:** Architecture & Integration
**Công việc:** MVC, Factory, UML, report, merge

## Cấu trúc thư mục

```
src/com/smartcity/core/              ← Code kiến trúc chung (MVC, Factory)
docs/Architecture_Report.md         ← Báo cáo tổng thể
docs/UML/                            ← Các biểu đồ UML tổng thể
```

## Hướng dẫn

1. Clone repo về máy
2. Checkout nhánh này: `git checkout feature/architecture-integration`
3. Đặt code vào `src/com/smartcity/core/`
4. Đặt báo cáo vào `docs/`
5. **Khi tích hợp:** Merge các nhánh feature vào `develop`, sau đó merge `develop` vào `main`

## Quy trình merge

```bash
git checkout develop
git merge feature/vehicle-system
git merge feature/driver-ai
git merge feature/traffic-light-system
git merge feature/road-map-system
git merge feature/gui-multimedia
git push origin develop
```

## Các lớp cần làm

- `SimulationController.java` — MVC Controller
- `VehicleFactory.java` — Factory Pattern tạo phương tiện
- `SimulationModel.java` — MVC Model
- `AppConfig.java` — Cấu hình toàn hệ thống
