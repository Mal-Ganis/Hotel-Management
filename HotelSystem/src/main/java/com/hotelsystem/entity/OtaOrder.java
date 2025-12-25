package com.hotelsystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "ota_orders")
@Data
public class OtaOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(unique = true)
    private String otaOrderId; // OTA平台订单ID

    @Size(max = 50)
    private String otaPlatform; // OTA平台名称，如 "携程"、"美团"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation; // 关联的内部预订

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SyncStatus syncStatus = SyncStatus.PENDING; // 同步状态

    @Column(name = "sync_at")
    private LocalDateTime syncAt; // 同步时间

    @Size(max = 500)
    private String syncMessage; // 同步消息/错误信息

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

    // 同步状态枚举
    public enum SyncStatus {
        PENDING,    // 待同步
        SUCCESS,    // 同步成功
        FAILED,     // 同步失败
        CANCELLED   // 已取消
    }
}

