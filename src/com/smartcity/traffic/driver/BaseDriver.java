package com.smartcity.traffic.driver;

import com.smartcity.traffic.vehicle.Vehicle;
import com.smartcity.traffic.road.Lane;
import com.smartcity.traffic.light.TrafficLight;

/**
 * Lớp cơ sở trừu tượng cho tất cả các loại tài xế.
 * Định nghĩa các phương thức chung và logic cốt lõi cho hoạt động lái xe.
 * Sử dụng Template Method Pattern để cho phép các lớp con ghi đè các hành vi cụ thể.
 * 
 * Trách nhiệm chính:
 * 1. Quản lý tốc độ tối đa và khoảng cách an toàn
 * 2. Xử lý đèn giao thông
 * 3. Cính toán khoảng cách và vị trí
 * 4. Cập nhật vị trí xe
 * 
 * @author Smart City Traffic Simulation Team
 * @version 1.0
 */
public abstract class BaseDriver implements DrivingStrategy {
    /** Tốc độ tối đa cho phép cho loại tài xế này (km/h) */
    protected double maxSpeed;
    
    /** Khoảng cách an toàn tối thiểu với xe phía trước (mét) */
    protected double safeDistance;

    /**
     * Khởi tạo với tốc độ mặc định là 60 km/h và khoảng cách an toàn 15m.
     */
    public BaseDriver() {
        this.maxSpeed = 60.0;
        this.safeDistance = 15.0;
    }

    /**
     * Khởi tạo với tốc độ và khoảng cách an toàn tùy chỉnh.
     * 
     * @param maxSpeed Tốc độ tối đa (km/h)
     * @param safeDistance Khoảng cách an toàn (mét)
     */
    public BaseDriver(double maxSpeed, double safeDistance) {
        this.maxSpeed = maxSpeed;
        this.safeDistance = safeDistance;
    }

    /**
     * Chu trình lái xe chính (Template Method Pattern).
     * Định nghĩa thứ tự thực hiện các bước lái xe:
     * 1. Xử lý đèn giao thông
     * 2. Tránh va chạm
     * 3. Điều chỉnh tốc độ
     * 4. Chuyển làn
     * 5. Cập nhật vị trí
     * 
     * @param vehicle Xe cần điều khiển
     * @param currentLane Làn hiện tại
     * @param trafficLight Đèn giao thông
     * @param deltaTime Khoảng thời gian giữa các frame (giây)
     */
    @Override
    public final void drive(Vehicle vehicle, Lane currentLane, TrafficLight trafficLight, double deltaTime) {
        if (vehicle == null || currentLane == null) return;

        vehicle.setBraking(false);
        boolean blockedByLight = handleTrafficLight(vehicle, trafficLight, deltaTime);

        if (!blockedByLight) {
            handleCollisionAvoidance(vehicle, currentLane, deltaTime);
            handleSpeedControl(vehicle, currentLane, deltaTime);
            handleLaneChange(vehicle, currentLane, deltaTime);
        }

        updatePosition(vehicle, deltaTime);
    }

    /**
     * Xử lý tránh va chạm - được ghi đè bởi các lớp con.
     * 
     * @param vehicle Xe cần điều khiển
     * @param currentLane Làn hiện tại
     * @param deltaTime Khoảng thời gian giữa các frame
     */
    public abstract void handleCollisionAvoidance(Vehicle vehicle, Lane currentLane, double deltaTime);
    
    /**
     * Điều chỉnh tốc độ - được ghi đè bởi các lớp con.
     * 
     * @param vehicle Xe cần điều khiển
     * @param currentLane Làn hiện tại
     * @param deltaTime Khoảng thời gian giữa các frame
     */
    public abstract void handleSpeedControl(Vehicle vehicle, Lane currentLane, double deltaTime);
    
    /**
     * Xử lý chuyển làn - được ghi đè bởi các lớp con.
     * 
     * @param vehicle Xe cần điều khiển
     * @param currentLane Làn hiện tại
     * @param deltaTime Khoảng thời gian giữa các frame
     */
    public abstract void handleLaneChange(Vehicle vehicle, Lane currentLane, double deltaTime);

