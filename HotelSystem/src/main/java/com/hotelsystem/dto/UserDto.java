package com.hotelsystem.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hotelsystem.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserDto {
    private Long id;

    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名长度不能超过50个字符")
    private String username;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    // 注意：密码在创建时必填，在更新时可选
    // 使用 @Size 而不是 @NotBlank，这样空值不会触发验证
    // 创建时的验证在 Controller 或 Service 层手动检查
    @Size(min = 6, message = "密码长度至少6个字符")
    private String password;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 50, message = "姓名长度不能超过50个字符")
    private String fullName;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;

    @Size(max = 20, message = "手机号长度不能超过20个字符")
    private String phone;

    private User.UserRole role;
    private Boolean isActive;
    private LocalDateTime createdAt;

    // 转换实体到DTO
    public static UserDto fromEntity(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setRole(user.getRole());
        dto.setIsActive(user.getIsActive());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }

    // 转换DTO到实体
    public User toEntity() {
        User user = new User();
        user.setUsername(this.username);
        user.setPassword(this.password); // 注意：实际使用中需要加密
        user.setFullName(this.fullName);
        user.setEmail(this.email);
        user.setPhone(this.phone);
        user.setRole(this.role != null ? this.role : User.UserRole.RECEPTIONIST);
        user.setIsActive(this.isActive != null ? this.isActive : true);
        return user;
    }
}
