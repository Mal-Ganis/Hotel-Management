package com.hotelsystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Entity
@Table(name = "room_standard_consumptions")
@Data
public class RoomStandardConsumption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room; // 关联的房间

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id")
    private Inventory inventory; // 关联的库存物品

    @NotNull
    @DecimalMin("0.00")
    private Double standardQuantity; // 标准消耗数量（每次清洁的标准用量）

    @Size(max = 200)
    private String description; // 说明
}

