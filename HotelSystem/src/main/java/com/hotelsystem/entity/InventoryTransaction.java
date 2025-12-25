package com.hotelsystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_transactions")
@Data
public class InventoryTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id")
    private Inventory inventory; // 关联的库存物品

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TransactionType type; // 交易类型：IN（入库）、OUT（出库）

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal quantity; // 数量

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal unitPrice; // 单价（入库时是采购价，出库时是当前成本价）

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal totalAmount; // 总金额

    @Size(max = 200)
    private String supplier; // 供应商（入库时使用）

    @Size(max = 200)
    private String reason; // 原因/备注（如 "采购入库"、"清洁消耗"、"报损"）

    @Size(max = 100)
    private String referenceNumber; // 关联单号（如采购单号、房间号）

    @Column(name = "created_by")
    private String createdBy; // 操作人

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // 交易类型枚举
    public enum TransactionType {
        IN,      // 入库
        OUT      // 出库
    }
}

