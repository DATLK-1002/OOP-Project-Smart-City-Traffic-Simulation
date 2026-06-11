package com.smartcity.traffic.driver;

import com.smartcity.traffic.road.Lane;
import com.smartcity.traffic.road.Road;
import com.smartcity.traffic.vehicle.Vehicle;

/**
 * Tài xế bình thường - tuân thủ luật lệ giao thông, lái xe an toàn.
 * 
 * Đặc điểm:
 * - Tốc độ tối đa: 60 km/h
 * - Khoảng cách an toàn: 15 mét
 * - Phanh trung bình (BRAKE_FORCE: 5.0 m/s²)
 * - Chuyển làn có kế hoạch, cần khoảng trống lớn
 * - Tôn trọng tốc độ giới hạn của làn
 * 
 * @author Smart City Traffic Simulation Team
 * @version 1.0
 */
public class NormalDriver extends BaseDriver {
    
    /** Gia tốc của tài xế bình thường (m/s²) */
    private static final double ACCELERATION = 2.5; 
    
    /** Lực phanh của tài xế bình thường (m/s²) */
    private static final double BRAKE_FORCE = 5.0;  
    
    /** Thời gian chờ tối thiểu giữa các lần chuyển làn (giây) */
    private static final double LANE_CHANGE_COOLDOWN = 3.0;
    
    /** Chênh lệch tốc độ tối thiểu để vượt xe phía trước (km/h) */
    private static final double SPEED_DIFF_TO_OVERTAKE = 10.0;
    
    /** Khoảng trống an toàn phía trước khi chuyển làn (mét) */
    private static final double SAFE_FRONT = 20.0;
    
    /** Khoảng trống an toàn phía sau khi chuyển làn (mét) */
    private static final double SAFE_REAR = 10.0;

    /** Khoảng cách tối thiểu để bắt đầu xem xét vượt xe phía trước (mét) */
    private static final double OVERTAKE_TRIGGER_DISTANCE = 40.0;

    /**
     * Khởi tạo tài xế bình thường với tham số mặc định.
     */
    public NormalDriver() {
        super(60.0, 15.0);
    }

    /**
     * Xử lý va chạm - Phanh mạnh khi xe phía trước quá gần.
     * Có 2 mức độ phanh:
     * 1. Khi khoảng cách < khoảng cách dừng: Phanh mạnh (1.5x)
     * 2. Khi khoảng cách < khoảng cách an toàn: Phanh vừa phải (1x)
     * 
     * @param vehicle Xe cần điều khiển
     * @param currentLane Làn hiện tại
     * @param deltaTime Khoảng thời gian giữa các frame
     */
    @Override
    public void handleCollisionAvoidance(Vehicle vehicle, Lane currentLane, double deltaTime) {
        Vehicle frontVehicle = getFrontVehicle(vehicle, currentLane);
        
        if (frontVehicle != null) {
            double distanceToFront = calculateDistance(vehicle, frontVehicle);
            double stoppingDistance = calculateStoppingDistance(vehicle);
            
            // Phanh mạnh nếu khoảng cách quá ngắn
            if (distanceToFront < stoppingDistance) {
                double newSpeed = Math.max(0, vehicle.getCurrentSpeed() - (BRAKE_FORCE * 1.5 * deltaTime * 3.6));
                vehicle.setCurrentSpeed(newSpeed);
                vehicle.setBraking(true);
            } 
            // Phanh nhẹ nếu gần khoảng cách an toàn
            else if (distanceToFront < safeDistance) {
                double newSpeed = Math.max(0, vehicle.getCurrentSpeed() - (BRAKE_FORCE * deltaTime * 3.6));
                vehicle.setCurrentSpeed(newSpeed);
                vehicle.setBraking(true);
            }
        }
    }

    /**
     * Điều chỉnh tốc độ dựa trên giới hạn làn và tốc độ xe phía trước.
     * Luôn cố gắng đạt tốc độ giới hạn của làn hoặc tốc độ tối đa (nếu thấp hơn).
     * 
     * @param vehicle Xe cần điều khiển
     * @param currentLane Làn hiện tại
     * @param deltaTime Khoảng thời gian giữa các frame
     */
    @Override
    public void handleSpeedControl(Vehicle vehicle, Lane currentLane, double deltaTime) {
        double targetSpeed = Math.min(this.maxSpeed, currentLane.getSpeedLimit());
        
        // Nếu có xe phía trước quá gần, giảm tốc độ xuống bằng tốc độ của xe phía trước
        Vehicle frontVehicle = getFrontVehicle(vehicle, currentLane);
        if (frontVehicle != null && calculateDistance(vehicle, frontVehicle) < safeDistance) {
            targetSpeed = Math.min(targetSpeed, frontVehicle.getCurrentSpeed());
        }
        
        // Cập nhật tốc độ theo gia tốc/phanh mềm
        double currentSpeed = vehicle.getCurrentSpeed();
        if (currentSpeed < targetSpeed - 1) {
            vehicle.setCurrentSpeed(Math.min(targetSpeed, currentSpeed + (ACCELERATION * deltaTime * 3.6)));
        } else if (currentSpeed > targetSpeed + 1) {
            vehicle.setCurrentSpeed(Math.max(targetSpeed, currentSpeed - (BRAKE_FORCE * deltaTime * 3.6)));
        }
    }

