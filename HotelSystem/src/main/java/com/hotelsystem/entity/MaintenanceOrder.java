package com.hotelsystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "maintenance_orders")
@Data
public class MaintenanceOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 50)
    private String orderNumber; // 工单号

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @NotBlank
    @Size(max = 200)
    private String problemDescription; // 问题描述

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UrgencyLevel urgency = UrgencyLevel.MEDIUM; // 紧急程度

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MaintenanceStatus status = MaintenanceStatus.PENDING; // 状态

    @Size(max = 50)
    private String assignedTo; // 分配给谁（维修人员）

    @Column(name = "estimated_completion_time")
    private LocalDateTime estimatedCompletionTime; // 预计完成时间

    @Column(name = "actual_completion_time")
    private LocalDateTime actualCompletionTime; // 实际完成时间

    private BigDecimal cost; // 维修成本

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
            orderNumber = "MT" + System.currentTimeMillis();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum UrgencyLevel {
        LOW,      // 低
        MEDIUM,   // 中
        HIGH,     // 高
        URGENT    // 紧急
    }

    public enum MaintenanceStatus {
        PENDING,    // 待处理
        IN_PROGRESS, // 处理中
        COMPLETED,   // 已完成
        CANCELLED    // 已取消
    }
}

