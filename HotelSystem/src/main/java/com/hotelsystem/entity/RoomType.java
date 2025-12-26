package com.hotelsystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "room_types")
@Data
public class RoomType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 50)
    @Column(unique = true)
    private String name; // 房型名称，如 "标准大床房"

    @Size(max = 500)
    private String description; // 房型描述

    @Column(name = "base_price")
    private BigDecimal basePrice; // 基础价格

    private Integer capacity; // 可住人数

    @Size(max = 500)
    private String amenities; // 设施清单（JSON格式或逗号分隔）

    @Size(max = 500)
    private String photos; // 照片URL列表（JSON格式）

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

