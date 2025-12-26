package com.hotelsystem.repository;

import com.hotelsystem.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    List<PurchaseOrder> findByStatus(PurchaseOrder.PurchaseStatus status);
    List<PurchaseOrder> findByInventoryId(Long inventoryId);
}

