package com.hotelsystem.controller;

import com.hotelsystem.dto.ApiResponse;
import com.hotelsystem.dto.RoomDto;
import com.hotelsystem.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

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
}
