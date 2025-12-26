package com.hotelsystem.controller;

import com.hotelsystem.dto.ApiResponse;
import com.hotelsystem.dto.ReservationDto;
import com.hotelsystem.entity.Reservation;
import com.hotelsystem.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final com.hotelsystem.repository.GuestRepository guestRepository;
    private final com.hotelsystem.repository.ReservationRepository reservationRepository;
    private final com.hotelsystem.repository.PaymentTransactionRepository paymentTransactionRepository;
    private final com.hotelsystem.repository.OperationLogRepository operationLogRepository;
    private final com.hotelsystem.repository.ReservationHistoryRepository reservationHistoryRepository;
    private final com.hotelsystem.repository.SystemSettingRepository systemSettingRepository;


    @GetMapping
    @PreAuthorize("hasAnyRole('RECEPTIONIST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<List<ReservationDto>>> getAllReservations() {
        List<ReservationDto> reservations = reservationService.getAllReservations();
        return ResponseEntity.ok(ApiResponse.success(reservations));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('GUEST','MANAGER','RECEPTIONIST','ADMIN')")
    public ResponseEntity<ApiResponse<ReservationDto>> getReservationById(@PathVariable Long id, Authentication authentication) {
        try {
            // 如果是宾客，确保只能查看自己的预订
            if (authentication != null && authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_GUEST"))) {
                String email = authentication.getName();
                var guestOpt = guestRepository.findByEmail(email);
                if (guestOpt.isEmpty()) {
                    return ResponseEntity.status(403).body(ApiResponse.error("未找到当前宾客"));
                }
                var reservation = reservationRepository.findById(id);
                if (reservation.isEmpty() || !reservation.get().getGuest().getId().equals(guestOpt.get().getId())) {
                    return ResponseEntity.status(403).body(ApiResponse.error("无权查看此预订"));
                }
            }
            
            return reservationService.getReservationById(id)
                    .map(reservation -> ResponseEntity.ok(ApiResponse.success(reservation)))
                    .orElse(ResponseEntity.ok(ApiResponse.error("预订不存在")));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 获取订单详情（包含订单条款、操作历史等）
     */
    @GetMapping("/{id}/details")
    @PreAuthorize("hasAnyRole('GUEST','MANAGER','RECEPTIONIST','ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReservationDetails(
            @PathVariable Long id, Authentication authentication) {
        try {
            // 权限检查
            if (authentication != null && authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_GUEST"))) {
                String email = authentication.getName();
                var guestOpt = guestRepository.findByEmail(email);
                if (guestOpt.isEmpty()) {
                    return ResponseEntity.status(403).body(ApiResponse.error("未找到当前宾客"));
                }
                var reservation = reservationRepository.findById(id);
                if (reservation.isEmpty() || !reservation.get().getGuest().getId().equals(guestOpt.get().getId())) {
                    return ResponseEntity.status(403).body(ApiResponse.error("无权查看此预订"));
                }
            }
            
            var reservationOpt = reservationRepository.findById(id);
            if (reservationOpt.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error("预订不存在"));
            }
            
            var reservation = reservationOpt.get();
            Map<String, Object> details = new java.util.HashMap<>();
            
            // 基本信息
            ReservationDto dto = reservationService.getReservationById(id).orElse(null);
            details.put("reservation", dto);
            
            // 订单条款（取消政策、保证金说明等）
            Map<String, Object> terms = new java.util.HashMap<>();
            
            // 从SystemSetting获取取消政策
            var cancelPolicySetting = systemSettingRepository.findByKey("cancellation_policy");
            if (cancelPolicySetting.isPresent()) {
                terms.put("cancellationPolicy", cancelPolicySetting.get().getValue());
            } else {
                terms.put("cancellationPolicy", "7天前：全额退款；3天前：退款80%；24小时前：退款50%；24小时内：退款10%；当天：不退款");
            }
            
            // 从SystemSetting获取保证金比例
            var depositSetting = systemSettingRepository.findByKey("deposit_rate");
            String depositRate = "30%";
            if (depositSetting.isPresent()) {
                try {
                    java.math.BigDecimal rate = new java.math.BigDecimal(depositSetting.get().getValue());
                    depositRate = rate.multiply(new java.math.BigDecimal("100")).intValue() + "%";
                } catch (Exception e) {
                    // 使用默认值
                }
            }
            terms.put("depositPolicy", "预订需支付保证金，金额为总房费的" + depositRate);
            terms.put("depositAmount", reservation.getTotalAmount() != null ? 
                reservation.getTotalAmount().multiply(new java.math.BigDecimal(depositSetting.isPresent() ? 
                    depositSetting.get().getValue() : "0.30")) : java.math.BigDecimal.ZERO);
            
            // 从SystemSetting获取入住退房时间
            var checkInTimeSetting = systemSettingRepository.findByKey("check_in_time");
            terms.put("checkInTime", checkInTimeSetting.isPresent() ? checkInTimeSetting.get().getValue() : "14:00");
            
            var checkOutTimeSetting = systemSettingRepository.findByKey("check_out_time");
            terms.put("checkOutTime", checkOutTimeSetting.isPresent() ? checkOutTimeSetting.get().getValue() : "12:00");
            
            details.put("terms", terms);
            
            // 操作历史（从ReservationHistory中获取）
            var historyList = reservationHistoryRepository.findByReservationIdOrderByCreatedAtDesc(id);
            List<Map<String, Object>> history = new java.util.ArrayList<>();
            for (var h : historyList) {
                Map<String, Object> historyItem = new java.util.HashMap<>();
                historyItem.put("action", h.getAction());
                historyItem.put("description", h.getDescription());
                historyItem.put("oldValue", h.getOldValue());
                historyItem.put("newValue", h.getNewValue());
                historyItem.put("time", h.getCreatedAt());
                historyItem.put("operator", h.getOperator());
                history.add(historyItem);
            }
            
            details.put("history", history);
            
            // 支付记录
            var payments = paymentTransactionRepository.findByReservationId(id);
            details.put("payments", payments);
            
            return ResponseEntity.ok(ApiResponse.success(details));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('GUEST','RECEPTIONIST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<ReservationDto>> createReservation(@Valid @RequestBody ReservationDto reservationDto, Authentication authentication) {
        try {
            // 如果是宾客，从token中获取guestId
            if (authentication != null && authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_GUEST"))) {
                String email = authentication.getName();
                var guestOpt = guestRepository.findByEmail(email);
                if (guestOpt.isEmpty()) {
                    return ResponseEntity.status(403).body(ApiResponse.error("未找到当前宾客信息"));
                }
                // 自动设置guestId为当前登录宾客
                reservationDto.setGuestId(guestOpt.get().getId());
            } else if (reservationDto.getGuestId() == null) {
                // 员工创建预订时必须提供guestId
                return ResponseEntity.status(400).body(ApiResponse.error("创建预订时必须指定宾客ID"));
            }

            ReservationDto createdReservation = reservationService.createReservation(reservationDto);
            return ResponseEntity.ok(ApiResponse.success("预订创建成功", createdReservation));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<ReservationDto>> updateReservation(
            @PathVariable Long id,
            @Valid @RequestBody ReservationDto reservationDto) {
        try {
            ReservationDto updatedReservation = reservationService.updateReservation(id, reservationDto);
            return ResponseEntity.ok(ApiResponse.success("预订更新成功", updatedReservation));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteReservation(@PathVariable Long id) {
        try {
            reservationService.deleteReservation(id);
            return ResponseEntity.ok(ApiResponse.success("预订删除成功", null));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('RECEPTIONIST','MANAGER','ADMIN')")
    @GetMapping("/guest/{guestId}")
    public ResponseEntity<ApiResponse<List<ReservationDto>>> getReservationsByGuestId(@PathVariable Long guestId) {
        List<ReservationDto> reservations = reservationService.getReservationsByGuestId(guestId);
        return ResponseEntity.ok(ApiResponse.success(reservations));
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<ApiResponse<List<ReservationDto>>> getReservationsByRoomId(@PathVariable Long roomId) {
        List<ReservationDto> reservations = reservationService.getReservationsByRoomId(roomId);
        return ResponseEntity.ok(ApiResponse.success(reservations));
    }

    // 当前认证宾客查看自己的预订
    @GetMapping("/me")
    @PreAuthorize("hasRole('GUEST')")
    public ResponseEntity<ApiResponse<List<ReservationDto>>> getMyReservations(Authentication authentication) {
        String email = authentication.getName();
        var guestOpt = guestRepository.findByEmail(email);
        if (guestOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error("未找到当前宾客"));
        }
        List<ReservationDto> reservations = reservationService.getReservationsByGuestId(guestOpt.get().getId());
        return ResponseEntity.ok(ApiResponse.success(reservations));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<ReservationDto>>> getReservationsByStatus(@PathVariable Reservation.ReservationStatus status) {
        List<ReservationDto> reservations = reservationService.getReservationsByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(reservations));
    }

    @GetMapping("/checkin-range")
    public ResponseEntity<ApiResponse<List<ReservationDto>>> getReservationsByCheckInDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        List<ReservationDto> reservations = reservationService.getReservationsByCheckInDateRange(start, end);
        return ResponseEntity.ok(ApiResponse.success(reservations));
    }

    // 取消预订
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('GUEST','RECEPTIONIST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Object>> cancelReservation(
            @PathVariable Long id, 
            @RequestBody(required = false) Map<String, String> request,
            Authentication authentication) {
        try {
            // 如果是宾客，确保只是取消自己的预订
            if (authentication != null && authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_GUEST"))) {
                String email = authentication.getName();
                var guestOpt = guestRepository.findByEmail(email);
                if (guestOpt.isEmpty()) {
                    return ResponseEntity.status(403).body(ApiResponse.error("未找到当前宾客"));
                }
                var reservations = reservationService.getReservationsByGuestId(guestOpt.get().getId());
                boolean owns = reservations.stream().anyMatch(r -> r.getId().equals(id));
                if (!owns) {
                    return ResponseEntity.status(403).body(ApiResponse.error("无权取消他人预订"));
                }
            }

            String cancelReason = request != null ? request.get("reason") : null;
            Map<String, Object> result = reservationService.cancelReservation(id, cancelReason);
            return ResponseEntity.ok(ApiResponse.success("取消成功", result));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    // 批量确认预订
    @PostMapping("/batch/confirm")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> batchConfirmReservations(@RequestBody Map<String, List<Long>> request) {
        try {
            List<Long> ids = request.get("ids");
            if (ids == null || ids.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error("请选择要确认的预订"));
            }
            
            int successCount = 0;
            int failCount = 0;
            List<String> errors = new java.util.ArrayList<>();
            
            for (Long id : ids) {
                try {
                    ReservationDto dto = reservationService.getReservationById(id)
                            .orElseThrow(() -> new RuntimeException("预订不存在: " + id));
                    if (dto.getStatus() == Reservation.ReservationStatus.PENDING) {
                        dto.setStatus(Reservation.ReservationStatus.CONFIRMED);
                        reservationService.updateReservation(id, dto);
                        successCount++;
                    } else {
                        failCount++;
                        errors.add("预订 " + id + " 状态不是待确认");
                    }
                } catch (Exception e) {
                    failCount++;
                    errors.add("预订 " + id + ": " + e.getMessage());
                }
            }
            
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            result.put("errors", errors);
            
            return ResponseEntity.ok(ApiResponse.success("批量操作完成", result));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    // 批量取消预订
    @PostMapping("/batch/cancel")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> batchCancelReservations(@RequestBody Map<String, List<Long>> request) {
        try {
            List<Long> ids = request.get("ids");
            if (ids == null || ids.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error("请选择要取消的预订"));
            }
            
            int successCount = 0;
            int failCount = 0;
            List<String> errors = new java.util.ArrayList<>();
            
            for (Long id : ids) {
                try {
                    reservationService.cancelReservation(id, "批量取消");
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    errors.add("预订 " + id + ": " + e.getMessage());
                }
            }
            
            Map<String, Object> batchResult = new java.util.HashMap<>();
            batchResult.put("successCount", successCount);
            batchResult.put("failCount", failCount);
            batchResult.put("errors", errors);
            
            return ResponseEntity.ok(ApiResponse.success("批量操作完成", batchResult));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * 改订预订（优化界面）
     */
    @PostMapping("/{id}/modify")
    @PreAuthorize("hasAnyRole('GUEST','RECEPTIONIST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> modifyReservation(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        try {
            // 权限检查
            if (authentication != null && authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_GUEST"))) {
                String email = authentication.getName();
                var guestOpt = guestRepository.findByEmail(email);
                if (guestOpt.isEmpty()) {
                    return ResponseEntity.status(403).body(ApiResponse.error("未找到当前宾客"));
                }
                var reservations = reservationService.getReservationsByGuestId(guestOpt.get().getId());
                boolean owns = reservations.stream().anyMatch(r -> r.getId().equals(id));
                if (!owns) {
                    return ResponseEntity.status(403).body(ApiResponse.error("无权修改他人预订"));
                }
            }

            java.time.LocalDate newCheckInDate = request.get("checkInDate") != null ?
                    java.time.LocalDate.parse(request.get("checkInDate").toString()) : null;
            java.time.LocalDate newCheckOutDate = request.get("checkOutDate") != null ?
                    java.time.LocalDate.parse(request.get("checkOutDate").toString()) : null;
            Long newRoomId = request.get("roomId") != null ?
                    Long.valueOf(request.get("roomId").toString()) : null;
            String newPreferredRoomType = request.get("preferredRoomType") != null ?
                    request.get("preferredRoomType").toString() : null;
            String operator = authentication != null ? authentication.getName() : "system";

            Map<String, Object> result = reservationService.modifyReservation(
                    id, newCheckInDate, newCheckOutDate, newRoomId, newPreferredRoomType, operator);
            return ResponseEntity.ok(ApiResponse.success("改订成功", result));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * 获取可用房间列表（用于改订界面）
     */
    @GetMapping("/modify/available-rooms")
    @PreAuthorize("hasAnyRole('GUEST','RECEPTIONIST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAvailableRoomsForModify(
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) 
                java.time.LocalDate checkInDate,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) 
                java.time.LocalDate checkOutDate,
            @RequestParam(required = false) String preferredRoomType) {
        try {
            List<Map<String, Object>> rooms = reservationService.getAvailableRoomsForModify(
                    checkInDate, checkOutDate, preferredRoomType);
            return ResponseEntity.ok(ApiResponse.success(rooms));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }
}