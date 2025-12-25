package com.hotelsystem.service.external;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 公安部门接口服务（模拟实现）
 * 预留接口，不真实调用公安部门API
 */
@Service
@Slf4j
public class PublicSecurityService {

    /**
     * 验证身份证信息
     * @param idCardNumber 身份证号
     * @param name 姓名
     * @return 验证结果
     */
    public Map<String, Object> verifyIdCard(String idCardNumber, String name) {
        log.info("模拟验证身份证: {} - {}", idCardNumber, name);
        
        // 模拟验证逻辑：简单格式校验
        boolean isValid = idCardNumber != null && 
                         (idCardNumber.length() == 15 || idCardNumber.length() == 18);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", isValid);
        result.put("valid", isValid);
        result.put("message", isValid ? "身份证验证通过（模拟）" : "身份证格式不正确（模拟）");
        
        if (isValid) {
            result.put("name", name);
            result.put("idCardNumber", idCardNumber);
            // 模拟返回身份证信息
            result.put("gender", "MALE");
            result.put("birthDate", "1990-01-01");
            result.put("address", "模拟地址");
        }
        
        return result;
    }

    /**
     * 上报住宿登记信息
     * @param guestInfo 宾客信息
     * @return 上报结果
     */
    public Map<String, Object> reportGuestRegistration(Map<String, Object> guestInfo) {
        log.info("模拟上报住宿登记信息: {}", guestInfo);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("reportId", "PS_" + System.currentTimeMillis());
        result.put("message", "住宿登记信息已上报（模拟）");
        
        return result;
    }

    /**
     * 上报退房信息
     * @param guestInfo 宾客信息
     * @return 上报结果
     */
    public Map<String, Object> reportGuestCheckout(Map<String, Object> guestInfo) {
        log.info("模拟上报退房信息: {}", guestInfo);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "退房信息已上报（模拟）");
        
        return result;
    }

    /**
     * 查询宾客登记历史
     * @param idCardNumber 身份证号
     * @return 登记历史
     */
    public Map<String, Object> queryGuestHistory(String idCardNumber) {
        log.info("模拟查询宾客登记历史: {}", idCardNumber);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("history", java.util.List.of());
        result.put("message", "查询完成（模拟）");
        
        return result;
    }
}

