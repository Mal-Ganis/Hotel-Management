package com.hotelsystem.controller;

import com.hotelsystem.dto.AuthRequest;
import com.hotelsystem.dto.AuthResponse;
import com.hotelsystem.dto.UserDto;
import com.hotelsystem.dto.GuestDto;
import com.hotelsystem.entity.User;
import com.hotelsystem.security.JwtUtil;
import com.hotelsystem.service.UserService;
import com.hotelsystem.repository.GuestRepository;
import com.hotelsystem.entity.Guest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final UserService userService;
    private final com.hotelsystem.service.GuestService guestService;
    private final GuestRepository guestRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
            
            // 验证角色：如果请求中指定了角色，需要验证用户的实际角色是否匹配
            if (request.getRole() != null && !request.getRole().isEmpty()) {
                String requestedRole = request.getRole().toUpperCase();
                
                // 检查是否为GUEST角色
                if ("GUEST".equals(requestedRole)) {
                    // 对于GUEST，检查是否为宾客
                    var guestOpt = guestRepository.findByEmail(request.getUsername());
                    if (guestOpt.isEmpty()) {
                        // 尝试通过姓名查找
                        var guests = guestRepository.findByFullNameContainingIgnoreCase(request.getUsername());
                        if (guests.isEmpty()) {
                            return ResponseEntity.ok(com.hotelsystem.dto.ApiResponse.error("用户不存在"));
                        }
                    }
                    // GUEST角色验证通过
                } else {
                    // 对于员工角色，从User表验证
                    var userOpt = userService.getUserByUsername(request.getUsername());
                    if (userOpt.isEmpty()) {
                        return ResponseEntity.ok(com.hotelsystem.dto.ApiResponse.error("用户不存在"));
                    }
                    
                    User.UserRole actualRole = userOpt.get().getRole();
                    
                    // 如果请求的角色与实际角色不匹配，拒绝登录
                    if (actualRole == null || !actualRole.name().equals(requestedRole)) {
                        String actualRoleName = actualRole != null ? actualRole.name() : "未知";
                        return ResponseEntity.ok(com.hotelsystem.dto.ApiResponse.error(
                                "角色不匹配：您的实际角色是 " + getRoleDisplayName(actualRoleName) + 
                                "，但选择了 " + getRoleDisplayName(requestedRole) + "。请选择正确的角色登录。"));
                    }
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
            return ResponseEntity.ok(com.hotelsystem.dto.ApiResponse.success("注册成功", created));
        } catch (RuntimeException ex) {
            return ResponseEntity.ok(com.hotelsystem.dto.ApiResponse.error(ex.getMessage()));
        }
    }

    // 验证密保问题
    @PostMapping("/verify-security-question")
    public ResponseEntity<?> verifySecurityQuestion(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String securityAnswer = request.get("securityAnswer");

        if (email == null || securityAnswer == null) {
            return ResponseEntity.ok(com.hotelsystem.dto.ApiResponse.error("邮箱和密保答案不能为空"));
        }

        Guest guest = guestRepository.findByEmail(email)
                .orElse(null);

        if (guest == null) {
            return ResponseEntity.ok(com.hotelsystem.dto.ApiResponse.error("用户不存在"));
        }

        if (guest.getSecurityQuestion() == null || guest.getSecurityAnswer() == null) {
            return ResponseEntity.ok(com.hotelsystem.dto.ApiResponse.error("该用户未设置密保问题"));
        }

        // 验证密保答案
        if (passwordEncoder.matches(securityAnswer, guest.getSecurityAnswer())) {
            return ResponseEntity.ok(com.hotelsystem.dto.ApiResponse.success("验证成功", null));
        } else {
            return ResponseEntity.ok(com.hotelsystem.dto.ApiResponse.error("密保答案错误"));
        }
    }

    // 获取密保问题
    @GetMapping("/security-question")
    public ResponseEntity<?> getSecurityQuestion(@RequestParam String email) {
        Guest guest = guestRepository.findByEmail(email)
                .orElse(null);

        if (guest == null) {
            return ResponseEntity.ok(com.hotelsystem.dto.ApiResponse.error("用户不存在"));
        }

        if (guest.getSecurityQuestion() == null) {
            return ResponseEntity.ok(com.hotelsystem.dto.ApiResponse.error("该用户未设置密保问题"));
        }

        return ResponseEntity.ok(com.hotelsystem.dto.ApiResponse.success("获取成功", 
                Map.of("securityQuestion", guest.getSecurityQuestion())));
    }

    // 重置密码
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String securityAnswer = request.get("securityAnswer");
        String newPassword = request.get("newPassword");

        if (email == null || securityAnswer == null || newPassword == null) {
            return ResponseEntity.ok(com.hotelsystem.dto.ApiResponse.error("参数不完整"));
        }

        if (newPassword.length() < 6) {
            return ResponseEntity.ok(com.hotelsystem.dto.ApiResponse.error("密码长度至少6个字符"));
        }

        Guest guest = guestRepository.findByEmail(email)
                .orElse(null);

        if (guest == null) {
            return ResponseEntity.ok(com.hotelsystem.dto.ApiResponse.error("用户不存在"));
        }

        if (guest.getSecurityAnswer() == null) {
            return ResponseEntity.ok(com.hotelsystem.dto.ApiResponse.error("该用户未设置密保问题"));
        }

        // 验证密保答案
        if (!passwordEncoder.matches(securityAnswer, guest.getSecurityAnswer())) {
            return ResponseEntity.ok(com.hotelsystem.dto.ApiResponse.error("密保答案错误"));
        }

        // 更新密码
        guest.setPassword(passwordEncoder.encode(newPassword));
        guestRepository.save(guest);

        return ResponseEntity.ok(com.hotelsystem.dto.ApiResponse.success("密码重置成功", null));
    }
}
