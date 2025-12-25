package com.hotelsystem.repository;

import com.hotelsystem.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByItemName(String itemName);
    boolean existsByItemName(String itemName);
    List<Inventory> findByCategory(String category);
    List<Inventory> findByIsActiveTrue();
    List<Inventory> findByCurrentQuantityLessThanEqual(BigDecimal threshold);
}

