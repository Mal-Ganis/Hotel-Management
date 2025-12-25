package com.hotelsystem.dto;

import com.hotelsystem.entity.PosConsumption;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PosConsumptionDto {
    private Long id;
    private Long reservationId;
    private String itemName;
    private String category;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private String description;
    private LocalDateTime consumptionDate;
    private String createdBy;
    private LocalDateTime createdAt;

    public static PosConsumptionDto fromEntity(PosConsumption consumption) {
        PosConsumptionDto dto = new PosConsumptionDto();
        dto.setId(consumption.getId());
        dto.setReservationId(consumption.getReservation().getId());
        dto.setItemName(consumption.getItemName());
        dto.setCategory(consumption.getCategory());
        dto.setQuantity(consumption.getQuantity());
        dto.setUnitPrice(consumption.getUnitPrice());
        dto.setTotalAmount(consumption.getTotalAmount());
        dto.setDescription(consumption.getDescription());
        dto.setConsumptionDate(consumption.getConsumptionDate());
        dto.setCreatedBy(consumption.getCreatedBy());
        dto.setCreatedAt(consumption.getCreatedAt());
        return dto;
    }

    public PosConsumption toEntity() {
        PosConsumption consumption = new PosConsumption();
        consumption.setItemName(this.itemName);
        consumption.setCategory(this.category);
        consumption.setQuantity(this.quantity);
        consumption.setUnitPrice(this.unitPrice);
        consumption.setTotalAmount(this.totalAmount);
        consumption.setDescription(this.description);
        consumption.setConsumptionDate(this.consumptionDate);
        return consumption;
    }
}

