package com.hotelsystem.controller;

import com.hotelsystem.dto.ApiResponse;
import com.hotelsystem.dto.InventoryDto;
import com.hotelsystem.dto.InventoryTransactionDto;
import com.hotelsystem.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    @PreAuthorize("hasAnyRole('HOUSEKEEPING','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<List<InventoryDto>>> getAllInventories() {
        List<InventoryDto> inventories = inventoryService.getAllInventories();
        return ResponseEntity.ok(ApiResponse.success(inventories));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('HOUSEKEEPING','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<InventoryDto>> getInventoryById(@PathVariable Long id) {
        return inventoryService.getInventoryById(id)
                .map(inventory -> ResponseEntity.ok(ApiResponse.success(inventory)))
                .orElse(ResponseEntity.ok(ApiResponse.error("库存物品不存在")));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<InventoryDto>> createInventory(@RequestBody InventoryDto dto) {
        try {
            InventoryDto created = inventoryService.createInventory(dto);
            return ResponseEntity.ok(ApiResponse.success("创建成功", created));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<InventoryDto>> updateInventory(
            @PathVariable Long id,
            @RequestBody InventoryDto dto) {
        try {
            InventoryDto updated = inventoryService.updateInventory(id, dto);
            return ResponseEntity.ok(ApiResponse.success("更新成功", updated));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/stock-in")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<InventoryTransactionDto>> stockIn(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        try {
            BigDecimal quantity = new BigDecimal(String.valueOf(request.get("quantity")));
            BigDecimal unitPrice = new BigDecimal(String.valueOf(request.get("unitPrice")));
            String supplier = (String) request.get("supplier");
            String reason = (String) request.get("reason");
            String createdBy = authentication != null ? authentication.getName() : "system";
            
            InventoryTransactionDto transaction = inventoryService.stockIn(
                    id, quantity, unitPrice, supplier, reason, createdBy);
            return ResponseEntity.ok(ApiResponse.success("入库成功", transaction));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("入库失败: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/stock-out")
    @PreAuthorize("hasAnyRole('HOUSEKEEPING','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<InventoryTransactionDto>> stockOut(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        try {
            BigDecimal quantity = new BigDecimal(String.valueOf(request.get("quantity")));
            String reason = (String) request.get("reason");
            String referenceNumber = (String) request.get("referenceNumber");
            String createdBy = authentication != null ? authentication.getName() : "system";
            
            InventoryTransactionDto transaction = inventoryService.stockOut(
                    id, quantity, reason, referenceNumber, createdBy);
            return ResponseEntity.ok(ApiResponse.success("出库成功", transaction));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("出库失败: " + e.getMessage()));
        }
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('HOUSEKEEPING','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLowStockItems() {
        List<InventoryDto> items = inventoryService.getLowStockItems();
        
        // 增强预警展示
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("items", items);
        result.put("count", items.size());
        
        // 按紧急程度分类
        List<InventoryDto> urgent = new java.util.ArrayList<>();
        List<InventoryDto> warning = new java.util.ArrayList<>();
        
        for (InventoryDto item : items) {
            // 如果当前库存为0或接近0，标记为紧急
            if (item.getCurrentQuantity().compareTo(java.math.BigDecimal.valueOf(0.1)) <= 0) {
                urgent.add(item);
            } else {
                warning.add(item);
            }
        }
        
        result.put("urgent", urgent);
        result.put("warning", warning);
        result.put("urgentCount", urgent.size());
        result.put("warningCount", warning.size());
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}/transactions")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<List<InventoryTransactionDto>>> getTransactions(@PathVariable Long id) {
        List<InventoryTransactionDto> transactions = inventoryService.getTransactionsByInventoryId(id);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }
}

