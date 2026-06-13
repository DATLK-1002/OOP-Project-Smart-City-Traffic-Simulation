package com.smartcity.traffic.light;

/**
 * Trạng thái (màu) của đèn giao thông.
 */
public enum TrafficLightState {
    RED,
    YELLOW,
    GREEN;

    /**
     * Trả về trạng thái kế tiếp theo chu trình mặc định:
     * GREEN -> YELLOW -> RED -> GREEN
     */
    public TrafficLightState next() {
        switch (this) {
            case GREEN:
                return YELLOW;
            case YELLOW:
                return RED;
            case RED:
                return GREEN;
            default:
                return RED;
        }
    }
}
