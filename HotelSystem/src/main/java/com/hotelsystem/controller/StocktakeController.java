package com.hotelsystem.controller;

import com.hotelsystem.dto.ApiResponse;
import com.hotelsystem.entity.InventoryStocktake;
import com.hotelsystem.service.StocktakeService;
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
@RequestMapping("/stocktake")
@RequiredArgsConstructor
public class StocktakeController {

    private final StocktakeService stocktakeService;

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<List<InventoryStocktake>>> getAllStocktakes() {
        List<InventoryStocktake> stocktakes = stocktakeService.getAllStocktakes();
        return ResponseEntity.ok(ApiResponse.success(stocktakes));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<List<InventoryStocktake>>> getStocktakesByStatus(
            @PathVariable InventoryStocktake.StocktakeStatus status) {
        List<InventoryStocktake> stocktakes = stocktakeService.getStocktakesByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(stocktakes));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<InventoryStocktake>> getStocktakeById(@PathVariable Long id) {
        return stocktakeService.getStocktakeById(id)
                .map(stocktake -> ResponseEntity.ok(ApiResponse.success(stocktake)))
                .orElse(ResponseEntity.ok(ApiResponse.error("盘点任务不存在")));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<InventoryStocktake>> createStocktake(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        try {
            Long inventoryId = Long.valueOf(request.get("inventoryId").toString());
            LocalDateTime stocktakeDate = request.get("stocktakeDate") != null ?
                    LocalDateTime.parse(request.get("stocktakeDate").toString()) :
                    LocalDateTime.now();
            String operator = authentication != null ? authentication.getName() : "system";

            InventoryStocktake stocktake = stocktakeService.createStocktake(
                    inventoryId, stocktakeDate, operator);
            return ResponseEntity.ok(ApiResponse.success("盘点任务创建成功", stocktake));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<InventoryStocktake>> completeStocktake(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        try {
            BigDecimal actualQuantity = new BigDecimal(request.get("actualQuantity").toString());
            String reason = request.get("reason") != null ? request.get("reason").toString() : null;
            String notes = request.get("notes") != null ? request.get("notes").toString() : null;

            InventoryStocktake stocktake = stocktakeService.completeStocktake(
                    id, actualQuantity, reason, notes);
            return ResponseEntity.ok(ApiResponse.success("盘点完成", stocktake));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/adjust")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<InventoryStocktake>> adjustInventory(@PathVariable Long id) {
        try {
            InventoryStocktake stocktake = stocktakeService.adjustInventory(id);
            return ResponseEntity.ok(ApiResponse.success("库存已调整", stocktake));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }
}

