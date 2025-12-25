package com.hotelsystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
@Data
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 50)
    private String reservationNumber; // 预订号，可自动生成

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id")
    private Guest guest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @NotNull
    private LocalDate checkInDate; // 入住日期

    @NotNull
    private LocalDate checkOutDate; // 离店日期

    private Integer numberOfGuests; // 入住人数

    @Size(max = 50)
    private String preferredRoomType; // 客户首选的房型（用于自动分配）

    private BigDecimal totalAmount; // 总金额

    private BigDecimal paidAmount; // 已付金额

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ReservationStatus status = ReservationStatus.CONFIRMED; // 预订状态

    @Size(max = 500)
    private String specialRequests; // 特殊要求

    @Column(name = "created_by")
    private String createdBy; // 创建预订的员工

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (reservationNumber == null) {
            reservationNumber = "RSV" + System.currentTimeMillis();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // 预订状态枚举
    public enum ReservationStatus {
        PENDING,      // 待确认
        CONFIRMED,    // 已确认
        CHECKED_IN,   // 已入住
        CHECKED_OUT,  // 已离店
        CANCELLED     // 已取消
    }
}