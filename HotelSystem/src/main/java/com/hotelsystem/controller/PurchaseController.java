package com.hotelsystem.controller;

import com.hotelsystem.dto.ApiResponse;
import com.hotelsystem.entity.PurchaseOrder;
import com.hotelsystem.service.PurchaseService;
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
@RequestMapping("/purchase")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<List<PurchaseOrder>>> getAllPurchaseOrders() {
        List<PurchaseOrder> orders = purchaseService.getAllPurchaseOrders();
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<List<PurchaseOrder>>> getPurchaseOrdersByStatus(
            @PathVariable PurchaseOrder.PurchaseStatus status) {
        List<PurchaseOrder> orders = purchaseService.getPurchaseOrdersByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<PurchaseOrder>> getPurchaseOrderById(@PathVariable Long id) {
        return purchaseService.getPurchaseOrderById(id)
                .map(order -> ResponseEntity.ok(ApiResponse.success(order)))
                .orElse(ResponseEntity.ok(ApiResponse.error("采购单不存在")));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<PurchaseOrder>> createPurchaseOrder(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        try {
            Long inventoryId = Long.valueOf(request.get("inventoryId").toString());
            BigDecimal quantity = new BigDecimal(request.get("quantity").toString());
            BigDecimal unitPrice = new BigDecimal(request.get("unitPrice").toString());
            String supplier = request.get("supplier") != null ? request.get("supplier").toString() : null;
            String supplierContact = request.get("supplierContact") != null ? 
                    request.get("supplierContact").toString() : null;
            LocalDateTime expectedArrivalTime = request.get("expectedArrivalTime") != null ?
                    LocalDateTime.parse(request.get("expectedArrivalTime").toString()) :
                    LocalDateTime.now().plusDays(7);
            String notes = request.get("notes") != null ? request.get("notes").toString() : null;
            String createdBy = authentication != null ? authentication.getName() : "system";

            PurchaseOrder order = purchaseService.createPurchaseOrder(
                    inventoryId, quantity, unitPrice, supplier, supplierContact,
                    expectedArrivalTime, notes, createdBy);
            return ResponseEntity.ok(ApiResponse.success("采购单创建成功", order));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/update-status")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<PurchaseOrder>> updatePurchaseOrderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            PurchaseOrder.PurchaseStatus status = PurchaseOrder.PurchaseStatus.valueOf(
                    request.get("status"));
            PurchaseOrder order = purchaseService.updatePurchaseOrderStatus(id, status);
            return ResponseEntity.ok(ApiResponse.success("状态更新成功", order));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<PurchaseOrder>> receivePurchaseOrder(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        try {
            BigDecimal receivedQuantity = new BigDecimal(request.get("receivedQuantity").toString());
            PurchaseOrder order = purchaseService.receivePurchaseOrder(id, receivedQuantity);
            return ResponseEntity.ok(ApiResponse.success("收货成功", order));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> cancelPurchaseOrder(@PathVariable Long id) {
        try {
            purchaseService.cancelPurchaseOrder(id);
            return ResponseEntity.ok(ApiResponse.success("采购单已取消", null));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }
}

