package com.hotelsystem.controller;

import com.hotelsystem.dto.ApiResponse;
import com.hotelsystem.dto.GuestPreferenceDto;
import com.hotelsystem.service.GuestPreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/guests/{guestId}/preferences")
@RequiredArgsConstructor
public class GuestPreferenceController {

    private final GuestPreferenceService preferenceService;

    @GetMapping
    @PreAuthorize("hasAnyRole('RECEPTIONIST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<List<GuestPreferenceDto>>> getPreferences(@PathVariable Long guestId) {
        List<GuestPreferenceDto> preferences = preferenceService.getPreferencesByGuestId(guestId);
        return ResponseEntity.ok(ApiResponse.success(preferences));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('RECEPTIONIST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<GuestPreferenceDto>> createPreference(
            @PathVariable Long guestId,
            @RequestBody GuestPreferenceDto dto) {
        try {
            GuestPreferenceDto created = preferenceService.createPreference(guestId, dto);
            return ResponseEntity.ok(ApiResponse.success("创建成功", created));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<GuestPreferenceDto>> updatePreference(
            @PathVariable Long id,
            @RequestBody GuestPreferenceDto dto) {
        try {
            GuestPreferenceDto updated = preferenceService.updatePreference(id, dto);
            return ResponseEntity.ok(ApiResponse.success("更新成功", updated));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePreference(@PathVariable Long id) {
        try {
            preferenceService.deletePreference(id);
            return ResponseEntity.ok(ApiResponse.success("删除成功", null));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/extract")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<String>> extractPreferencesFromHistory(@PathVariable Long guestId) {
        try {
            preferenceService.extractPreferencesFromHistory(guestId);
            return ResponseEntity.ok(ApiResponse.success("偏好提取成功", null));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/recommendations")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<List<String>>> getRecommendedRoomTypes(@PathVariable Long guestId) {
        List<String> recommendations = preferenceService.getRecommendedRoomTypes(guestId);
        return ResponseEntity.ok(ApiResponse.success(recommendations));
    }
}

