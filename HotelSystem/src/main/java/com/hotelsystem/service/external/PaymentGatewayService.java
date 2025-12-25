package com.hotelsystem.service.external;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付网关服务（模拟实现）
 * 预留接口，不真实调用支付网关API
 */
@Service
@Slf4j
public class PaymentGatewayService {

    /**
     * 创建支付订单
     * @param transactionId 交易ID
     * @param amount 金额
     * @param description 描述
     * @return 支付订单信息
     */
    public Map<String, Object> createPaymentOrder(Long transactionId, BigDecimal amount, String description) {
        log.info("模拟创建支付订单: transactionId={}, amount={}, description={}", 
                transactionId, amount, description);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("paymentOrderId", "PAY_" + System.currentTimeMillis());
        result.put("paymentUrl", "/payment/simulate?orderId=" + transactionId);
        result.put("qrCode", "模拟二维码数据");
        result.put("message", "支付订单已创建（模拟）");
        
        return result;
    }

    /**
     * 查询支付状态
     * @param paymentOrderId 支付订单ID
     * @return 支付状态
     */
    public Map<String, Object> queryPaymentStatus(String paymentOrderId) {
        log.info("模拟查询支付状态: {}", paymentOrderId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("status", "SUCCESS"); // PENDING, SUCCESS, FAILED
        result.put("message", "支付状态查询完成（模拟）");
        
        return result;
    }

    /**
     * 创建退款订单
     * @param transactionId 交易ID
     * @param amount 退款金额
     * @param reason 退款原因
     * @return 退款订单信息
     */
    public Map<String, Object> createRefundOrder(Long transactionId, BigDecimal amount, String reason) {
        log.info("模拟创建退款订单: transactionId={}, amount={}, reason={}", 
                transactionId, amount, reason);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("refundOrderId", "REFUND_" + System.currentTimeMillis());
        result.put("message", "退款订单已创建（模拟）");
        
        return result;
    }

    /**
     * 查询退款状态
     * @param refundOrderId 退款订单ID
     * @return 退款状态
     */
    public Map<String, Object> queryRefundStatus(String refundOrderId) {
        log.info("模拟查询退款状态: {}", refundOrderId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("status", "SUCCESS"); // PENDING, SUCCESS, FAILED
        result.put("message", "退款状态查询完成（模拟）");
        
        return result;
    }

    /**
     * 处理支付回调
     * @param callbackData 回调数据
     * @return 处理结果
     */
    public Map<String, Object> handlePaymentCallback(Map<String, Object> callbackData) {
        log.info("模拟处理支付回调: {}", callbackData);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "支付回调处理完成（模拟）");
        
        return result;
    }

    /**
     * 处理退款回调
     * @param callbackData 回调数据
     * @return 处理结果
     */
    public Map<String, Object> handleRefundCallback(Map<String, Object> callbackData) {
        log.info("模拟处理退款回调: {}", callbackData);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "退款回调处理完成（模拟）");
        
        return result;
    }
}

