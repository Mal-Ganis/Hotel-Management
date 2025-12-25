package com.hotelsystem.controller;

import com.hotelsystem.dto.ApiResponse;
import com.hotelsystem.dto.RoomDto;
import com.hotelsystem.service.RoomService;
import com.hotelsystem.service.RoomStatusUpdateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
@Slf4j
public class RoomController {

    private final RoomService roomService;
    private final RoomStatusUpdateService roomStatusUpdateService;

    @GetMapping
    @PreAuthorize("hasAnyRole('GUEST','RECEPTIONIST','MANAGER','ADMIN','HOUSEKEEPING')")
    public ResponseEntity<ApiResponse<List<RoomDto>>> getAllRooms() {
        List<RoomDto> rooms = roomService.getAllRooms();
        return ResponseEntity.ok(ApiResponse.success(rooms));
    }

    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('GUEST','RECEPTIONIST','MANAGER','ADMIN','HOUSEKEEPING')")
    public ResponseEntity<ApiResponse<List<RoomDto>>> getAvailableRooms(
            @RequestParam(required = false) String checkIn,
            @RequestParam(required = false) String checkOut) {
        try {
            LocalDate in = (checkIn == null || checkIn.isEmpty()) ? null : LocalDate.parse(checkIn);
            LocalDate out = (checkOut == null || checkOut.isEmpty()) ? null : LocalDate.parse(checkOut);
            List<RoomDto> availableRooms = roomService.getAvailableRooms(in, out);
            return ResponseEntity.ok(ApiResponse.success(availableRooms));
        } catch (DateTimeParseException e) {
            return ResponseEntity.ok(ApiResponse.error("日期格式错误，使用 YYYY-MM-DD 格式"));
        }
    }

    @GetMapping("/by-type")
    @PreAuthorize("hasAnyRole('GUEST','RECEPTIONIST','MANAGER','ADMIN','HOUSEKEEPING')")
    public ResponseEntity<ApiResponse<List<RoomDto>>> getRoomsByType(@RequestParam String roomType) {
        List<RoomDto> rooms = roomService.getRoomsByType(roomType);
        return ResponseEntity.ok(ApiResponse.success(rooms));
    }

    @GetMapping("/type/{roomType}")
    @PreAuthorize("hasAnyRole('GUEST','RECEPTIONIST','MANAGER','ADMIN','HOUSEKEEPING')")
    public ResponseEntity<ApiResponse<List<RoomDto>>> getRoomsByTypePath(@PathVariable String roomType) {
        List<RoomDto> rooms = roomService.getRoomsByType(roomType);
        return ResponseEntity.ok(ApiResponse.success(rooms));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('GUEST','RECEPTIONIST','MANAGER','ADMIN','HOUSEKEEPING')")
    public ResponseEntity<ApiResponse<List<RoomDto>>> getActiveRooms() {
        List<RoomDto> activeRooms = roomService.getActiveRooms();
        return ResponseEntity.ok(ApiResponse.success(activeRooms));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('GUEST','RECEPTIONIST','MANAGER','ADMIN','HOUSEKEEPING')")
    public ResponseEntity<ApiResponse<RoomDto>> getRoomById(@PathVariable Long id) {
        return roomService.getRoomById(id)
                .map(room -> ResponseEntity.ok(ApiResponse.success(room)))
                .orElse(ResponseEntity.ok(ApiResponse.error("房间不存在")));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<RoomDto>> createRoom(@Valid @RequestBody RoomDto roomDto) {
        try {
            RoomDto createdRoom = roomService.createRoom(roomDto);
            return ResponseEntity.ok(ApiResponse.success("房间创建成功", createdRoom));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<RoomDto>> updateRoom(
            @PathVariable Long id,
            @Valid @RequestBody RoomDto roomDto) {
        try {
            RoomDto updatedRoom = roomService.updateRoom(id, roomDto);
            return ResponseEntity.ok(ApiResponse.success("房间更新成功", updatedRoom));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(@PathVariable Long id) {
        try {
            roomService.deleteRoom(id);
            return ResponseEntity.ok(ApiResponse.success("房间删除成功", null));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    // 更新房间状态（供前台和房务员工使用）
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','HOUSEKEEPING','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<RoomDto>> updateRoomStatus(
            @PathVariable Long id,
            @RequestParam String status,
            org.springframework.security.core.Authentication authentication) {
        try {
            RoomDto roomDto = roomService.getRoomById(id)
                    .orElseThrow(() -> new RuntimeException("房间不存在"));
            
            // 验证状态值
            com.hotelsystem.entity.Room.RoomStatus newStatus;
            try {
                newStatus = com.hotelsystem.entity.Room.RoomStatus.valueOf(status.toUpperCase());
                roomDto.setStatus(newStatus);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.ok(ApiResponse.error("无效的房间状态"));
            }
            
            RoomDto updatedRoom = roomService.updateRoom(id, roomDto);
            
            // 如果状态更新为AVAILABLE（已清洁完成），自动触发库存扣减
            if (newStatus == com.hotelsystem.entity.Room.RoomStatus.AVAILABLE) {
                try {
                    String createdBy = authentication != null ? authentication.getName() : "system";
                    roomStatusUpdateService.handleRoomCleaned(id, createdBy);
                } catch (Exception e) {
                    // 库存扣减失败不影响房态更新
                    log.warn("房间状态更新后自动扣减库存失败: {}", e.getMessage());
                }
            }
            
            return ResponseEntity.ok(ApiResponse.success("房间状态更新成功", updatedRoom));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }
}
