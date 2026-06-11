package com.smartcity.traffic.driver;

import com.smartcity.traffic.light.TrafficLight;
import com.smartcity.traffic.road.Lane;
import com.smartcity.traffic.road.Road;
import com.smartcity.traffic.vehicle.Vehicle;

/**
 * Xe ưu tiên (Emergency) - Cứu thương, cảnh sát, cứu hỏa.
 * Có đặc quyền cao nhất: vượt tất cả đèn đỏ, chuyển làn không hạn chế.
 * 
 * Đặc điểm:
 * - Tốc độ tối đa: 120 km/h (cao nhất)
 * - Khoảng cách an toàn: 10 mét
 * - Gia tốc: 6.0 m/s² (cao nhất)
 * - Phanh: 10.0 m/s² (mạnh nhất)
 * - Vượt tất cả đèn đỏ (không bao giờ dừng)
 * - Luôn cố gắng đi vào làn trái cùng (Fast Lane)
 * - Phát siren cảnh báo
 * - Có thể ép xe khác nhường đường
 * 
 * @author Smart City Traffic Simulation Team
 * @version 1.0
 */
public class EmergencyDriver extends BaseDriver {

    /** Gia tốc của xe ưu tiên (m/s²) */
    private static final double ACCELERATION = 6.0;
    
    /** Lực phanh của xe ưu tiên (m/s²) */
    private static final double BRAKE_FORCE = 10.0;

    /** Tốc độ an toàn khi vượt giao lộ (km/h) */
    private static final double INTERSECTION_SAFE_SPEED = 40.0;
    
    /** Khoảng cách phát tín hiệu cảnh báo tới xe khác (mét) */
    private static final double SIREN_WARNING_DISTANCE = 50.0;

    /** Khoảng trống an toàn phía trước khi chuyển làn (mét) */
    private static final double SAFE_FRONT = 10.0; 
    
    /** Khoảng trống an toàn phía sau khi chuyển làn (mét) */
    private static final double SAFE_REAR = 5.0;
    
    /** Cờ kiểm tra xem siren có bật không */
    private boolean sirenOn = true;
    
    /** Loại xe ưu tiên: "AMBULANCE", "FIRETRUCK", "POLICE" */
    private String emergencyType;

    /**
     * Khởi tạo xe ưu tiên với loại xe xác định.
     * 
     * @param emergencyType Loại xe ưu tiên ("AMBULANCE", "FIRETRUCK", "POLICE")
     */
    public EmergencyDriver(String emergencyType) {
        super(120.0, 10.0);
        this.emergencyType = emergencyType;
    }

    /**
     * Xử lý đèn giao thông - VƯỢT TẤT CẢ ĐÈN ĐỎ.
     * Ghi đè hoàn toàn phương thức của BaseDriver.
     * Xe ưu tiên không bao giờ bị chặn bởi đèn đỏ.
     * 
     * Hành vi:
     * - Phát siren cảnh báo
     * - Nếu tốc độ quá cao tại giao lộ, giảm xuống để an toàn
     * - Luôn trả về false (không bị chặn)
     * 
     * @param vehicle Xe cần điều khiển
     * @param trafficLight Đèn giao thông
     * @param deltaTime Khoảng thời gian giữa các frame
     * @return false (không bao giờ bị chặn bởi đèn)
     */
    @Override
    protected boolean handleTrafficLight(Vehicle vehicle, TrafficLight trafficLight, double deltaTime) {
        if (sirenOn) {
            vehicle.playSiren();
        }
        
        if (trafficLight != null && trafficLight.isRed() && isNearIntersection(vehicle)) {
            System.out.println("[Emergency] " + vehicle.getVehicleId() + " VƯỢT ĐÈN ĐỎ với còi hụ!");
            
            // Giảm tốc độ an toàn khi vượt giao lộ nếu đang chạy quá nhanh
            if (vehicle.getCurrentSpeed() > INTERSECTION_SAFE_SPEED) {
                vehicle.setCurrentSpeed(vehicle.getCurrentSpeed() - (BRAKE_FORCE * deltaTime));
            }
        }
        
        vehicle.setStopped(false);
        return false; // Không bao giờ bị chặn bởi đèn đỏ
    }

