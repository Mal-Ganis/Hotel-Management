package com.hotelsystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory")
@Data
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(unique = true)
    private String itemName; // 物品名称，如 "毛巾"、"洗发水"、"矿泉水"

    @Size(max = 50)
    private String category; // 分类，如 "布草"、"洗浴品"、"饮品"、"清洁用品"

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal currentQuantity; // 当前库存数量

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal safetyThreshold; // 安全库存阈值

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal unitCost; // 当前单位成本（移动加权平均）

    @Size(max = 50)
    private String unit; // 单位，如 "条"、"瓶"、"包"

    @Size(max = 200)
    private String description; // 物品描述

    @Column(name = "is_active")
    private Boolean isActive = true; // 是否启用

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

