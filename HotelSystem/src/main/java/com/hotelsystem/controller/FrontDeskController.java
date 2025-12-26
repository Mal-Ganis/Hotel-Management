package com.hotelsystem.controller;

import com.hotelsystem.dto.CheckInRequest;
import com.hotelsystem.dto.CheckOutRequest;
import com.hotelsystem.dto.ApiResponse;
import com.hotelsystem.dto.ReservationDto;
import com.hotelsystem.service.ReservationService;
import com.hotelsystem.repository.GuestRepository;
import com.hotelsystem.repository.RoomRepository;
import com.hotelsystem.entity.Guest;
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
    private final GuestRepository guestRepository;
    private final RoomRepository roomRepository;

    // 散客快速入住（无需预订）
    @PostMapping("/quick-checkin")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> quickCheckIn(
            @RequestBody CheckInRequest request,
            Authentication authentication) {
        try {
            String staff = authentication != null ? authentication.getName() : "system";
            
            // 查找或创建宾客
            Guest guest = null;
            if (request.getIdCardNumber() != null) {
                guest = guestRepository.findByIdCardNumber(request.getIdCardNumber()).orElse(null);
            }
            if (guest == null && request.getGuestId() != null) {
                guest = guestRepository.findById(request.getGuestId()).orElse(null);
            }
            if (guest == null) {
                return ResponseEntity.ok(ApiResponse.error("未找到宾客信息，请先创建宾客"));
            }
            
            // 创建临时预订
            ReservationDto reservationDto = new ReservationDto();
            reservationDto.setGuestId(guest.getId());
            reservationDto.setRoomId(request.getRoomId());
            reservationDto.setCheckInDate(request.getCheckInDate() != null ? request.getCheckInDate() : java.time.LocalDate.now());
            reservationDto.setCheckOutDate(request.getCheckOutDate());
            reservationDto.setNumberOfGuests(request.getNumberOfGuests());
            reservationDto.setPreferredRoomType(request.getPreferredRoomType());
            reservationDto.setStatus(com.hotelsystem.entity.Reservation.ReservationStatus.PENDING);
            
            ReservationDto createdReservation = reservationService.createReservation(reservationDto);
            
            // 直接办理入住
            Map<String, Object> result = reservationService.checkIn(createdReservation.getId(), request.getCollectAmount(), staff);
            result.put("isWalkIn", true); // 标记为散客
            return ResponseEntity.ok(ApiResponse.success("散客入住成功", result));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

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
    
    // 获取入住确认信息（用于确认页面）
    @GetMapping("/checkin/{reservationId}/confirm")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCheckInConfirmation(@PathVariable Long reservationId) {
        try {
            return reservationService.getReservationById(reservationId)
                    .map(reservation -> {
                        Map<String, Object> confirmInfo = new java.util.HashMap<>();
                        confirmInfo.put("reservation", reservation);
                        confirmInfo.put("room", reservation.getRoom());
                        confirmInfo.put("guest", reservation.getGuest());
                        // 计算押金等
                        java.math.BigDecimal deposit = reservation.getTotalAmount() != null ? 
                            reservation.getTotalAmount().multiply(new java.math.BigDecimal("0.30")) : 
                            java.math.BigDecimal.ZERO;
                        confirmInfo.put("deposit", deposit);
                        return ResponseEntity.ok(ApiResponse.success(confirmInfo));
                    })
                    .orElse(ResponseEntity.ok(ApiResponse.error("预订不存在")));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    // 房间检查（退房前）
    @PostMapping("/checkout/{reservationId}/inspection")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> roomInspection(
            @PathVariable Long reservationId,
            @RequestBody CheckOutRequest request,
            Authentication authentication) {
        try {
            String staff = authentication != null ? authentication.getName() : "system";
            Map<String, Object> result = reservationService.performRoomInspection(
                    reservationId, request, staff);
            return ResponseEntity.ok(ApiResponse.success("房间检查完成", result));
        } catch (Exception e) {
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
            // 如果提供了房间检查信息，先执行检查
            if (request != null && request.getRoomInspectionCompleted() != null && request.getRoomInspectionCompleted()) {
                String staff = authentication != null ? authentication.getName() : "system";
                reservationService.performRoomInspection(reservationId, request, staff);
            }
            
            java.math.BigDecimal extras = request != null ? request.getExtraCharges() : null;
            java.math.BigDecimal collect = request != null ? request.getCollectAmount() : null;
            String staff = authentication != null ? authentication.getName() : "system";
            Map<String, Object> result = reservationService.checkOut(reservationId, extras, collect, staff);
            return ResponseEntity.ok(ApiResponse.success("退房结算完成", result));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }
    
    // 获取退房账单详情
    @GetMapping("/checkout/{reservationId}/bill")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCheckOutBill(@PathVariable Long reservationId) {
        try {
            Map<String, Object> billDetails = reservationService.getCheckOutBillDetails(reservationId);
            return ResponseEntity.ok(ApiResponse.success(billDetails));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }
}
