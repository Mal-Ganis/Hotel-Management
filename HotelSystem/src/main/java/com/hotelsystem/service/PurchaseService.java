package com.hotelsystem.service;

import com.hotelsystem.entity.PurchaseOrder;
import com.hotelsystem.entity.Inventory;
import com.hotelsystem.repository.PurchaseOrderRepository;
import com.hotelsystem.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;

    public List<PurchaseOrder> getAllPurchaseOrders() {
        return purchaseOrderRepository.findAll();
    }

    public List<PurchaseOrder> getPurchaseOrdersByStatus(PurchaseOrder.PurchaseStatus status) {
        return purchaseOrderRepository.findByStatus(status);
    }

    public List<PurchaseOrder> getPurchaseOrdersByInventory(Long inventoryId) {
        return purchaseOrderRepository.findByInventoryId(inventoryId);
    }

    public Optional<PurchaseOrder> getPurchaseOrderById(Long id) {
        return purchaseOrderRepository.findById(id);
    }

    @Transactional
    public PurchaseOrder createPurchaseOrder(Long inventoryId, BigDecimal quantity, 
                                            BigDecimal unitPrice, String supplier, 
                                            String supplierContact, LocalDateTime expectedArrivalTime,
                                            String notes, String createdBy) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new RuntimeException("库存物品不存在"));

        PurchaseOrder order = new PurchaseOrder();
        order.setInventoryId(inventoryId);
        order.setItemName(inventory.getItemName());
        order.setQuantity(quantity);
        order.setUnitPrice(unitPrice);
        order.setTotalAmount(quantity.multiply(unitPrice));
        order.setSupplier(supplier);
        order.setSupplierContact(supplierContact);
        order.setExpectedArrivalTime(expectedArrivalTime);
        order.setStatus(PurchaseOrder.PurchaseStatus.PENDING);
        order.setNotes(notes);
        order.setCreatedBy(createdBy);

        return purchaseOrderRepository.save(order);
    }

    @Transactional
    public PurchaseOrder updatePurchaseOrderStatus(Long orderId, PurchaseOrder.PurchaseStatus status) {
        PurchaseOrder order = purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("采购单不存在"));

        order.setStatus(status);
        
        if (status == PurchaseOrder.PurchaseStatus.ARRIVED) {
            order.setActualArrivalTime(LocalDateTime.now());
        } else if (status == PurchaseOrder.PurchaseStatus.RECEIVED) {
            // 自动入库
            if (order.getReceivedQuantity() == null || order.getReceivedQuantity().compareTo(BigDecimal.ZERO) == 0) {
                order.setReceivedQuantity(order.getQuantity());
            }
            inventoryService.stockIn(
                    order.getInventoryId(),
                    order.getReceivedQuantity(),
                    order.getUnitPrice(),
                    order.getSupplier(),
                    "采购入库 - " + order.getOrderNumber(),
                    order.getCreatedBy()
            );
            order.setActualArrivalTime(LocalDateTime.now());
        }

        return purchaseOrderRepository.save(order);
    }

    @Transactional
    public PurchaseOrder receivePurchaseOrder(Long orderId, BigDecimal receivedQuantity) {
        PurchaseOrder order = purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("采购单不存在"));

        order.setReceivedQuantity(receivedQuantity);
        order.setActualArrivalTime(LocalDateTime.now());
        order.setStatus(PurchaseOrder.PurchaseStatus.ARRIVED);
        
        return purchaseOrderRepository.save(order);
    }

    @Transactional
    public void cancelPurchaseOrder(Long orderId) {
        PurchaseOrder order = purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("采购单不存在"));

        order.setStatus(PurchaseOrder.PurchaseStatus.CANCELLED);
        purchaseOrderRepository.save(order);
    }
}

