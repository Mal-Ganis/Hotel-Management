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


    @GetMapping
    @PreAuthorize("hasAnyRole('RECEPTIONIST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<List<ReservationDto>>> getAllReservations() {
        List<ReservationDto> reservations = reservationService.getAllReservations();
        return ResponseEntity.ok(ApiResponse.success(reservations));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','RECEPTIONIST','ADMIN')")
    public ResponseEntity<ApiResponse<ReservationDto>> getReservationById(@PathVariable Long id) {
        return reservationService.getReservationById(id)
                .map(reservation -> ResponseEntity.ok(ApiResponse.success(reservation)))
                .orElse(ResponseEntity.ok(ApiResponse.error("预订不存在")));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('GUEST','RECEPTIONIST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<ReservationDto>> createReservation(@RequestBody ReservationDto reservationDto, Authentication authentication) {
        try {
            // 如果是宾客，从token中获取guestId（在验证之前设置）
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

            // 手动验证必填字段
            if (reservationDto.getGuestId() == null) {
                return ResponseEntity.status(400).body(ApiResponse.error("宾客ID不能为空"));
            }
            if (reservationDto.getCheckInDate() == null) {
                return ResponseEntity.status(400).body(ApiResponse.error("入住日期不能为空"));
            }
            if (reservationDto.getCheckOutDate() == null) {
                return ResponseEntity.status(400).body(ApiResponse.error("离店日期不能为空"));
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
    public ResponseEntity<ApiResponse<Object>> cancelReservation(@PathVariable Long id, Authentication authentication) {
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

            Map<String, Object> result = reservationService.cancelReservation(id);
            return ResponseEntity.ok(ApiResponse.success("取消成功", result));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }
}