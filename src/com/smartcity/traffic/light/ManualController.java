package com.smartcity.traffic.light;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bộ điều khiển đèn giao thông ở chế độ THỦ CÔNG.
 *
 * <p>Ở chế độ này, đèn không tự chuyển trạng thái theo thời gian. Người dùng
 * (qua GUI) click vào đèn, GUI gọi {@link #switchLight(TrafficLight)} hoặc
 * {@link #setLightState(TrafficLight, TrafficLightState)} để đổi trạng thái
 * đèn ngay lập tức.</p>
 */
public class ManualController {

    private final List<TrafficLight> lights = new ArrayList<>();

    /** Đăng ký một đèn giao thông để controller này quản lý. */
    public void addLight(TrafficLight light) {
        if (light != null && !lights.contains(light)) {
            lights.add(light);
            // Ở chế độ thủ công, đèn không tự đổi theo thời gian.
            light.setAutoMode(false);
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
     * Người dùng click vào đèn -> chuyển sang trạng thái kế tiếp ngay.
     *
     * @param light đèn được click
     */
    public void switchLight(TrafficLight light) {
        if (light != null && lights.contains(light)) {
            light.switchToNext();
        }
    }

    /**
     * Đặt trực tiếp đèn về một trạng thái cụ thể (vd: người dùng chọn màu
     * mong muốn từ menu thay vì chỉ next).
     *
     * @param light đèn cần đổi
     * @param state trạng thái mong muốn
     */
    public void setLightState(TrafficLight light, TrafficLightState state) {
        if (light != null && lights.contains(light)) {
            light.setState(state);
        }
    }
}
