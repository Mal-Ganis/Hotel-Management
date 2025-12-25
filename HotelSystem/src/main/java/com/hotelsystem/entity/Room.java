package com.hotelsystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rooms")
@Data
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @NotBlank
    @Size(max = 20)
    @Column(unique = true)
    private String roomNumber; // 房间号，如 "101", "201A"

    @NotBlank
    @Size(max = 50)
    private String roomType; // 房型，如 "标准大床房", "豪华双床房"

    @Size(max = 200)
    private String description; // 房间描述

    @DecimalMin("0.00")
    private BigDecimal price; // 房间价格

    private Integer capacity; // 可住人数

    @Size(max = 100)
    private String amenities; // 设施，如 "WiFi,空调,电视"

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RoomStatus status = RoomStatus.AVAILABLE; // 房间状态

    @Column(name = "is_active")
    private Boolean isActive = true; // 是否可用

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

    // 房间状态枚举
    public enum RoomStatus {
        AVAILABLE,      // 空闲
        OCCUPIED,       // 已入住
        RESERVED,       // 已预订
        CLEANING,       // 清洁中
        MAINTENANCE     // 维修中
    }
}
