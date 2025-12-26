package com.hotelsystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_stocktake")
@Data
public class InventoryStocktake {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 50)
    private String taskNumber; // 盘点任务号

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id")
    private Inventory inventory;

    @NotNull
    @Column(name = "system_quantity")
    private BigDecimal systemQuantity; // 系统库存数量

    @NotNull
    @Column(name = "actual_quantity")
    private BigDecimal actualQuantity; // 实际盘点数量

    @Column(name = "difference")
    private BigDecimal difference; // 差异（实际 - 系统）

    @Size(max = 200)
    private String reason; // 差异原因

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private StocktakeStatus status = StocktakeStatus.PENDING; // 状态

    @Column(name = "stocktake_date")
    private LocalDateTime stocktakeDate; // 盘点日期

    @Size(max = 50)
    private String operator; // 操作人

    @Size(max = 500)
    private String notes; // 备注

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (taskNumber == null) {
            taskNumber = "ST" + System.currentTimeMillis();
        }
        if (stocktakeDate == null) {
            stocktakeDate = LocalDateTime.now();
        }
        createdAt = LocalDateTime.now();
    }

    public enum StocktakeStatus {
        PENDING,    // 待处理
        COMPLETED,  // 已完成
        ADJUSTED    // 已调整
    }
}

