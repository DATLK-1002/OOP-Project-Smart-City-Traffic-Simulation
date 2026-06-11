package com.smartcity.traffic.driver;

import com.smartcity.traffic.vehicle.Vehicle;
import com.smartcity.traffic.road.Lane;
import com.smartcity.traffic.light.TrafficLight;

/**
 * Interface định nghĩa chiến lược lái xe cho các loại tài xế khác nhau trong hệ thống giao thông.
 * Sử dụng Strategy Pattern để cho phép các hành vi lái xe khác nhau được thay đổi động.
 * 
 * @author Smart City Traffic Simulation Team
 * @version 1.0
 */
public interface DrivingStrategy {
    /**
     * Thực hiện chu trình lái xe chính, bao gồm xử lý đèn, tránh va chạm, điều chỉnh tốc độ và chuyển làn.
     * 
     * @param vehicle Xe cần điều khiển
     * @param currentLane Làn hiện tại của xe
     * @param trafficLight Đèn giao thông tại giao lộ
     * @param deltaTime Khoảng thời gian giữa các frame (giây)
     */
    void drive(Vehicle vehicle, Lane currentLane, TrafficLight trafficLight, double deltaTime);
    
    /**
     * Xử lý tránh va chạm với xe phía trước.
     * 
     * @param vehicle Xe cần điều khiển
     * @param currentLane Làn hiện tại
     * @param deltaTime Khoảng thời gian giữa các frame (giây)
     */
    void handleCollisionAvoidance(Vehicle vehicle, Lane currentLane, double deltaTime);
    
    /**
     * Xử lý chuyển làn dựa trên các điều kiện giao thông.
     * 
     * @param vehicle Xe cần điều khiển
     * @param currentLane Làn hiện tại
     * @param deltaTime Khoảng thời gian giữa các frame (giây)
     */
    void handleLaneChange(Vehicle vehicle, Lane currentLane, double deltaTime);
    
    /**
     * Điều chỉnh tốc độ dựa trên tốc độ giới hạn làn và tình huống giao thông.
     * 
     * @param vehicle Xe cần điều khiển
     * @param currentLane Làn hiện tại
     * @param deltaTime Khoảng thời gian giữa các frame (giây)
     */
    void handleSpeedControl(Vehicle vehicle, Lane currentLane, double deltaTime);
    
    /**
     * Trả về tên chiến lược lái xe.
     * 
     * @return Tên chiến lược (VD: "NORMAL", "AGGRESSIVE", "EMERGENCY")
     */
    String getStrategyName();
}