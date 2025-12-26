package com.hotelsystem.repository;

import com.hotelsystem.entity.InventoryStocktake;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryStocktakeRepository extends JpaRepository<InventoryStocktake, Long> {
    List<InventoryStocktake> findByStatus(InventoryStocktake.StocktakeStatus status);
    List<InventoryStocktake> findByInventoryId(Long inventoryId);
}

