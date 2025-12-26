package com.hotelsystem.service;

import com.hotelsystem.entity.InventoryStocktake;
import com.hotelsystem.entity.Inventory;
import com.hotelsystem.repository.InventoryStocktakeRepository;
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
public class StocktakeService {

    private final InventoryStocktakeRepository stocktakeRepository;
    private final InventoryRepository inventoryRepository;

    public List<InventoryStocktake> getAllStocktakes() {
        return stocktakeRepository.findAll();
    }

    public List<InventoryStocktake> getStocktakesByStatus(InventoryStocktake.StocktakeStatus status) {
        return stocktakeRepository.findByStatus(status);
    }

    public List<InventoryStocktake> getStocktakesByInventory(Long inventoryId) {
        return stocktakeRepository.findByInventoryId(inventoryId);
    }

    public Optional<InventoryStocktake> getStocktakeById(Long id) {
        return stocktakeRepository.findById(id);
    }

    @Transactional
    public InventoryStocktake createStocktake(Long inventoryId, LocalDateTime stocktakeDate, String operator) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new RuntimeException("库存物品不存在"));

        InventoryStocktake stocktake = new InventoryStocktake();
        stocktake.setInventory(inventory);
        stocktake.setSystemQuantity(inventory.getCurrentQuantity());
        stocktake.setActualQuantity(BigDecimal.ZERO); // 待录入
        stocktake.setDifference(BigDecimal.ZERO);
        stocktake.setStatus(InventoryStocktake.StocktakeStatus.PENDING);
        stocktake.setStocktakeDate(stocktakeDate != null ? stocktakeDate : LocalDateTime.now());
        stocktake.setOperator(operator);

        return stocktakeRepository.save(stocktake);
    }

    @Transactional
    public InventoryStocktake completeStocktake(Long stocktakeId, BigDecimal actualQuantity, String reason, String notes) {
        InventoryStocktake stocktake = stocktakeRepository.findById(stocktakeId)
                .orElseThrow(() -> new RuntimeException("盘点任务不存在"));

        stocktake.setActualQuantity(actualQuantity);
        stocktake.setDifference(actualQuantity.subtract(stocktake.getSystemQuantity()));
        stocktake.setReason(reason);
        stocktake.setNotes(notes);
        stocktake.setStatus(InventoryStocktake.StocktakeStatus.COMPLETED);

        return stocktakeRepository.save(stocktake);
    }

    @Transactional
    public InventoryStocktake adjustInventory(Long stocktakeId) {
        InventoryStocktake stocktake = stocktakeRepository.findById(stocktakeId)
                .orElseThrow(() -> new RuntimeException("盘点任务不存在"));

        if (stocktake.getStatus() != InventoryStocktake.StocktakeStatus.COMPLETED) {
            throw new RuntimeException("只有已完成的盘点任务才能调整库存");
        }

        Inventory inventory = stocktake.getInventory();
        inventory.setCurrentQuantity(stocktake.getActualQuantity());
        inventoryRepository.save(inventory);

        stocktake.setStatus(InventoryStocktake.StocktakeStatus.ADJUSTED);
        return stocktakeRepository.save(stocktake);
    }
}

