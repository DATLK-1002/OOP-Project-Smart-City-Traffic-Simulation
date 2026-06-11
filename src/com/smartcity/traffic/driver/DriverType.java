package com.smartcity.traffic.driver;

/**
 * Enum định nghĩa các loại tài xế khác nhau trong hệ thống giao thông.
 * 
 * Các loại tài xế:
 * - NORMAL: Tài xế bình thường, tuân thủ luật lệ giao thông
 * - AGGRESSIVE: Tài xế hung hăng, liều lĩnh, vượt đèn vàng, chuyển làn liên tục
 * - EMERGENCY: Xe ưu tiên (cứu thương, cảnh sát), vượt tất cả đèn đỏ
 * 
 * @author Smart City Traffic Simulation Team
 * @version 1.0
 */
public enum DriverType {
    /** Tài xế bình thường tuân thủ luật giao thông */
    NORMAL,
    
    /** Tài xế hung hăng, liều lĩnh, chấp nhận rủi ro */
    AGGRESSIVE,
    
    /** Xe ưu tiên (cứu thương, cảnh sát) có đặc quyền */
    EMERGENCY
}
