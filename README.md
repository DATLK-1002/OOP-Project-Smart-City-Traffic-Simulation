# 🧠 Người 2 — Driver AI

**Module:** Driver AI
**Công việc:** Strategy pattern, overtaking, collision

## Cấu trúc thư mục

```
src/com/smartcity/traffic/driver/    ← Toàn bộ code Java của bạn
docs/Driver_AI_Report.md             ← Báo cáo của bạn
```

## Hướng dẫn

1. Clone repo về máy
2. Checkout nhánh này: `git checkout feature/driver-ai`
3. Đặt code Java vào `src/com/smartcity/traffic/driver/`
4. Đặt báo cáo vào `docs/Driver_AI_Report.md`
5. Commit và push lên nhánh này

## Các lớp cần làm

- `DriverBehavior.java` — Interface Strategy
- `NormalDriver.java`, `AggressiveDriver.java`, `CarefulDriver.java`
- `OvertakingStrategy.java` — Chiến lược vượt xe
- `CollisionDetector.java` — Phát hiện va chạm
