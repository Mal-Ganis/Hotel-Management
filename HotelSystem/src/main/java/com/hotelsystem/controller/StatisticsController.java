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
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRoomTypeStatistics() {
        Map<String, Object> stats = statisticsService.getRoomTypeStatistics();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * 获取完整的经营分析报表（收入、成本、利润）
     */
    @GetMapping("/business-analysis")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBusinessAnalysis(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Map<String, Object> analysis = statisticsService.getBusinessAnalysis(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(analysis));
    }
    
    /**
     * 对比分析（同比、环比）
     */
    @GetMapping("/compare")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCompareAnalysis(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "YEAR_OVER_YEAR") String compareType) {
        Map<String, Object> comparison = statisticsService.getCompareAnalysis(startDate, endDate, compareType);
        return ResponseEntity.ok(ApiResponse.success(comparison));
    }
    
    /**
     * 实时数据看板
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRealTimeDashboard() {
        Map<String, Object> dashboard = statisticsService.getRealTimeDashboard();
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }
    
    /**
     * 导出数据为Excel
     */
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<org.springframework.core.io.Resource> exportToExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "BUSINESS_ANALYSIS") String exportType) {
        try {
            org.springframework.core.io.Resource resource = statisticsService.exportToExcel(startDate, endDate, exportType);
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=statistics_" + startDate + "_" + endDate + ".xlsx")
                    .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

