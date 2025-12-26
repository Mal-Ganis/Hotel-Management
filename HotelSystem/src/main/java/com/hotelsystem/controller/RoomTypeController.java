package com.hotelsystem.controller;

import com.hotelsystem.dto.ApiResponse;
import com.hotelsystem.entity.RoomType;
import com.hotelsystem.service.RoomTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/room-types")
@RequiredArgsConstructor
public class RoomTypeController {

    private final RoomTypeService roomTypeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoomType>>> getAllRoomTypes() {
        List<RoomType> roomTypes = roomTypeService.getAllRoomTypes();
        return ResponseEntity.ok(ApiResponse.success(roomTypes));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<RoomType>>> getActiveRoomTypes() {
        List<RoomType> roomTypes = roomTypeService.getActiveRoomTypes();
        return ResponseEntity.ok(ApiResponse.success(roomTypes));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomType>> getRoomTypeById(@PathVariable Long id) {
        return roomTypeService.getRoomTypeById(id)
                .map(roomType -> ResponseEntity.ok(ApiResponse.success(roomType)))
                .orElse(ResponseEntity.ok(ApiResponse.error("房型不存在")));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<RoomType>> createRoomType(@RequestBody RoomType roomType) {
        try {
            RoomType created = roomTypeService.createRoomType(roomType);
            return ResponseEntity.ok(ApiResponse.success("房型创建成功", created));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<RoomType>> updateRoomType(
            @PathVariable Long id,
            @RequestBody RoomType roomType) {
        try {
            RoomType updated = roomTypeService.updateRoomType(id, roomType);
            return ResponseEntity.ok(ApiResponse.success("房型更新成功", updated));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteRoomType(@PathVariable Long id) {
        try {
            roomTypeService.deleteRoomType(id);
            return ResponseEntity.ok(ApiResponse.success("房型删除成功", null));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }
}

