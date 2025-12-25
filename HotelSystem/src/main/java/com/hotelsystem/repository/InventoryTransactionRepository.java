package com.hotelsystem.repository;

import com.hotelsystem.entity.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
    List<InventoryTransaction> findByInventoryId(Long inventoryId);
    List<InventoryTransaction> findByType(InventoryTransaction.TransactionType type);
    List<InventoryTransaction> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}

