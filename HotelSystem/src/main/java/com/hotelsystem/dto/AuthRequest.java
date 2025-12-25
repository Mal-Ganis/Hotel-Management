package com.hotelsystem.dto;

import lombok.Data;

@Data
public class AuthRequest {
    private String username;
    private String password;
    private String role; // 用户选择的角色，用于验证
}