    /**
     * Xử lý va chạm - Tránh va chạm nhưng có thể ép xe khác nhường đường.
     * Phát tín hiệu cảnh báo khi khoảng cách < 50m.
     * Phanh an toàn khi khoảng cách quá gần.
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
            
            // Phát tín hiệu ép xe phía trước nhường đường nếu khoảng cách < 50m
            if (distanceToFront < SIREN_WARNING_DISTANCE) {
                // Yêu cầu Vehicle cung cấp phương thức này để xe khác biết nhường đường
                // frontVehicle.setYieldToEmergency(true); 
            }

            // Phanh an toàn nếu quá gần
            if (distanceToFront < stoppingDistance * 0.8) {
                double newSpeed = Math.max(0, vehicle.getCurrentSpeed() - (BRAKE_FORCE * 1.5 * deltaTime * 3.6));
                vehicle.setCurrentSpeed(newSpeed);
                vehicle.setBraking(true);
            }

            else {
                vehicle.setBraking(false);
            }
        }
    }

    /**
     * Điều chỉnh tốc độ - PHÓNG HẾT GA, không quan tâm giới hạn làn.
     * Luôn cố gắng đạt tốc độ tối đa (120 km/h).
     * 
     * @param vehicle Xe cần điều khiển
     * @param currentLane Làn hiện tại
     * @param deltaTime Khoảng thời gian giữa các frame
     */
    @Override
    public void handleSpeedControl(Vehicle vehicle, Lane currentLane, double deltaTime) {
        double targetSpeed = this.maxSpeed;  // Luôn cố gắng đạt tốc độ tối đa
        
        // Cập nhật tốc độ theo gia tốc/phanh vật lý
        double currentSpeed = vehicle.getCurrentSpeed();
        if (currentSpeed < targetSpeed - 1) {
            vehicle.setCurrentSpeed(Math.min(targetSpeed, currentSpeed + (ACCELERATION * deltaTime * 3.6)));
        } else if (currentSpeed > targetSpeed + 1) {
            vehicle.setCurrentSpeed(Math.max(targetSpeed, currentSpeed - (BRAKE_FORCE * deltaTime * 3.6)));
        }
        
        // Hủy trạng thái phanh khi đạt tốc độ mục tiêu
        if (vehicle.getCurrentSpeed() >= targetSpeed - 1) {
            vehicle.setBraking(false);
        }
    }

    /**
     * Xử lý chuyển làn - TÌM ĐƯỜNG NHANH NHẤT.
     * Luôn cố gắng tiến về làn trái cùng (Fast Lane) để di chuyển nhanh nhất.
     * Vùng an toàn nhỏ vì có đặc quyền cao.
     * 
     * @param vehicle Xe cần điều khiển
     * @param currentLane Làn hiện tại
     * @param deltaTime Khoảng thời gian giữa các frame
     */
    @Override
    public void handleLaneChange(Vehicle vehicle, Lane currentLane, double deltaTime) {
        double currentCooldown = vehicle.getLaneChangeCooldown();
        if (currentCooldown > 0) {
            vehicle.setLaneChangeCooldown(Math.max(0, currentCooldown - deltaTime));
            return;
        }

        // Luôn cố gắng bám làn trái cùng (Fast Lane) để di chuyển nhanh nhất
        Road road = currentLane.getRoad();
        if (road != null && !road.getLanes().isEmpty()) {
            Lane fastLane = road.getLanes().get(0);
            
            if (currentLane != fastLane) {
                // Cố gắng lách sang trái để tiến về Fast Lane
                Lane leftLane = getLeftLane(currentLane);
                if (leftLane != null && isLaneChangeSafe(vehicle, leftLane)) {
                    vehicle.changeLane(leftLane);
                    vehicle.setLaneChangeCooldown(1.0);
                    System.out.println("[Emergency] " + vehicle.getVehicleId() + " đang lách ra làn ngoài cùng!");
                }
            }
        }
    }

    /**
     * Kiểm tra xem chuyển làn có an toàn hay không (ưu tiên).
     * Vùng an toàn nhỏ hơn: 10m trước, 5m sau.
     * 
     * @param vehicle Xe muốn chuyển làn
     * @param targetLane Làn mục tiêu
     * @return true nếu khoảng trống đủ, false nếu có xe cản
     */
    private boolean isLaneChangeSafe(Vehicle vehicle, Lane targetLane) { 

        for (Vehicle other : targetLane.getVehicles()) {
            double relativePos = other.getPosition() - vehicle.getPosition();
            if (relativePos > -SAFE_REAR && relativePos < SAFE_FRONT) {
                return false; 
            }
        }
        return true;
    }

    /**
     * Lấy làn bên trái của làn hiện tại.
     * 
     * @param currentLane Làn hiện tại
     * @return Làn bên trái, hoặc null nếu không có hoặc lỗi
     */
    private Lane getLeftLane(Lane currentLane) {
        Road road = currentLane.getRoad();
        if (road == null) {
            return null;
        }

        int currentIndex = road.getLanes().indexOf(currentLane);
        if (currentIndex > 0) {
            return road.getLanes().get(currentIndex - 1);
        }
        return null;
    }

    /**
     * Trả về tên chiến lược lái xe với loại xe ưu tiên.
     * 
     * @return Ví dụ: "EMERGENCY (AMBULANCE)", "EMERGENCY (FIRETRUCK)"
     */
    @Override
    public String getStrategyName() {
        return DriverType.EMERGENCY.toString() + " (" + emergencyType + ")";
    }
}
