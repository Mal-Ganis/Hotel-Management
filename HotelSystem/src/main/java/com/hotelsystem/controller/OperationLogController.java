package com.hotelsystem.controller;

import com.hotelsystem.dto.ApiResponse;
import com.hotelsystem.entity.OperationLog;
import com.hotelsystem.repository.OperationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志控制器
 */
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogRepository operationLogRepository;

    /**
     * 获取操作日志列表（分页）
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Page<OperationLog>>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<OperationLog> logs;
        
        if (username != null && !username.isEmpty()) {
            logs = operationLogRepository.findByUsernameContainingIgnoreCase(username, pageable);
        } else if (action != null && !action.isEmpty()) {
            logs = operationLogRepository.findByActionContainingIgnoreCase(action, pageable);
        } else if (startDate != null && endDate != null) {
            logs = operationLogRepository.findByCreatedAtBetween(startDate, endDate, pageable);
        } else {
            logs = operationLogRepository.findAll(pageable);
        }
        
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    /**
     * 获取最近的日志
     */
    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<List<OperationLog>>> getRecentLogs(
            @RequestParam(defaultValue = "50") int limit) {
        List<OperationLog> logs = operationLogRepository.findAll(
                Sort.by(Sort.Direction.DESC, "createdAt")
        ).stream().limit(limit).toList();
        return ResponseEntity.ok(ApiResponse.success(logs));
    }
    
    /**
     * 获取当前用户的操作日志
     */
    @GetMapping("/my-logs")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','HOUSEKEEPING','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Page<OperationLog>>> getMyLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            org.springframework.security.core.Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        if (username == null) {
            return ResponseEntity.ok(ApiResponse.error("未找到当前用户"));
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<OperationLog> logs = operationLogRepository.findByUsernameContainingIgnoreCase(username, pageable);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }
    
    /**
     * 按操作类型查询日志
     */
    @GetMapping("/by-action")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Page<OperationLog>>> getLogsByAction(
            @RequestParam String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<OperationLog> logs = operationLogRepository.findByActionContainingIgnoreCase(action, pageable);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }
    
    /**
     * 获取关键操作详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<OperationLog>> getLogById(@PathVariable Long id) {
        return operationLogRepository.findById(id)
                .map(log -> ResponseEntity.ok(ApiResponse.success(log)))
                .orElse(ResponseEntity.ok(ApiResponse.error("日志不存在")));
    }
}

