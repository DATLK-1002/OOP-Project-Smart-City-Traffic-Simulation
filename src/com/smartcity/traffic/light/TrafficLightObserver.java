package com.smartcity.traffic.light;

/**
 * Observer pattern: các đối tượng quan tâm đến thay đổi trạng thái
 * của đèn giao thông (ví dụ: xe, GUI, bộ điều khiển khác) implement
 * interface này để được thông báo khi đèn đổi màu hoặc đếm giây.
 */
public interface TrafficLightObserver {

    /**
     * Được gọi mỗi khi đèn giao thông thay đổi trạng thái (màu).
     *
     * @param light    đèn giao thông phát sự kiện
     * @param newState trạng thái mới của đèn
     */
    void onStateChanged(TrafficLight light, TrafficLightState newState);

    /**
     * Được gọi mỗi khi đèn giao thông cập nhật bộ đếm giây (nếu có).
     * Các đèn không hỗ trợ đếm giây có thể không gọi phương thức này.
     *
     * @param light          đèn giao thông phát sự kiện
     * @param secondsLeft    số giây còn lại của trạng thái hiện tại
     */
    void onCountdownTick(TrafficLight light, int secondsLeft);
}
