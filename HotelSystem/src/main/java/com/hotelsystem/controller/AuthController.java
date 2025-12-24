package com.hotelsystem.controller;

import com.hotelsystem.dto.AuthRequest;
import com.hotelsystem.dto.AuthResponse;
import com.hotelsystem.dto.UserDto;
import com.hotelsystem.dto.GuestDto;
import com.hotelsystem.entity.User;
import com.hotelsystem.security.JwtUtil;
import com.hotelsystem.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final UserService userService;
    private final com.hotelsystem.service.GuestService guestService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
            
            // 验证角色：如果请求中指定了角色，需要验证用户的实际角色是否匹配
            if (request.getRole() != null && !request.getRole().isEmpty()) {
                // 从数据库中获取用户的实际角色（更准确）
                var userOpt = userService.getUserByUsername(request.getUsername());
                if (userOpt.isEmpty()) {
                    return ResponseEntity.ok(com.hotelsystem.dto.ApiResponse.error("用户不存在"));
                }
                
                User.UserRole actualRole = userOpt.get().getRole();
                String requestedRole = request.getRole().toUpperCase();
                
                // 如果请求的角色与实际角色不匹配，拒绝登录
                if (actualRole == null || !actualRole.name().equals(requestedRole)) {
                    String actualRoleName = actualRole != null ? actualRole.name() : "未知";
                    return ResponseEntity.ok(com.hotelsystem.dto.ApiResponse.error(
                            "角色不匹配：您的实际角色是 " + getRoleDisplayName(actualRoleName) + 
                            "，但选择了 " + getRoleDisplayName(requestedRole) + "。请选择正确的角色登录。"));
                }
            }
            
            String token = jwtUtil.generateToken(userDetails);
            
            // 返回统一格式的响应
            com.hotelsystem.dto.AuthResponse authResponse = new AuthResponse(token);
            return ResponseEntity.ok(com.hotelsystem.dto.ApiResponse.success("登录成功", authResponse));
        } catch (Exception e) {
            return ResponseEntity.ok(com.hotelsystem.dto.ApiResponse.error("登录失败: " + e.getMessage()));
        }
    }
    
    private String getRoleDisplayName(String role) {
        switch (role) {
            case "ADMIN": return "管理员";
            case "MANAGER": return "经理";
            case "RECEPTIONIST": return "前台";
            case "HOUSEKEEPING": return "房务";
            default: return role;
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerGuest(@Valid @RequestBody GuestDto guestDto) {
        try {
            GuestDto created = guestService.createGuest(guestDto);
            return ResponseEntity.ok(created);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}
