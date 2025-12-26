package com.hotelsystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 50)
    @Column(unique = true)
    private String username;

    @NotBlank
    @Size(max = 100)
    private String password;

    @NotBlank
    @Size(max = 50)
    private String fullName;

    @Email
    @Size(max = 100)
    @Column(unique = true)
    private String email;

    @Size(max = 20)
    private String phone;

    @Size(max = 200)
    @Column(name = "security_question")
    private String securityQuestion; // 密保问题

    @Size(max = 200)
    @Column(name = "security_answer")
    private String securityAnswer; // 密保答案（加密存储）

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UserRole role = UserRole.MANAGER; // 默认角色为经理

    @Column(name = "is_active")
    private Boolean isActive = true;

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

    public enum UserRole {
        ADMIN,       // 管理员
        MANAGER,     // 经理
        RECEPTIONIST, // 前台
        HOUSEKEEPING // 房务
    }
}