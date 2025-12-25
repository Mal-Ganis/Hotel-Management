package com.hotelsystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "guest_preferences")
@Data
public class GuestPreference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id")
    private Guest guest; // 关联的宾客

    @NotBlank
    @Size(max = 50)
    private String preferenceType; // 偏好类型，如 "房型偏好"、"楼层偏好"、"设施偏好"

    @NotBlank
    @Size(max = 200)
    private String preferenceValue; // 偏好值，如 "豪华双床房"、"高楼层"、"无烟房"

    @Size(max = 500)
    private String description; // 偏好描述

    @Column(name = "frequency")
    private Integer frequency = 1; // 出现频率（用于推荐排序）

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt; // 最后使用时间

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (lastUsedAt == null) {
            lastUsedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

