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
@Table(name = "pos_consumptions")
@Data
public class PosConsumption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation; // 关联的预订

    @NotBlank
    @Size(max = 100)
    private String itemName; // 消费项目名称，如 "早餐"、"矿泉水"、"洗衣服务"

    @Size(max = 50)
    private String category; // 消费分类，如 "餐饮"、"服务"、"商品"

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal quantity; // 数量

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal unitPrice; // 单价

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal totalAmount; // 总金额

    @Size(max = 200)
    private String description; // 消费描述

    @Column(name = "consumption_date")
    private LocalDateTime consumptionDate; // 消费时间

    @Column(name = "created_by")
    private String createdBy; // 录入人

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (consumptionDate == null) {
            consumptionDate = LocalDateTime.now();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

