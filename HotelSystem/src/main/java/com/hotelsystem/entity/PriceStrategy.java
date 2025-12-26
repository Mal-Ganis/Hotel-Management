package com.hotelsystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "price_strategies")
@Data
public class PriceStrategy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(length = 100)
    private String name; // 策略名称，如 "节假日价格"、"促销价格"

    @Column(length = 50)
    private String roomType; // 适用的房型（null表示所有房型）

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private StrategyType type; // 策略类型

    @Column(name = "start_date")
    private LocalDate startDate; // 生效开始日期

    @Column(name = "end_date")
    private LocalDate endDate; // 生效结束日期

    @Column(name = "base_price")
    private BigDecimal basePrice; // 基础价格（固定价格策略）

    @Column(name = "discount_rate")
    private BigDecimal discountRate; // 折扣率（0-1，如0.8表示8折）

    @Column(name = "price_adjustment")
    private BigDecimal priceAdjustment; // 价格调整（正数表示加价，负数表示减价）

    @Column(name = "dynamic_factor")
    private BigDecimal dynamicFactor; // 动态因子（用于动态价格策略）

    @Column(length = 500)
    private String description; // 策略描述

    @Column(name = "is_active")
    private Boolean isActive = true; // 是否启用

    @Column(name = "priority")
    private Integer priority = 0; // 优先级（数字越大优先级越高）

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

    public enum StrategyType {
        FIXED,      // 固定价格
        DISCOUNT,   // 折扣价格
        ADJUSTMENT, // 价格调整
        DYNAMIC     // 动态价格（根据入住率等调整）
    }
}