    /**
     * Xử lý đèn giao thông - giảm tốc độ và dừng khi đèn đỏ.
     * Có thể được ghi đè để có hành vi khác (VD: vượt đèn vàng).
     * 
     * @param vehicle Xe cần điều khiển
     * @param trafficLight Đèn giao thông
     * @param deltaTime Khoảng thời gian giữa các frame
     * @return true nếu xe bị chặn bởi đèn đỏ, false nếu có thể tiếp tục
     */
    protected boolean handleTrafficLight(Vehicle vehicle, TrafficLight trafficLight, double deltaTime) {
        double SPEED_STEP = 2.0; 
        if (trafficLight != null && trafficLight.isRed() && isNearIntersection(vehicle)) {
            if (vehicle.getCurrentSpeed() > 20.0) {
                vehicle.setCurrentSpeed(vehicle.getCurrentSpeed() - SPEED_STEP * 2);
            } else {
                vehicle.setCurrentSpeed(0);
                vehicle.setStopped(true);
            }
            return true;
        } else {
            vehicle.setStopped(false);
            return false;
        }
    }

    /**
     * Tìm xe phía trước trong làn hiện tại.
     * 
     * @param vehicle Xe tham chiếu
     * @param lane Làn cần tìm
     * @return Xe phía trước gần nhất, hoặc null nếu không có
     */
    protected Vehicle getFrontVehicle(Vehicle vehicle, Lane lane) {

        Vehicle closest = null;
        double minDistance = Double.MAX_VALUE;

        for (Vehicle other : lane.getVehicles()) {

            if (other == vehicle) {
                continue;
            }

            double distance =
                    other.getPosition()
                    - vehicle.getPosition();

            if (distance > 0 && distance < minDistance) {

                minDistance = distance;
                closest = other;
            }
        }

        return closest;
    }

    /**
     * Tính khoảng cách giữa hai xe.
     * 
     * @param v1 Xe thứ nhất
     * @param v2 Xe thứ hai
     * @return Khoảng cách tuyệt đối giữa hai xe (mét)
     */
    protected double calculateDistance(Vehicle v1, Vehicle v2) {
        return Math.abs(v1.getPosition() - v2.getPosition());
    }

    /**
     * Tính khoảng cách dừng của xe dựa trên vận tốc hiện tại.
     * Sử dụng công thức vật lý: s = v²/(2a)
     * 
     * @param vehicle Xe cần tính
     * @return Khoảng cách dừng (mét)
     */
    protected double calculateStoppingDistance(Vehicle vehicle) {
        double speedMps = vehicle.getCurrentSpeed() / 3.6;
        double deceleration = 7.0;
        return (speedMps * speedMps) / (2 * deceleration);
    }

    /**
     * Kiểm tra xe có gần giao lộ hay không.
     * Giao lộ được coi là gần khi khoảng cách < 30 mét.
     * 
     * @param vehicle Xe cần kiểm tra
     * @return true nếu gần giao lộ, false nếu xa
     */
    protected boolean isNearIntersection(Vehicle vehicle) {
        return vehicle.getDistanceToNextIntersection() < 30;
    }

    /**
     * Cập nhật vị trí xe dựa trên vận tốc hiện tại.
     * Tính toán: vị trí mới = vị trí cũ + (vận tốc * thời gian)
     * 
     * @param vehicle Xe cần cập nhật
     * @param deltaTime Khoảng thời gian giữa các frame (giây)
     */
    protected void updatePosition(Vehicle vehicle, double deltaTime) {
        double speedMps = vehicle.getCurrentSpeed() / 3.6;
        double newPosition = vehicle.getPosition() + (speedMps * deltaTime);
        vehicle.setPosition(newPosition);
        
        double newDistance = vehicle.getDistanceToNextIntersection() - (speedMps * deltaTime);
        vehicle.setDistanceToNextIntersection(newDistance);
    }
}
