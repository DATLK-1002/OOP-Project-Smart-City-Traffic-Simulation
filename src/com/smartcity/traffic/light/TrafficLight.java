package com.smartcity.traffic.light;

import java.util.ArrayList;
import java.util.List;

/**
 * Lớp đại diện cho một đèn giao thông (TrafficLight) tại một ngã rẽ.
 *
 * <p>Đóng vai trò Subject trong Observer pattern: mọi xe / GUI / module khác
 * muốn theo dõi trạng thái đèn chỉ cần implement {@link TrafficLightObserver}
 * và đăng ký qua {@link #addObserver(TrafficLightObserver)}.</p>
 *
 * <p>Hỗ trợ 3 kiểu hiển thị đếm giây thông qua enum {@link CountdownMode}:
 * <ul>
 *     <li>{@code NONE} - không đếm số giây</li>
 *     <li>{@code FULL} - đếm số giây liên tục</li>
 *     <li>{@code LAST_TEN} - chỉ đếm số giây khi còn 10 giây cuối</li>
 * </ul>
 * </p>
 *
 * <p>Lớp này không phụ thuộc vào tọa độ hay vẽ hình (logic vẽ tách riêng ở
 * tầng GUI / module view), chỉ chịu trách nhiệm quản lý trạng thái và thời
 * gian của đèn.</p>
 */
public class TrafficLight {

    /**
     * Kiểu hiển thị đếm giây của đèn.
     */
    public enum CountdownMode {
        /** Không hiển thị số giây. */
        NONE,
        /** Hiển thị số giây cho mọi trạng thái. */
        FULL,
        /** Chỉ hiển thị số giây khi còn <= 10 giây. */
        LAST_TEN
    }

    private final String id;
    private TrafficLightState state;
    private final CountdownMode countdownMode;

    // Thời gian (giây) cho từng trạng thái
    private int greenDuration;
    private int yellowDuration;
    private int redDuration;

    // Số giây còn lại của trạng thái hiện tại
    private int secondsLeft;

    // true nếu đèn đang ở chế độ tự động (tự chuyển theo thời gian)
    private boolean autoMode;

    private final List<TrafficLightObserver> observers = new ArrayList<>();

    /**
     * Tạo đèn giao thông mới.
     *
     * @param id             tên/định danh đèn (vd: "NorthLight")
     * @param greenDuration  thời gian đèn xanh (giây)
     * @param yellowDuration thời gian đèn vàng (giây)
     * @param redDuration    thời gian đèn đỏ (giây)
     * @param countdownMode  kiểu hiển thị đếm giây
     * @param initialState   trạng thái khởi đầu
     */
    public TrafficLight(String id, int greenDuration, int yellowDuration, int redDuration,
                         CountdownMode countdownMode, TrafficLightState initialState) {
        this.id = id;
        this.greenDuration = greenDuration;
        this.yellowDuration = yellowDuration;
        this.redDuration = redDuration;
        this.countdownMode = countdownMode;
        this.state = initialState;
        this.secondsLeft = durationOf(initialState);
        this.autoMode = true;
    }

    /**
     * Tạo đèn giao thông với chế độ đếm giây mặc định (FULL) và bắt đầu ở
     * trạng thái RED.
     */
    public TrafficLight(String id, int greenDuration, int yellowDuration, int redDuration) {
        this(id, greenDuration, yellowDuration, redDuration, CountdownMode.FULL, TrafficLightState.RED);
    }

    // ---------------------------------------------------------------
    // Observer pattern
    // ---------------------------------------------------------------

    /** Đăng ký một observer để nhận thông báo khi đèn thay đổi. */
    public void addObserver(TrafficLightObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    /** Hủy đăng ký observer. */
    public void removeObserver(TrafficLightObserver observer) {
        observers.remove(observer);
    }

    private void notifyStateChanged() {
        for (TrafficLightObserver obs : observers) {
            obs.onStateChanged(this, state);
        }
    }

    private void notifyCountdownTick() {
        for (TrafficLightObserver obs : observers) {
            obs.onCountdownTick(this, secondsLeft);
        }
    }

    // ---------------------------------------------------------------
    // Logic vận hành đèn
    // ---------------------------------------------------------------

    /**
     * Tiến hành 1 "tick" thời gian (vd: 1 giây trôi qua). Chỉ có tác dụng
     * khi đèn đang ở chế độ tự động. Khi hết thời gian của trạng thái hiện
     * tại, đèn sẽ tự chuyển sang trạng thái kế tiếp.
     */
    public void tick() {
        if (!autoMode) {
            return;
        }
        secondsLeft--;
        if (secondsLeft <= 0) {
            advanceState();
        } else {
            notifyCountdownTick();
        }
    }

    /**
     * Chuyển đèn sang trạng thái kế tiếp ngay lập tức và đặt lại bộ đếm giây
     * theo thời gian cấu hình của trạng thái mới.
     */
    private void advanceState() {
        setState(state.next());
    }

    /**
     * Đặt trạng thái mới cho đèn (dùng cho cả tự động và thủ công) và reset
     * lại bộ đếm giây tương ứng.
     */
    public void setState(TrafficLightState newState) {
        this.state = newState;
        this.secondsLeft = durationOf(newState);
        notifyStateChanged();
        notifyCountdownTick();
    }

    /**
     * Chuyển đèn sang trạng thái kế tiếp (dùng cho điều khiển thủ công khi
     * người dùng click vào đèn).
     */
    public void switchToNext() {
        advanceState();
    }

    private int durationOf(TrafficLightState s) {
        switch (s) {
            case GREEN:
                return greenDuration;
            case YELLOW:
                return yellowDuration;
            case RED:
                return redDuration;
            default:
                return 0;
        }
    }

    // ---------------------------------------------------------------
    // Getter / Setter
    // ---------------------------------------------------------------

    public String getId() {
        return id;
    }

    public TrafficLightState getState() {
        return state;
    }

    public int getSecondsLeft() {
        return secondsLeft;
    }

    public CountdownMode getCountdownMode() {
        return countdownMode;
    }

    public boolean isAutoMode() {
        return autoMode;
    }

    public void setAutoMode(boolean autoMode) {
        this.autoMode = autoMode;
    }

    public int getGreenDuration() {
        return greenDuration;
    }

    public void setGreenDuration(int greenDuration) {
        this.greenDuration = greenDuration;
    }

    public int getYellowDuration() {
        return yellowDuration;
    }

    public void setYellowDuration(int yellowDuration) {
        this.yellowDuration = yellowDuration;
    }

    public int getRedDuration() {
        return redDuration;
    }

    public void setRedDuration(int redDuration) {
        this.redDuration = redDuration;
    }

    /**
     * Trả về số giây để hiển thị, tính theo {@link CountdownMode} của đèn.
     * GUI nên dùng phương thức này thay vì {@link #getSecondsLeft()} trực
     * tiếp để biết khi nào nên hiển thị số.
     *
     * @return số giây còn lại nếu đèn cần hiển thị, hoặc -1 nếu không hiển
     *         thị số giây ở thời điểm hiện tại.
     */
    public int getDisplaySeconds() {
        switch (countdownMode) {
            case NONE:
                return -1;
            case LAST_TEN:
                return secondsLeft <= 10 ? secondsLeft : -1;
            case FULL:
            default:
                return secondsLeft;
        }
    }

    @Override
    public String toString() {
        return "TrafficLight{" +
                "id='" + id + '\'' +
                ", state=" + state +
                ", secondsLeft=" + secondsLeft +
                ", autoMode=" + autoMode +
                '}';
    }
}
