package com.hotelsystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "waitlist")
@Data
public class Waitlist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id")
    private Guest guest;

    @NotNull
    private LocalDate desiredCheckInDate;

    @NotNull
    private LocalDate desiredCheckOutDate;

    @Size(max = 50)
    private String preferredRoomType; // 房型偏好

    @Size(max = 20)
    private String phone; // 联系方式

    @Size(max = 100)
    private String email; // 邮箱

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private WaitlistStatus status = WaitlistStatus.PENDING; // 状态

    @Column(name = "notified_at")
    private LocalDateTime notifiedAt; // 通知时间

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

    public enum WaitlistStatus {
        PENDING,    // 待处理
        NOTIFIED,  // 已通知
        CONVERTED, // 已转为预订
        CANCELLED  // 已取消
    }
}

