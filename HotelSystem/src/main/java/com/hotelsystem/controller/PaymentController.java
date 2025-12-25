package com.hotelsystem.controller;

import com.hotelsystem.dto.ApiResponse;
import com.hotelsystem.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // 测试用：创建一个挂起的支付交易（通常由前端在发起支付时调用）
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Object>> createPendingPayment(@RequestBody Map<String, Object> payload) {
        try {
            Object rid = payload.get("reservationId");
            Object amt = payload.get("amount");
            Object note = payload.get("note");
            if (rid == null || amt == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("缺少 reservationId 或 amount"));
            }
            Long reservationId = Long.valueOf(String.valueOf(rid));
            java.math.BigDecimal amount = new java.math.BigDecimal(String.valueOf(amt));
            var t = paymentService.createPendingPayment(reservationId, amount, note == null ? null : String.valueOf(note));
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("transactionId", t.getId());
            result.put("status", t.getStatus());
            return ResponseEntity.ok(ApiResponse.success("创建支付交易成功", result));
        } catch (Exception ex) {
            return ResponseEntity.ok(ApiResponse.error("创建支付交易失败: " + ex.getMessage()));
        }
    }

    // 占位的支付回调：第三方支付可回调此接口并传回 transactionId、status、providerTransactionId
    @PostMapping("/callback")
    public ResponseEntity<ApiResponse<String>> paymentCallback(@RequestBody Map<String, Object> payload) {
        try {
            // 示例 payload: { transactionId: 1, status: 'SUCCESS', providerTransactionId: 'abc123' }
            Object tid = payload.get("transactionId");
            Object status = payload.get("status");
            Object providerId = payload.get("providerTransactionId");

            if (tid == null || status == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("缺少 transactionId 或 status"));
            }

            Long transactionId = Long.valueOf(String.valueOf(tid));
            String st = String.valueOf(status);

            if ("SUCCESS".equalsIgnoreCase(st)) {
                paymentService.markSuccess(transactionId, providerId != null ? String.valueOf(providerId) : null);
            } else {
                paymentService.markFailed(transactionId, "Provider callback: " + st);
            }

            return ResponseEntity.ok(ApiResponse.success("回调处理完成"));
        } catch (Exception ex) {
            return ResponseEntity.ok(ApiResponse.error("回调处理失败: " + ex.getMessage()));
        }
    }
}
