package com.hotelsystem.controller;

import com.hotelsystem.dto.ApiResponse;
import com.hotelsystem.entity.PaymentTransaction;
import com.hotelsystem.repository.PaymentTransactionRepository;
import com.hotelsystem.repository.GuestRepository;
import com.hotelsystem.repository.ReservationRepository;
import com.hotelsystem.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final GuestRepository guestRepository;
    private final ReservationRepository reservationRepository;

    /**
     * 获取支持的支付方式列表
     */
    @GetMapping("/methods")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPaymentMethods() {
        List<Map<String, Object>> methods = new java.util.ArrayList<>();
        
        Map<String, Object> alipay = new java.util.HashMap<>();
        alipay.put("code", "ALIPAY");
        alipay.put("name", "支付宝");
        alipay.put("icon", "alipay-icon");
        alipay.put("description", "支持支付宝扫码支付");
        methods.add(alipay);
        
        Map<String, Object> wechat = new java.util.HashMap<>();
        wechat.put("code", "WECHAT");
        wechat.put("name", "微信支付");
        wechat.put("icon", "wechat-icon");
        wechat.put("description", "支持微信扫码支付");
        methods.add(wechat);
        
        Map<String, Object> cash = new java.util.HashMap<>();
        cash.put("code", "CASH");
        cash.put("name", "现金支付");
        cash.put("icon", "cash-icon");
        cash.put("description", "前台现金支付");
        methods.add(cash);
        
        Map<String, Object> card = new java.util.HashMap<>();
        card.put("code", "CARD");
        card.put("name", "银行卡");
        card.put("icon", "card-icon");
        card.put("description", "支持刷卡支付");
        methods.add(card);
        
        return ResponseEntity.ok(ApiResponse.success(methods));
    }

    // 测试用：创建一个挂起的支付交易（通常由前端在发起支付时调用）
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Object>> createPendingPayment(@RequestBody Map<String, Object> payload) {
        try {
            Object rid = payload.get("reservationId");
            Object amt = payload.get("amount");
            Object note = payload.get("note");
            Object paymentMethod = payload.get("paymentMethod"); // 支付方式
            
            if (rid == null || amt == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("缺少 reservationId 或 amount"));
            }
            
            // 验证支付方式
            if (paymentMethod != null) {
                String method = String.valueOf(paymentMethod);
                if (!java.util.Arrays.asList("ALIPAY", "WECHAT", "CASH", "CARD").contains(method)) {
                    return ResponseEntity.badRequest().body(ApiResponse.error("不支持的支付方式"));
                }
            }
            
            Long reservationId = Long.valueOf(String.valueOf(rid));
            java.math.BigDecimal amount = new java.math.BigDecimal(String.valueOf(amt));
            var t = paymentService.createPendingPayment(reservationId, amount, 
                    note == null ? null : String.valueOf(note),
                    paymentMethod != null ? String.valueOf(paymentMethod) : null);
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("transactionId", t.getId());
            result.put("status", t.getStatus());
            result.put("paymentMethod", t.getPaymentMethod());
            return ResponseEntity.ok(ApiResponse.success("创建支付交易成功", result));
        } catch (NumberFormatException e) {
            return ResponseEntity.ok(ApiResponse.error("金额格式错误，请检查输入"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.error("参数错误: " + e.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.ok(ApiResponse.error("创建支付交易失败: " + ex.getMessage() + 
                    "。请检查网络连接或联系客服"));
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
                String errorMessage = "支付失败";
                if ("FAILED".equalsIgnoreCase(st)) {
                    errorMessage = "支付失败，请检查账户余额或联系客服";
                } else if ("TIMEOUT".equalsIgnoreCase(st)) {
                    errorMessage = "支付超时，请重试或选择其他支付方式";
                } else if ("CANCELLED".equalsIgnoreCase(st)) {
                    errorMessage = "支付已取消";
                }
                paymentService.markFailed(transactionId, errorMessage);
            }

            return ResponseEntity.ok(ApiResponse.success("回调处理完成"));
        } catch (NumberFormatException e) {
            return ResponseEntity.ok(ApiResponse.error("交易ID格式错误"));
        } catch (Exception ex) {
            return ResponseEntity.ok(ApiResponse.error("回调处理失败: " + ex.getMessage() + 
                    "。如问题持续，请联系技术支持"));
        }
    }
    
    /**
     * 查询支付状态（用于前端轮询）
     */
    @GetMapping("/transaction/{transactionId}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPaymentStatus(@PathVariable Long transactionId) {
        try {
            var transactionOpt = paymentTransactionRepository.findById(transactionId);
            if (transactionOpt.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error("交易不存在"));
            }
            
            var transaction = transactionOpt.get();
            Map<String, Object> statusInfo = new java.util.HashMap<>();
            statusInfo.put("transactionId", transaction.getId());
            statusInfo.put("status", transaction.getStatus());
            statusInfo.put("amount", transaction.getAmount());
            statusInfo.put("paymentMethod", transaction.getPaymentMethod());
            statusInfo.put("createdAt", transaction.getCreatedAt());
            
            // 根据状态提供不同的提示信息
            String message = "";
            String suggestion = "";
            if (transaction.getStatus() == PaymentTransaction.TransactionStatus.PENDING) {
                message = "支付处理中，请稍候...";
                suggestion = "如长时间未完成，请刷新页面或联系客服";
            } else if (transaction.getStatus() == PaymentTransaction.TransactionStatus.SUCCESS) {
                message = "支付成功";
                suggestion = "订单已确认，请查看订单详情";
            } else if (transaction.getStatus() == PaymentTransaction.TransactionStatus.FAILED) {
                message = "支付失败";
                suggestion = transaction.getNote() != null ? transaction.getNote() : 
                        "请检查账户余额或网络连接，或选择其他支付方式";
            }
            statusInfo.put("message", message);
            statusInfo.put("suggestion", suggestion);
            
            return ResponseEntity.ok(ApiResponse.success(statusInfo));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("查询失败: " + e.getMessage()));
        }
    }
    
    /**
     * 重试支付
     */
    @PostMapping("/transaction/{transactionId}/retry")
    public ResponseEntity<ApiResponse<Map<String, Object>>> retryPayment(@PathVariable Long transactionId) {
        try {
            var transactionOpt = paymentTransactionRepository.findById(transactionId);
            if (transactionOpt.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error("交易不存在"));
            }
            
            var transaction = transactionOpt.get();
            if (transaction.getStatus() != PaymentTransaction.TransactionStatus.FAILED) {
                return ResponseEntity.ok(ApiResponse.error("只有失败的交易才能重试"));
            }
            
            // 创建新的支付交易
            var newTransaction = paymentService.createPendingPayment(
                    transaction.getReservationId(),
                    transaction.getAmount(),
                    "重试支付 - " + (transaction.getNote() != null ? transaction.getNote() : ""),
                    transaction.getPaymentMethod());
            
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("newTransactionId", newTransaction.getId());
            result.put("message", "已创建新的支付交易，请完成支付");
            
            return ResponseEntity.ok(ApiResponse.success("重试成功", result));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("重试失败: " + e.getMessage()));
        }
    }

    /**
     * 获取支付记录（按预订ID）
     */
    @GetMapping("/reservation/{reservationId}")
    @PreAuthorize("hasAnyRole('GUEST','RECEPTIONIST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<List<PaymentTransaction>>> getPaymentsByReservation(
            @PathVariable Long reservationId, Authentication authentication) {
        try {
            // 如果是宾客，确保只能查看自己的预订
            if (authentication != null && authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_GUEST"))) {
                String email = authentication.getName();
                var guestOpt = guestRepository.findByEmail(email);
                if (guestOpt.isEmpty()) {
                    return ResponseEntity.status(403).body(ApiResponse.error("未找到当前宾客"));
                }
                var reservation = reservationRepository.findById(reservationId);
                if (reservation.isEmpty() || !reservation.get().getGuest().getId().equals(guestOpt.get().getId())) {
                    return ResponseEntity.status(403).body(ApiResponse.error("无权查看此预订的支付记录"));
                }
            }
            
            List<PaymentTransaction> transactions = paymentService.getTransactionsByReservation(reservationId);
            return ResponseEntity.ok(ApiResponse.success(transactions));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 获取我的支付记录（宾客端）
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('GUEST')")
    public ResponseEntity<ApiResponse<List<PaymentTransaction>>> getMyPayments(Authentication authentication) {
        try {
            String email = authentication.getName();
            var guestOpt = guestRepository.findByEmail(email);
            if (guestOpt.isEmpty()) {
                return ResponseEntity.status(403).body(ApiResponse.error("未找到当前宾客"));
            }
            
            // 获取宾客的所有预订
            var reservations = reservationRepository.findByGuestId(guestOpt.get().getId());
            List<Long> reservationIds = reservations.stream()
                    .map(r -> r.getId())
                    .collect(Collectors.toList());
            
            // 获取这些预订的所有支付记录
            List<PaymentTransaction> transactions = paymentTransactionRepository.findAll().stream()
                    .filter(t -> reservationIds.contains(t.getReservationId()))
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success(transactions));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 管理端查询所有支付记录（支持筛选）
     */
    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<List<PaymentTransaction>>> getAllPayments(
            @RequestParam(required = false) PaymentTransaction.TransactionType type,
            @RequestParam(required = false) PaymentTransaction.TransactionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<PaymentTransaction> allTransactions = paymentTransactionRepository.findAll();
            
            // 应用筛选条件
            List<PaymentTransaction> filtered = allTransactions.stream()
                    .filter(t -> type == null || t.getType() == type)
                    .filter(t -> status == null || t.getStatus() == status)
                    .filter(t -> {
                        if (startDate == null && endDate == null) return true;
                        if (t.getCreatedAt() == null) return false;
                        LocalDate transactionDate = t.getCreatedAt().toLocalDate();
                        if (startDate != null && transactionDate.isBefore(startDate)) return false;
                        if (endDate != null && transactionDate.isAfter(endDate)) return false;
                        return true;
                    })
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success(filtered));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }
}
