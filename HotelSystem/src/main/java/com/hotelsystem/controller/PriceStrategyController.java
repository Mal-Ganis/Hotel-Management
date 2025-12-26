package com.hotelsystem.controller;

import com.hotelsystem.dto.ApiResponse;
import com.hotelsystem.entity.PriceStrategy;
import com.hotelsystem.service.PriceStrategyService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/price-strategies")
@RequiredArgsConstructor
public class PriceStrategyController {

    private final PriceStrategyService priceStrategyService;

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<List<PriceStrategy>>> getAllPriceStrategies() {
        List<PriceStrategy> strategies = priceStrategyService.getAllPriceStrategies();
        return ResponseEntity.ok(ApiResponse.success(strategies));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<PriceStrategy>>> getActivePriceStrategies() {
        List<PriceStrategy> strategies = priceStrategyService.getActivePriceStrategies();
        return ResponseEntity.ok(ApiResponse.success(strategies));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<PriceStrategy>> getPriceStrategyById(@PathVariable Long id) {
        return priceStrategyService.getPriceStrategyById(id)
                .map(strategy -> ResponseEntity.ok(ApiResponse.success(strategy)))
                .orElse(ResponseEntity.ok(ApiResponse.error("价格策略不存在")));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<PriceStrategy>> createPriceStrategy(@RequestBody PriceStrategy strategy) {
        try {
            PriceStrategy created = priceStrategyService.createPriceStrategy(strategy);
            return ResponseEntity.ok(ApiResponse.success("价格策略创建成功", created));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<PriceStrategy>> updatePriceStrategy(
            @PathVariable Long id,
            @RequestBody PriceStrategy strategy) {
        try {
            PriceStrategy updated = priceStrategyService.updatePriceStrategy(id, strategy);
            return ResponseEntity.ok(ApiResponse.success("价格策略更新成功", updated));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePriceStrategy(@PathVariable Long id) {
        try {
            priceStrategyService.deletePriceStrategy(id);
            return ResponseEntity.ok(ApiResponse.success("价格策略删除成功", null));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 计算房间在指定日期的实际价格
     */
    @GetMapping("/calculate-price")
    public ResponseEntity<ApiResponse<Map<String, Object>>> calculateRoomPrice(
            @RequestParam Long roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            BigDecimal price = priceStrategyService.calculateRoomPrice(roomId, date);
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("roomId", roomId);
            result.put("date", date);
            result.put("calculatedPrice", price);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 批量调整价格
     */
    @PostMapping("/batch-update")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> batchUpdatePrices(
            @RequestBody Map<String, Object> request) {
        try {
            String roomType = request.get("roomType").toString();
            BigDecimal newPrice = new BigDecimal(request.get("newPrice").toString());
            priceStrategyService.batchUpdatePrices(roomType, newPrice);
            
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("roomType", roomType);
            result.put("newPrice", newPrice);
            result.put("message", "批量价格更新成功");
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }
}

