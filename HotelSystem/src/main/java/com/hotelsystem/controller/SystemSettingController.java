package com.hotelsystem.controller;

import com.hotelsystem.dto.ApiResponse;
import com.hotelsystem.dto.SystemSettingDto;
import com.hotelsystem.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SystemSettingController {

    private final SystemSettingService settingService;

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<List<SystemSettingDto>>> getAllSettings() {
        List<SystemSettingDto> settings = settingService.getAllSettings();
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    @GetMapping("/{key}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<SystemSettingDto>> getSettingByKey(@PathVariable String key) {
        return settingService.getSettingByKey(key)
                .map(setting -> ResponseEntity.ok(ApiResponse.success(setting)))
                .orElse(ResponseEntity.ok(ApiResponse.error("设置不存在")));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<SystemSettingDto>> saveSetting(@RequestBody SystemSettingDto dto) {
        try {
            SystemSettingDto saved = settingService.saveSetting(dto);
            return ResponseEntity.ok(ApiResponse.success("设置保存成功", saved));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{key}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<SystemSettingDto>> updateSetting(
            @PathVariable String key,
            @RequestBody SystemSettingDto dto) {
        try {
            dto.setKey(key);
            SystemSettingDto saved = settingService.saveSetting(dto);
            return ResponseEntity.ok(ApiResponse.success("设置更新成功", saved));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSetting(@PathVariable Long id) {
        try {
            settingService.deleteSetting(id);
            return ResponseEntity.ok(ApiResponse.success("设置删除成功", null));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }
}

