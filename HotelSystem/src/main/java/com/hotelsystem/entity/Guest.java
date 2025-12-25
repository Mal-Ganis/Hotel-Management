package com.hotelsystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "guests")
@Data
public class Guest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 50)
    private String fullName;

    @NotBlank
    @Size(max = 20)
    @Column(unique = true)
    private String idCardNumber; // 身份证号

    @Size(max = 20)
    private String phone;

    @Size(max = 100)
    private String password; // 用于线上登录（已加密）

    @Email
    @Size(max = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    private LocalDate dateOfBirth;

    @Size(max = 200)
    private String address;

    @Size(max = 100)
    private String preferences; // 偏好，如 "无烟,高楼层"

    @Size(max = 500)
    private String specialRequests; // 特殊要求

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

    // 性别枚举
    public enum Gender {
        MALE, FEMALE
    }
}
