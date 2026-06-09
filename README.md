# 🚦 Người 3 — Traffic Light System

**Module:** Traffic Light System
**Công việc:** TrafficLight, auto/manual, Observer pattern

## Cấu trúc thư mục

```
src/com/smartcity/traffic/light/     ← Toàn bộ code Java của bạn
docs/Traffic_Light_System_Report.md  ← Báo cáo của bạn
```

## Hướng dẫn

1. Clone repo về máy
2. Checkout nhánh này: `git checkout feature/traffic-light-system`
3. Đặt code Java vào `src/com/smartcity/traffic/light/`
4. Đặt báo cáo vào `docs/Traffic_Light_System_Report.md`
5. Commit và push lên nhánh này

## Các lớp cần làm

- `TrafficLight.java` — Đèn giao thông
- `TrafficLightState.java` — Enum trạng thái (RED, YELLOW, GREEN)
- `AutoController.java`, `ManualController.java`
- `TrafficLightObserver.java` — Observer pattern
