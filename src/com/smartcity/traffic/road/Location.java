package com.smartcity.traffic.road;

/**
 * Lớp Location quản lý tọa độ (x, y) và tỷ lệ hiển thị (scale) của các đối tượng trên bản đồ.
 * Hỗ trợ tính toán zoom in (ngã rẽ) và zoom out (đường rộng).
 */
public class Location {
    private double x;
    private double y;
    private double scale;  // Tỷ lệ hiển thị: 1.0 = bình thường, > 1.0 = zoom in, < 1.0 = zoom out

    /**
     * Khởi tạo Location với tọa độ và tỷ lệ mặc định.
     *
     * @param x tọa độ x
     * @param y tọa độ y
     */
    public Location(double x, double y) {
        this.x = x;
        this.y = y;
        this.scale = 1.0;
    }

    /**
     * Khởi tạo Location với tọa độ và tỷ lệ tùy chỉnh.
     *
     * @param x     tọa độ x
     * @param y     tọa độ y
     * @param scale tỷ lệ hiển thị
     */
    public Location(double x, double y, double scale) {
        this.x = x;
        this.y = y;
        this.scale = scale;
    }

    /**
     * Tính khoảng cách Euclidean giữa hai vị trí.
     *
     * @param other vị trí khác
     * @return khoảng cách
     */
    public double distanceTo(Location other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Cập nhật tọa độ.
     *
     * @param x tọa độ x mới
     * @param y tọa độ y mới
     */
    public void updatePosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Cập nhật tỷ lệ hiển thị.
     *
     * @param scale tỷ lệ mới
     */
    public void setScale(double scale) {
        this.scale = Math.max(0.1, scale);  // Đảm bảo scale không quá nhỏ
    }

    /**
     * Lấy tọa độ x.
     *
     * @return tọa độ x
     */
    public double getX() {
        return x;
    }

    /**
     * Lấy tọa độ y.
     *
     * @return tọa độ y
     */
    public double getY() {
        return y;
    }

    /**
     * Lấy tỷ lệ hiển thị.
     *
     * @return tỷ lệ
     */
    public double getScale() {
        return scale;
    }

    /**
     * Trả về tọa độ đã được scale.
     *
     * @return mảng [x_scaled, y_scaled]
     */
    public double[] getScaledCoordinates() {
        return new double[]{x * scale, y * scale};
    }

    @Override
    public String toString() {
        return String.format("Location(x=%.2f, y=%.2f, scale=%.2f)", x, y, scale);
    }
}
