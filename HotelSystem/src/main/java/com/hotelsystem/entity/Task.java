package com.hotelsystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
@Data
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    private String title; // 任务标题

    @Size(max = 500)
    private String description; // 任务描述

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TaskType type; // 任务类型

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TaskStatus status = TaskStatus.PENDING; // 任务状态

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room; // 关联的房间（如果是房间相关任务）

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation; // 关联的预订（如果是预订相关任务）

    @Size(max = 50)
    private String assignedTo; // 分配给谁（用户username）

    @Column(name = "priority")
    private Integer priority = 5; // 优先级（1-10，数字越大优先级越高）

    @Column(name = "due_date")
    private LocalDateTime dueDate; // 截止时间

    @Column(name = "completed_at")
    private LocalDateTime completedAt; // 完成时间

    @Column(name = "created_by")
    private String createdBy; // 创建人

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

    // 任务类型枚举
    public enum TaskType {
        CLEANING,      // 清洁任务
        MAINTENANCE,   // 维修任务
        CHECK_IN,      // 入住任务
        CHECK_OUT,     // 退房任务
        INVENTORY,     // 库存任务
        OTHER          // 其他
    }

    // 任务状态枚举
    public enum TaskStatus {
        PENDING,    // 待处理
        IN_PROGRESS, // 进行中
        COMPLETED,  // 已完成
        CANCELLED   // 已取消
    }
}

