package com.hotelsystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    private String title; // 通知标题

    @Size(max = 500)
    private String content; // 通知内容

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private NotificationType type; // 通知类型

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private NotificationStatus status = NotificationStatus.UNREAD; // 通知状态

    @Size(max = 50)
    private String recipientType; // 接收者类型：GUEST、USER

    @Size(max = 100)
    private String recipientId; // 接收者ID（宾客ID或用户ID）

    @Size(max = 200)
    private String relatedEntityType; // 关联实体类型：RESERVATION、INVENTORY、TASK

    private Long relatedEntityId; // 关联实体ID

    @Column(name = "read_at")
    private LocalDateTime readAt; // 阅读时间

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // 通知类型枚举
    public enum NotificationType {
        RESERVATION_CONFIRMED,  // 预订确认
        RESERVATION_CANCELLED,  // 预订取消
        CHECK_IN_REMINDER,      // 入住提醒
        CHECK_OUT_REMINDER,    // 退房提醒
        INVENTORY_ALERT,        // 库存预警
        TASK_ASSIGNED,          // 任务分配
        PAYMENT_SUCCESS,        // 支付成功
        PAYMENT_FAILED          // 支付失败
    }

    // 通知状态枚举
    public enum NotificationStatus {
        UNREAD,  // 未读
        READ     // 已读
    }
}

