package com.hotelsystem.controller;

import com.hotelsystem.dto.ApiResponse;
import com.hotelsystem.entity.Waitlist;
import com.hotelsystem.service.WaitlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/waitlist")
@RequiredArgsConstructor
public class WaitlistController {

    private final WaitlistService waitlistService;
    private final com.hotelsystem.repository.GuestRepository guestRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('RECEPTIONIST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<List<Waitlist>>> getAllWaitlist() {
        List<Waitlist> waitlist = waitlistService.getAllWaitlist();
        return ResponseEntity.ok(ApiResponse.success(waitlist));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<List<Waitlist>>> getPendingWaitlist() {
        List<Waitlist> waitlist = waitlistService.getPendingWaitlist();
        return ResponseEntity.ok(ApiResponse.success(waitlist));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Waitlist>> getWaitlistById(@PathVariable Long id) {
        return waitlistService.getWaitlistById(id)
                .map(waitlist -> ResponseEntity.ok(ApiResponse.success(waitlist)))
                .orElse(ResponseEntity.ok(ApiResponse.error("候补记录不存在")));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('GUEST','RECEPTIONIST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Waitlist>> addToWaitlist(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        try {
            Long guestId;
            // 如果是宾客，从token中获取guestId
            if (authentication != null && authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_GUEST"))) {
                String email = authentication.getName();
                var guestOpt = guestRepository.findByEmail(email);
                if (guestOpt.isEmpty()) {
                    return ResponseEntity.status(403).body(ApiResponse.error("未找到当前宾客信息"));
                }
                guestId = guestOpt.get().getId();
            } else {
                guestId = Long.valueOf(request.get("guestId").toString());
            }

            LocalDate desiredCheckInDate = LocalDate.parse(request.get("desiredCheckInDate").toString());
            LocalDate desiredCheckOutDate = LocalDate.parse(request.get("desiredCheckOutDate").toString());
            String preferredRoomType = request.get("preferredRoomType") != null ? 
                    request.get("preferredRoomType").toString() : null;
            String phone = request.get("phone") != null ? request.get("phone").toString() : null;
            String email = request.get("email") != null ? request.get("email").toString() : null;

            Waitlist waitlist = waitlistService.addToWaitlist(guestId, desiredCheckInDate, desiredCheckOutDate,
                    preferredRoomType, phone, email);
            return ResponseEntity.ok(ApiResponse.success("已加入候补名单", waitlist));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/notify")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> notifyWaitlist(@PathVariable Long id) {
        try {
            waitlistService.notifyWaitlist(id);
            return ResponseEntity.ok(ApiResponse.success("通知已发送", null));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/convert")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<com.hotelsystem.entity.Reservation>> convertToReservation(
            @PathVariable Long id,
            @RequestBody Map<String, Long> request) {
        try {
            Long roomId = request.get("roomId");
            com.hotelsystem.entity.Reservation reservation = waitlistService.convertToReservation(id, roomId);
            return ResponseEntity.ok(ApiResponse.success("已转为预订", reservation));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('GUEST','RECEPTIONIST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> cancelWaitlist(@PathVariable Long id) {
        try {
            waitlistService.cancelWaitlist(id);
            return ResponseEntity.ok(ApiResponse.success("已取消候补", null));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }
}

