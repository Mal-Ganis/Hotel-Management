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
}

