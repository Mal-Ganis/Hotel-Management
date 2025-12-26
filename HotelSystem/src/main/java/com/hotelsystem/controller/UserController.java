package com.hotelsystem.controller;

import com.hotelsystem.dto.ApiResponse;
import com.hotelsystem.dto.UserDto;
import com.hotelsystem.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import java.util.Map;

import java.util.List;

@RestController
@RequestMapping("/users")  // 改为 /users
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 获取所有用户 - 仅管理员可访问
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserDto>>> getAllUsers() {
        List<UserDto> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    // 根据ID获取用户 - 仅管理员可访问
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(user -> ResponseEntity.ok(ApiResponse.success(user)))
                .orElse(ResponseEntity.ok(ApiResponse.error("用户不存在")));
    }

    // 创建用户 - 仅管理员可访问
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDto>> createUser(@Valid @RequestBody UserDto userDto) {
        try {
            // 手动验证密码（因为移除了@NotBlank，允许更新时为空）
            if (userDto.getPassword() == null || userDto.getPassword().trim().isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error("密码不能为空"));
            }
            // 使用带审计的方法
            UserDto createdUser = userService.createUserAudited(userDto);
            return ResponseEntity.ok(ApiResponse.success("用户创建成功", createdUser));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    // 更新用户 - 仅管理员可访问
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserDto userDto) {
        try {
            UserDto updatedUser = userService.updateUserAudited(id, userDto);
            return ResponseEntity.ok(ApiResponse.success("用户更新成功", updatedUser));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    // 删除用户
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUserAudited(id);
            return ResponseEntity.ok(ApiResponse.success("用户删除成功", null));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }
    
    // ========== 个人中心相关接口 ==========
    
    // 获取当前登录用户信息
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECEPTIONIST','HOUSEKEEPING')")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        return userService.getUserByUsername(username)
                .map(user -> ResponseEntity.ok(ApiResponse.success(user)))
                .orElse(ResponseEntity.ok(ApiResponse.error("用户不存在")));
    }
    
    // 更新当前用户密码
    @PostMapping("/me/password")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECEPTIONIST','HOUSEKEEPING')")
    public ResponseEntity<ApiResponse<Void>> updateCurrentUserPassword(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            String oldPassword = request.get("oldPassword");
            String newPassword = request.get("newPassword");
            
            if (oldPassword == null || newPassword == null) {
                return ResponseEntity.ok(ApiResponse.error("参数不完整"));
            }
            
            if (newPassword.length() < 6) {
                return ResponseEntity.ok(ApiResponse.error("新密码长度至少6个字符"));
            }
            
            userService.updateCurrentUserPassword(username, oldPassword, newPassword);
            return ResponseEntity.ok(ApiResponse.success("密码修改成功", null));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }
    
    // 更新当前用户密保
    @PostMapping("/me/security")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECEPTIONIST','HOUSEKEEPING')")
    public ResponseEntity<ApiResponse<Void>> updateCurrentUserSecurity(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            String password = request.get("password");
            String securityQuestion = request.get("securityQuestion");
            String securityAnswer = request.get("securityAnswer");
            
            if (password == null || securityQuestion == null || securityAnswer == null) {
                return ResponseEntity.ok(ApiResponse.error("参数不完整"));
            }
            
            userService.updateCurrentUserSecurity(username, password, securityQuestion, securityAnswer);
            return ResponseEntity.ok(ApiResponse.success("密保修改成功", null));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }
}
