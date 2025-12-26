package com.hotelsystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "room_check_inspections")
@Data
public class RoomCheckInspection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @Column(name = "facilities_ok")
    private Boolean facilitiesOk = true; // 设施完好

    @Column(name = "has_damage")
    private Boolean hasDamage = false; // 是否有损坏

    @Column(length = 500)
    private String damageDescription; // 损坏描述

    @Column(length = 500)
    private String damagePhotos; // 损坏照片URL（JSON格式）

    @Column(name = "items_left_behind")
    private Boolean itemsLeftBehind = false; // 是否有遗留物品

    @Column(length = 500)
    private String itemsDescription; // 遗留物品描述

    @Column(name = "inspection_completed")
    private Boolean inspectionCompleted = false; // 检查是否完成

    @Column(length = 50)
    private String inspector; // 检查人

    @Column(name = "inspected_at")
    private LocalDateTime inspectedAt; // 检查时间

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

