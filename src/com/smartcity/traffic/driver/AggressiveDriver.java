package com.smartcity.traffic.driver;

import com.smartcity.traffic.vehicle.Vehicle;
import com.smartcity.traffic.road.Lane;
import com.smartcity.traffic.road.Road;
import com.smartcity.traffic.light.TrafficLight;

/**
 * Tài xế hung hăng - liều lĩnh, vượt đèn vàng, chuyển làn liên tục, bấm còi.
 * 
 * Đặc điểm:
 * - Tốc độ tối đa: 90 km/h (cao hơn Normal)
 * - Khoảng cách an toàn: 8 mét (ngắn hơn Normal)
 * - Gia tốc: 4.0 m/s² (nhanh hơn Normal)
 * - Phanh muộn hơn, chỉ phanh khi khoảng cách < 70% stopping distance
 * - Vượt đèn vàng nếu đang chạy nhanh
 * - Chuyển làn hung hăng, vùng an toàn nhỏ hơn
 * 
 * @author Smart City Traffic Simulation Team
 * @version 1.0
 */
public class AggressiveDriver extends BaseDriver {

    /** Gia tốc của tài xế hung hăng (m/s²) */
    private static final double ACCELERATION = 4.0;
    
    /** Lực phanh của tài xế hung hăng (m/s²) */
    private static final double BRAKE_FORCE = 8.0;

    /**
     * Khởi tạo tài xế hung hăng với tham số mặc định.
     * Tốc độ tối đa: 90 km/h, Khoảng cách an toàn: 8 mét
     */
    public AggressiveDriver() {
        super(90.0, 8.0);
    }

    /**
     * Xử lý đèn giao thông - Vượt đèn vàng nếu chạy nhanh.
     * Ghi đè phương thức của BaseDriver để có hành vi đặc thù.
     * 
     * Hành vi:
     * - Đèn vàng + tốc độ > 40 km/h: Thốc ga vượt luôn
     * - Đèn đỏ: Phải phanh (theo logic của BaseDriver)
     * - Xanh: Tiếp tục lái bình thường
     * 
     * @param vehicle Xe cần điều khiển
     * @param trafficLight Đèn giao thông
     * @param deltaTime Khoảng thời gian giữa các frame
     * @return false (không bao giờ bị chặn bởi đèn vàng)
     */
    @Override
    protected boolean handleTrafficLight(Vehicle vehicle, TrafficLight trafficLight, double deltaTime) {
        if (trafficLight != null && isNearIntersection(vehicle)) {
            
            // Vượt đèn vàng nếu đang chạy nhanh
            if (trafficLight.isYellow() && vehicle.getCurrentSpeed() > 40.0) {
                System.out.println("[Aggressive] " + vehicle.getVehicleId() + " đang thốc ga vượt đèn vàng!");
                vehicle.setCurrentSpeed(Math.min(maxSpeed, vehicle.getCurrentSpeed() + (ACCELERATION * deltaTime * 3.6)));
                return false;
            }
            
            // Phải phanh ở đèn đỏ
            if (trafficLight.isRed()) {
                return super.handleTrafficLight(vehicle, trafficLight, deltaTime);
            }
        }
        vehicle.setStopped(false);
        return false;
    }

