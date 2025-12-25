package com.hotelsystem.dto;

import com.hotelsystem.entity.InventoryTransaction;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class InventoryTransactionDto {
    private Long id;
    private Long inventoryId;
    private String inventoryName;
    private InventoryTransaction.TransactionType type;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private String supplier;
    private String reason;
    private String referenceNumber;
    private String createdBy;
    private LocalDateTime createdAt;

    public static InventoryTransactionDto fromEntity(InventoryTransaction transaction) {
        InventoryTransactionDto dto = new InventoryTransactionDto();
        dto.setId(transaction.getId());
        dto.setInventoryId(transaction.getInventory().getId());
        dto.setInventoryName(transaction.getInventory().getItemName());
        dto.setType(transaction.getType());
        dto.setQuantity(transaction.getQuantity());
        dto.setUnitPrice(transaction.getUnitPrice());
        dto.setTotalAmount(transaction.getTotalAmount());
        dto.setSupplier(transaction.getSupplier());
        dto.setReason(transaction.getReason());
        dto.setReferenceNumber(transaction.getReferenceNumber());
        dto.setCreatedBy(transaction.getCreatedBy());
        dto.setCreatedAt(transaction.getCreatedAt());
        return dto;
    }
}

