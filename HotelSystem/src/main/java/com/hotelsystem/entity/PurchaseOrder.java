package com.hotelsystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "purchase_orders")
@Data
public class PurchaseOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 50)
    private String orderNumber; // 采购单号

    @NotBlank
    @Size(max = 100)
    private String itemName; // 物品名称

    @NotNull
    @Column(name = "inventory_id")
    private Long inventoryId; // 关联的库存物品ID

    @NotNull
    @Column(name = "quantity")
    private BigDecimal quantity; // 采购数量

    @Column(name = "unit_price")
    private BigDecimal unitPrice; // 单价

    @Column(name = "total_amount")
    private BigDecimal totalAmount; // 总金额

    @Size(max = 100)
    private String supplier; // 供应商

    @Size(max = 50)
    private String supplierContact; // 供应商联系方式

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PurchaseStatus status = PurchaseStatus.PENDING; // 状态

    @Column(name = "expected_arrival_time")
    private LocalDateTime expectedArrivalTime; // 预计到货时间

    @Column(name = "actual_arrival_time")
    private LocalDateTime actualArrivalTime; // 实际到货时间

    @Column(name = "received_quantity")
    private BigDecimal receivedQuantity; // 已收货数量

    @Size(max = 500)
    private String notes; // 备注

    @Size(max = 50)
    private String createdBy; // 创建人

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (orderNumber == null) {
            orderNumber = "PO" + System.currentTimeMillis();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum PurchaseStatus {
        PENDING,      // 待采购
        ORDERED,      // 已下单
        ARRIVED,      // 已到货
        RECEIVED,     // 已入库
        CANCELLED     // 已取消
    }
}

