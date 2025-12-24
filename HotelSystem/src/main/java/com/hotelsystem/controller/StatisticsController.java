package com.hotelsystem.controller;

import com.hotelsystem.dto.ApiResponse;
import com.hotelsystem.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * 统计报表控制器
 */
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    /**
     * 获取今日统计
     */
    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','HOUSEKEEPING','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTodayStatistics() {
        Map<String, Object> stats = statisticsService.getTodayStatistics();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * 获取日期范围统计
     */
    @GetMapping("/date-range")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDateRangeStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Map<String, Object> stats = statisticsService.getDateRangeStatistics(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * 获取房型统计
     */
    @GetMapping("/room-types")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRoomTypeStatistics() {
        Map<String, Object> stats = statisticsService.getRoomTypeStatistics();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}

