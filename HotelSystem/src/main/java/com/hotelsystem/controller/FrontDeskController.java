package com.hotelsystem.controller;

import com.hotelsystem.dto.CheckInRequest;
import com.hotelsystem.dto.CheckOutRequest;
import com.hotelsystem.dto.ApiResponse;
import com.hotelsystem.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/frontdesk")
@RequiredArgsConstructor
public class FrontDeskController {

    private final ReservationService reservationService;

    // 前台办理入住（员工权限）
    @PostMapping("/checkin/{reservationId}")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkIn(
            @PathVariable Long reservationId,
            @RequestBody(required = false) CheckInRequest request,
            Authentication authentication) {
        try {
            java.math.BigDecimal collect = request != null ? request.getCollectAmount() : null;
            String staff = authentication != null ? authentication.getName() : "system";
            Map<String, Object> result = reservationService.checkIn(reservationId, collect, staff);
            return ResponseEntity.ok(ApiResponse.success("入住成功", result));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    // 前台办理退房（员工权限）
    @PostMapping("/checkout/{reservationId}")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkOut(
            @PathVariable Long reservationId,
            @RequestBody(required = false) CheckOutRequest request,
            Authentication authentication) {
        try {
            java.math.BigDecimal extras = request != null ? request.getExtraCharges() : null;
            java.math.BigDecimal collect = request != null ? request.getCollectAmount() : null;
            String staff = authentication != null ? authentication.getName() : "system";
            Map<String, Object> result = reservationService.checkOut(reservationId, extras, collect, staff);
            return ResponseEntity.ok(ApiResponse.success("退房结算完成", result));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }
}