    /**
     * Xử lý chuyển làn - chỉ vượt khi cần thiết và an toàn.
     * Ưu tiên lách sang trái (Fast Lane), nếu không được thì lách sang phải.
     * Kiểm tra khoảng cách và tốc độ của các xe xung quanh trước khi chuyển.
     * 
     * @param vehicle Xe cần điều khiển
     * @param currentLane Làn hiện tại
     * @param deltaTime Khoảng thời gian giữa các frame
     */
    @Override
    public void handleLaneChange(Vehicle vehicle, Lane currentLane, double deltaTime) {
        // Kiểm tra thời gian chờ giữa các lần chuyển làn
        double currentCooldown = vehicle.getLaneChangeCooldown();
        if (currentCooldown > 0) {
            vehicle.setLaneChangeCooldown(Math.max(0, currentCooldown - deltaTime));
            return; 
        }

        // Kiểm tra nếu có xe phía trước chậm hơn
        Vehicle frontVehicle = getFrontVehicle(vehicle, currentLane);
        if (frontVehicle != null) {
            double distanceToFront = calculateDistance(vehicle, frontVehicle);
            if (distanceToFront < OVERTAKE_TRIGGER_DISTANCE &&
                frontVehicle.getCurrentSpeed() < vehicle.getCurrentSpeed() - SPEED_DIFF_TO_OVERTAKE) {
                
                Road road = currentLane.getRoad();
                if (road != null) {
                    int currentIndex = road.getLanes().indexOf(currentLane);
                    boolean changedLane = false;

                    // Ưu tiên lách sang trái
                    if (currentIndex > 0) {
                        Lane leftLane = road.getLanes().get(currentIndex - 1);
                        if (isLaneChangeSafe(vehicle, leftLane)) {
                            vehicle.changeLane(leftLane);
                            vehicle.setLaneChangeCooldown(LANE_CHANGE_COOLDOWN); 
                            System.out.println("[Normal] " + vehicle.getVehicleId() + " xin nhan lách sang trái an toàn!");
                            changedLane = true;
                        }
                    }

                    // Nếu lách trái không được, thử lách sang phải
                    if (!changedLane && currentIndex < road.getLanes().size() - 1) {
                        Lane rightLane = road.getLanes().get(currentIndex + 1);
                        if (isLaneChangeSafe(vehicle, rightLane)) {
                            vehicle.changeLane(rightLane);
                            vehicle.setLaneChangeCooldown(LANE_CHANGE_COOLDOWN);
                            System.out.println("[Normal] " + vehicle.getVehicleId() + " xin nhan lách sang phải an toàn!");
                        }
                    }
                }
            }
        }
    }

    /**
     * Kiểm tra xem chuyển làn có an toàn hay không.
     * Kiểm tra:
     * 1. Có xe khác trong vùng nguy hiểm phía trước/sau không?
     * 2. Có xe nào từ phía sau đang chạy nhanh hơn nhiều không?
     * 
     * @param vehicle Xe muốn chuyển làn
     * @param targetLane Làn mục tiêu
     * @return true nếu an toàn, false nếu có xe cản đường
     */
    private boolean isLaneChangeSafe(Vehicle vehicle, Lane targetLane) {
        for (Vehicle other : targetLane.getVehicles()) {
            double relativePos = other.getPosition() - vehicle.getPosition();

            // Kiểm tra xe phía trước/sau trong vùng nguy hiểm
            if (relativePos > -SAFE_REAR && relativePos < SAFE_FRONT) {
                return false; 
            }
            
            // Kiểm tra xe từ phía sau đang chạy nhanh quá
            if (relativePos < 0 && other.getCurrentSpeed() > vehicle.getCurrentSpeed() + 20) {
                return false; 
            }
        }
        return true;
    }

    /**
     * Trả về tên chiến lược lái xe.
     * 
     * @return "NORMAL"
     */
    @Override
    public String getStrategyName() {
        return DriverType.NORMAL.toString();
    }
}