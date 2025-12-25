package com.hotelsystem.controller.external;

import com.hotelsystem.dto.ApiResponse;
import com.hotelsystem.service.external.OtaPlatformService;
import com.hotelsystem.service.external.PaymentGatewayService;
import com.hotelsystem.service.external.PublicSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 外部集成接口控制器
 * 提供OTA平台、公安部门、支付网关的接口调用入口（模拟实现）
 */
@RestController
@RequestMapping("/api/external")
@RequiredArgsConstructor
public class ExternalIntegrationController {

    private final OtaPlatformService otaPlatformService;
    private final PublicSecurityService publicSecurityService;
    private final PaymentGatewayService paymentGatewayService;

    // ========== OTA平台接口 ==========

    /**
     * 同步预订到OTA平台
     */
    @PostMapping("/ota/sync-reservation")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> syncReservationToOta(
            @RequestBody Map<String, Object> request) {
        try {
            // 这里应该从request中提取ReservationDto，简化处理
            Map<String, Object> result = otaPlatformService.syncReservationToOta(null);
            return ResponseEntity.ok(ApiResponse.success("同步成功", result));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("同步失败: " + e.getMessage()));
        }
    }

    /**
     * 更新OTA平台房间库存
     */
    @PostMapping("/ota/update-inventory")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateOtaInventory(
            @RequestBody Map<String, Object> request) {
        try {
            String roomType = (String) request.get("roomType");
            Integer availableCount = (Integer) request.get("availableCount");
            Map<String, Object> result = otaPlatformService.updateOtaInventory(roomType, availableCount);
            return ResponseEntity.ok(ApiResponse.success("更新成功", result));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("更新失败: " + e.getMessage()));
        }
    }

    // ========== 公安部门接口 ==========

    /**
     * 验证身份证
     */
    @PostMapping("/security/verify-id-card")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyIdCard(
            @RequestBody Map<String, Object> request) {
        try {
            String idCardNumber = (String) request.get("idCardNumber");
            String name = (String) request.get("name");
            Map<String, Object> result = publicSecurityService.verifyIdCard(idCardNumber, name);
            return ResponseEntity.ok(ApiResponse.success("验证完成", result));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("验证失败: " + e.getMessage()));
        }
    }

    /**
     * 上报住宿登记信息
     */
    @PostMapping("/security/report-registration")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reportGuestRegistration(
            @RequestBody Map<String, Object> guestInfo) {
        try {
            Map<String, Object> result = publicSecurityService.reportGuestRegistration(guestInfo);
            return ResponseEntity.ok(ApiResponse.success("上报成功", result));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("上报失败: " + e.getMessage()));
        }
    }

    /**
     * 上报退房信息
     */
    @PostMapping("/security/report-checkout")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reportGuestCheckout(
            @RequestBody Map<String, Object> guestInfo) {
        try {
            Map<String, Object> result = publicSecurityService.reportGuestCheckout(guestInfo);
            return ResponseEntity.ok(ApiResponse.success("上报成功", result));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("上报失败: " + e.getMessage()));
        }
    }

    // ========== 支付网关接口 ==========

    /**
     * 创建支付订单
     */
    @PostMapping("/payment/create-order")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createPaymentOrder(
            @RequestBody Map<String, Object> request) {
        try {
            Long transactionId = Long.valueOf(String.valueOf(request.get("transactionId")));
            BigDecimal amount = new BigDecimal(String.valueOf(request.get("amount")));
            String description = (String) request.get("description");
            Map<String, Object> result = paymentGatewayService.createPaymentOrder(transactionId, amount, description);
            return ResponseEntity.ok(ApiResponse.success("支付订单创建成功", result));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("创建失败: " + e.getMessage()));
        }
    }

    /**
     * 查询支付状态
     */
    @GetMapping("/payment/query-status/{paymentOrderId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> queryPaymentStatus(
            @PathVariable String paymentOrderId) {
        try {
            Map<String, Object> result = paymentGatewayService.queryPaymentStatus(paymentOrderId);
            return ResponseEntity.ok(ApiResponse.success("查询完成", result));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 创建退款订单
     */
    @PostMapping("/payment/create-refund")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createRefundOrder(
            @RequestBody Map<String, Object> request) {
        try {
            Long transactionId = Long.valueOf(String.valueOf(request.get("transactionId")));
            BigDecimal amount = new BigDecimal(String.valueOf(request.get("amount")));
            String reason = (String) request.get("reason");
            Map<String, Object> result = paymentGatewayService.createRefundOrder(transactionId, amount, reason);
            return ResponseEntity.ok(ApiResponse.success("退款订单创建成功", result));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("创建失败: " + e.getMessage()));
        }
    }
}

