package com.hotelsystem.dto;

import com.hotelsystem.entity.Inventory;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class InventoryDto {
    private Long id;
    private String itemName;
    private String category;
    private BigDecimal currentQuantity;
    private BigDecimal safetyThreshold;
    private BigDecimal unitCost;
    private String unit;
    private String description;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static InventoryDto fromEntity(Inventory inventory) {
        InventoryDto dto = new InventoryDto();
        dto.setId(inventory.getId());
        dto.setItemName(inventory.getItemName());
        dto.setCategory(inventory.getCategory());
        dto.setCurrentQuantity(inventory.getCurrentQuantity());
        dto.setSafetyThreshold(inventory.getSafetyThreshold());
        dto.setUnitCost(inventory.getUnitCost());
        dto.setUnit(inventory.getUnit());
        dto.setDescription(inventory.getDescription());
        dto.setIsActive(inventory.getIsActive());
        dto.setCreatedAt(inventory.getCreatedAt());
        dto.setUpdatedAt(inventory.getUpdatedAt());
        return dto;
    }

    public Inventory toEntity() {
        Inventory inventory = new Inventory();
        inventory.setItemName(this.itemName);
        inventory.setCategory(this.category);
        inventory.setCurrentQuantity(this.currentQuantity);
        inventory.setSafetyThreshold(this.safetyThreshold);
        inventory.setUnitCost(this.unitCost);
        inventory.setUnit(this.unit);
        inventory.setDescription(this.description);
        inventory.setIsActive(this.isActive);
        return inventory;
    }
}

