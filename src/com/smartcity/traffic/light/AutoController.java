package com.smartcity.traffic.light;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bộ điều khiển đèn giao thông ở chế độ TỰ ĐỘNG.
 *
 * <p>Mỗi lần {@link #tick()} được gọi (đại diện cho 1 đơn vị thời gian, vd:
 * 1 giây trôi qua trong vòng lặp mô phỏng / game loop), tất cả các đèn do
 * controller này quản lý sẽ giảm bộ đếm giây và tự chuyển trạng thái theo
 * chu trình GREEN -> YELLOW -> RED -> GREEN khi hết thời gian.</p>
 */
public class AutoController {

    private final List<TrafficLight> lights = new ArrayList<>();

    /** Đăng ký một đèn giao thông để controller này quản lý. */
    public void addLight(TrafficLight light) {
        if (light != null && !lights.contains(light)) {
            lights.add(light);
            light.setAutoMode(true);
        }
    }

    /** Hủy đăng ký một đèn giao thông. */
    public void removeLight(TrafficLight light) {
        lights.remove(light);
    }

    /** Trả về danh sách (không thể thay đổi) các đèn đang được quản lý. */
    public List<TrafficLight> getLights() {
        return Collections.unmodifiableList(lights);
    }

    /**
     * Tiến hành một bước thời gian cho toàn bộ đèn: mỗi đèn sẽ tự giảm bộ
     * đếm giây và chuyển trạng thái khi cần thiết.
     */
    public void tick() {
        for (TrafficLight light : lights) {
            light.tick();
        }
    }
}
