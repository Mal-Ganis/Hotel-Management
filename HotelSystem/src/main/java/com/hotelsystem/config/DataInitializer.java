package com.hotelsystem.config;

import com.hotelsystem.entity.User;
import com.hotelsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 创建默认管理员用户（如果不存在）
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFullName("系统管理员");
            admin.setEmail("admin@hotel.com");
            admin.setPhone("13800138000");
            admin.setRole(User.UserRole.ADMIN);
            admin.setIsActive(true);

            userRepository.save(admin);
            System.out.println("默认管理员用户已创建 - 用户名: admin, 密码: admin123");
        }
    }
}
