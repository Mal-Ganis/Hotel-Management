package com.hotelsystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservation_history")
@Data
public class ReservationHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    @NotBlank
    @Column(length = 50)
    private String action; // 操作类型：CREATED, PAID, STATUS_CHANGED, MODIFIED, CANCELLED

    @Column(length = 100)
    private String oldValue; // 旧值

    @Column(length = 100)
    private String newValue; // 新值

    @Column(length = 500)
    private String description; // 操作描述

    @Column(length = 50)
    private String operator; // 操作人

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