    /**
     * Xử lý va chạm - Phanh muộn hơn NormalDriver.
     * Chỉ bắt đầu phanh khi khoảng cách < 70% stopping distance.
     * Phanh mạnh (1.5x) khi quá gần.
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
            
            // Phanh muộn: chỉ phanh khi khoảng cách < 70% stopping distance
            if (distanceToFront < stoppingDistance * 0.7) {
                double newSpeed = Math.max(0, vehicle.getCurrentSpeed() - (BRAKE_FORCE * 1.5 * deltaTime * 3.6));
                vehicle.setCurrentSpeed(newSpeed);
                vehicle.setBraking(true);
                System.out.println("[Aggressive] " + vehicle.getVehicleId() + " phanh cháy lốp!");
            }
        }
    }

    /**
     * Điều chỉnh tốc độ - Chạy lố tốc độ giới hạn 15%.
     * Nếu bị cản đường, bấm còi và bám đuôi xe phía trước.
     * 
     * @param vehicle Xe cần điều khiển
     * @param currentLane Làn hiện tại
     * @param deltaTime Khoảng thời gian giữa các frame
     */
    @Override
    public void handleSpeedControl(Vehicle vehicle, Lane currentLane, double deltaTime) {
        double laneSpeedLimit = currentLane.getSpeedLimit();
        // Chạy lố tốc độ cho phép 15%
        double targetSpeed = Math.min(maxSpeed, laneSpeedLimit * 1.15); 
        
        Vehicle frontVehicle = getFrontVehicle(vehicle, currentLane);
        if (frontVehicle != null) {
            double distanceToFront = calculateDistance(vehicle, frontVehicle);
            
            // Nếu bị cản đường -> Bám đuôi (Tailgating) và Bấm còi
            if (distanceToFront < safeDistance * 1.5) {
                targetSpeed = Math.min(targetSpeed, frontVehicle.getCurrentSpeed() + 5);
                
                if (distanceToFront < safeDistance) {
                    // vehicle.honk();
                    System.out.println("[Aggressive] " + vehicle.getVehicleId() + " bấm còi: 'Tránh đường coi!!'");
                }
            }
        }
        
        // Cập nhật tốc độ theo Gia tốc vật lý
        double currentSpeed = vehicle.getCurrentSpeed();
        if (currentSpeed < targetSpeed - 1) {
            vehicle.setCurrentSpeed(Math.min(targetSpeed, currentSpeed + (ACCELERATION * deltaTime * 3.6)));
        } else if (currentSpeed > targetSpeed + 1) {
            vehicle.setCurrentSpeed(Math.max(targetSpeed, currentSpeed - (BRAKE_FORCE * deltaTime * 3.6)));
        }
        
    }

    /**
     * Xử lý chuyển làn - Lách sang trái và phải liên tục để vượt.
     * Vùng an toàn nhỏ hơn (5m trước, 2m sau), có thể thực hiện thao tác nguy hiểm.
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

        // Nếu có xe phía trước chậm hơn, vượt ngay lập tức
        Vehicle frontVehicle = getFrontVehicle(vehicle, currentLane);
        if (frontVehicle != null && frontVehicle.getCurrentSpeed() < vehicle.getCurrentSpeed() - 5) {
            
            // Ưu tiên lách trái
            Lane leftLane = getLeftLane(currentLane);
            if (leftLane != null && isLaneChangeSafe(vehicle, leftLane)) {
                vehicle.changeLane(leftLane);
                vehicle.setLaneChangeCooldown(1.5);
                System.out.println("[Aggressive] Đánh võng sang trái!");
                return;
            }
            
            // Lách trái không được thì lách phải
            Lane rightLane = getRightLane(currentLane);
            if (rightLane != null && isLaneChangeSafe(vehicle, rightLane)) {
                vehicle.changeLane(rightLane);
                vehicle.setLaneChangeCooldown(1.5);
                System.out.println("[Aggressive] Đánh võng sang phải!");
            }
        }
    }

    /**
     * Kiểm tra xem chuyển làn có an toàn hay không (hung hăng).
     * Vùng an toàn nhỏ hơn Normal: 5m trước, 2m sau.
     * 
     * @param vehicle Xe muốn chuyển làn
     * @param targetLane Làn mục tiêu
     * @return true nếu khoảng trống đủ, false nếu có xe cản
     */
    private boolean isLaneChangeSafe(Vehicle vehicle, Lane targetLane) {
        double SAFE_FRONT = 5.0;  // Nhỏ hơn Normal
        double SAFE_REAR = 2.0;   // Nhỏ hơn Normal

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
     * @return Làn bên trái, hoặc null nếu không có
     */
    private Lane getLeftLane(Lane currentLane) {
        Road road = currentLane.getRoad();
        int currentIndex = road.getLanes().indexOf(currentLane);
        if (currentIndex > 0) return road.getLanes().get(currentIndex - 1);
        return null;
    }

    /**
     * Lấy làn bên phải của làn hiện tại.
     * 
     * @param currentLane Làn hiện tại
     * @return Làn bên phải, hoặc null nếu không có
     */
    private Lane getRightLane(Lane currentLane) {
        Road road = currentLane.getRoad();
        int currentIndex = road.getLanes().indexOf(currentLane);
        if (currentIndex < road.getLanes().size() - 1) return road.getLanes().get(currentIndex + 1);
        return null;
    }

    /**
     * Trả về tên chiến lược lái xe.
     * 
     * @return "AGGRESSIVE"
     */
    @Override
    public String getStrategyName() {
        return DriverType.AGGRESSIVE.toString();
    }
}
