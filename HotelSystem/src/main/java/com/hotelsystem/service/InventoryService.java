package com.hotelsystem.service;

import com.hotelsystem.dto.InventoryDto;
import com.hotelsystem.dto.InventoryTransactionDto;
import com.hotelsystem.entity.Inventory;
import com.hotelsystem.entity.InventoryTransaction;
import com.hotelsystem.repository.InventoryRepository;
import com.hotelsystem.repository.InventoryTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository transactionRepository;

    public List<InventoryDto> getAllInventories() {
        return inventoryRepository.findAll().stream()
                .map(InventoryDto::fromEntity)
                .collect(Collectors.toList());
    }

    public Optional<InventoryDto> getInventoryById(Long id) {
        return inventoryRepository.findById(id)
                .map(InventoryDto::fromEntity);
    }

    public InventoryDto createInventory(InventoryDto dto) {
        if (inventoryRepository.existsByItemName(dto.getItemName())) {
            throw new RuntimeException("物品名称已存在");
        }
        Inventory inventory = dto.toEntity();
        if (inventory.getCurrentQuantity() == null) {
            inventory.setCurrentQuantity(BigDecimal.ZERO);
        }
        if (inventory.getUnitCost() == null) {
            inventory.setUnitCost(BigDecimal.ZERO);
        }
        Inventory saved = inventoryRepository.save(inventory);
        return InventoryDto.fromEntity(saved);
    }

    public InventoryDto updateInventory(Long id, InventoryDto dto) {
        Inventory existing = inventoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("库存物品不存在"));
        
        if (!existing.getItemName().equals(dto.getItemName()) &&
                inventoryRepository.existsByItemName(dto.getItemName())) {
            throw new RuntimeException("物品名称已被其他物品使用");
        }

        existing.setItemName(dto.getItemName());
        existing.setCategory(dto.getCategory());
        existing.setSafetyThreshold(dto.getSafetyThreshold());
        existing.setUnit(dto.getUnit());
        existing.setDescription(dto.getDescription());
        existing.setIsActive(dto.getIsActive());
        
        Inventory updated = inventoryRepository.save(existing);
        return InventoryDto.fromEntity(updated);
    }

    @Transactional
    public InventoryTransactionDto stockIn(Long inventoryId, BigDecimal quantity, BigDecimal unitPrice, 
                                           String supplier, String reason, String createdBy) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new RuntimeException("库存物品不存在"));

        // 计算新的加权平均成本
        BigDecimal currentQty = inventory.getCurrentQuantity();
        BigDecimal currentCost = inventory.getUnitCost();
        BigDecimal newQty = currentQty.add(quantity);
        
        BigDecimal newCost;
        if (newQty.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal totalValue = currentQty.multiply(currentCost).add(quantity.multiply(unitPrice));
            newCost = totalValue.divide(newQty, 2, RoundingMode.HALF_UP);
        } else {
            newCost = unitPrice;
        }

        // 更新库存
        inventory.setCurrentQuantity(newQty);
        inventory.setUnitCost(newCost);
        inventoryRepository.save(inventory);

        // 创建入库交易记录
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setInventory(inventory);
        transaction.setType(InventoryTransaction.TransactionType.IN);
        transaction.setQuantity(quantity);
        transaction.setUnitPrice(unitPrice);
        transaction.setTotalAmount(quantity.multiply(unitPrice));
        transaction.setSupplier(supplier);
        transaction.setReason(reason);
        transaction.setCreatedBy(createdBy);
        
        InventoryTransaction saved = transactionRepository.save(transaction);
        return InventoryTransactionDto.fromEntity(saved);
    }

    @Transactional
    public InventoryTransactionDto stockOut(Long inventoryId, BigDecimal quantity, String reason, 
                                          String referenceNumber, String createdBy) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new RuntimeException("库存物品不存在"));

        if (inventory.getCurrentQuantity().compareTo(quantity) < 0) {
            throw new RuntimeException("库存不足，当前库存: " + inventory.getCurrentQuantity());
        }

        // 更新库存
        BigDecimal newQty = inventory.getCurrentQuantity().subtract(quantity);
        inventory.setCurrentQuantity(newQty);
        inventoryRepository.save(inventory);

        // 创建出库交易记录（使用当前成本价）
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setInventory(inventory);
        transaction.setType(InventoryTransaction.TransactionType.OUT);
        transaction.setQuantity(quantity);
        transaction.setUnitPrice(inventory.getUnitCost());
        transaction.setTotalAmount(quantity.multiply(inventory.getUnitCost()));
        transaction.setReason(reason);
        transaction.setReferenceNumber(referenceNumber);
        transaction.setCreatedBy(createdBy);
        
        InventoryTransaction saved = transactionRepository.save(transaction);
        return InventoryTransactionDto.fromEntity(saved);
    }

    public List<InventoryDto> getLowStockItems() {
        return inventoryRepository.findAll().stream()
                .filter(inv -> inv.getCurrentQuantity().compareTo(inv.getSafetyThreshold()) <= 0)
                .map(InventoryDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<InventoryTransactionDto> getTransactionsByInventoryId(Long inventoryId) {
        return transactionRepository.findByInventoryId(inventoryId).stream()
                .map(InventoryTransactionDto::fromEntity)
                .collect(Collectors.toList());
    }
}

