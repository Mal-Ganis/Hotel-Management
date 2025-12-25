package com.hotelsystem.config;

import com.hotelsystem.entity.User;
import com.hotelsystem.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

@Component
public class InitialDataLoader implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.initial-admin.username:admin}")
    private String adminUsername;

    @Value("${app.initial-admin.password:admin123}")
    private String adminPassword;

    public InitialDataLoader(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        boolean exists = userRepository.findByUsername(adminUsername).isPresent();
        if (!exists) {
            User admin = new User();
            admin.setUsername(adminUsername);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setFullName("Initial Admin");
            admin.setEmail(adminUsername + "@example.com");
            admin.setRole(User.UserRole.ADMIN);
            admin.setIsActive(true);
            userRepository.save(admin);
            System.out.println("Created initial admin user: " + adminUsername);
        }
    }
}
