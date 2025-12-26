package com.hotelsystem.controller;

import com.hotelsystem.dto.ApiResponse;
import com.hotelsystem.entity.MaintenanceOrder;
import com.hotelsystem.service.MaintenanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/maintenance")
@RequiredArgsConstructor
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN','HOUSEKEEPING')")
    public ResponseEntity<ApiResponse<List<MaintenanceOrder>>> getAllMaintenanceOrders() {
        List<MaintenanceOrder> orders = maintenanceService.getAllMaintenanceOrders();
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN','HOUSEKEEPING')")
    public ResponseEntity<ApiResponse<List<MaintenanceOrder>>> getPendingMaintenanceOrders() {
        List<MaintenanceOrder> orders = maintenanceService.getPendingMaintenanceOrders();
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/room/{roomId}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN','HOUSEKEEPING')")
    public ResponseEntity<ApiResponse<List<MaintenanceOrder>>> getMaintenanceOrdersByRoom(@PathVariable Long roomId) {
        List<MaintenanceOrder> orders = maintenanceService.getMaintenanceOrdersByRoom(roomId);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN','HOUSEKEEPING')")
    public ResponseEntity<ApiResponse<MaintenanceOrder>> getMaintenanceOrderById(@PathVariable Long id) {
        return maintenanceService.getMaintenanceOrderById(id)
                .map(order -> ResponseEntity.ok(ApiResponse.success(order)))
                .orElse(ResponseEntity.ok(ApiResponse.error("维修工单不存在")));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN','HOUSEKEEPING')")
    public ResponseEntity<ApiResponse<MaintenanceOrder>> createMaintenanceOrder(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        try {
            Long roomId = Long.valueOf(request.get("roomId").toString());
            String problemDescription = request.get("problemDescription").toString();
            MaintenanceOrder.UrgencyLevel urgency = request.get("urgency") != null ?
                    MaintenanceOrder.UrgencyLevel.valueOf(request.get("urgency").toString()) :
                    MaintenanceOrder.UrgencyLevel.MEDIUM;
            LocalDateTime estimatedCompletionTime = request.get("estimatedCompletionTime") != null ?
                    LocalDateTime.parse(request.get("estimatedCompletionTime").toString()) :
                    LocalDateTime.now().plusDays(1);
            String createdBy = authentication != null ? authentication.getName() : "system";

            MaintenanceOrder order = maintenanceService.createMaintenanceOrder(
                    roomId, problemDescription, urgency, estimatedCompletionTime, createdBy);
            return ResponseEntity.ok(ApiResponse.success("维修工单创建成功", order));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<MaintenanceOrder>> assignMaintenanceOrder(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            String assignedTo = request.get("assignedTo");
            MaintenanceOrder order = maintenanceService.assignMaintenanceOrder(id, assignedTo);
            return ResponseEntity.ok(ApiResponse.success("工单已分配", order));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN','HOUSEKEEPING')")
    public ResponseEntity<ApiResponse<MaintenanceOrder>> completeMaintenanceOrder(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        try {
            BigDecimal cost = request.get("cost") != null ?
                    new BigDecimal(request.get("cost").toString()) : BigDecimal.ZERO;
            String notes = request.get("notes") != null ? request.get("notes").toString() : null;

            MaintenanceOrder order = maintenanceService.completeMaintenanceOrder(id, cost, notes);
            return ResponseEntity.ok(ApiResponse.success("维修完成", order));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> cancelMaintenanceOrder(@PathVariable Long id) {
        try {
            maintenanceService.cancelMaintenanceOrder(id);
            return ResponseEntity.ok(ApiResponse.success("工单已取消", null));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }
}

