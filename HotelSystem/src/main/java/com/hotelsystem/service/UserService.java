package com.hotelsystem.service;

import com.hotelsystem.dto.UserDto;
import com.hotelsystem.entity.User;
import com.hotelsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 获取所有用户
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserDto::fromEntity)
                .collect(Collectors.toList());
    }

    // 根据ID获取用户
    public Optional<UserDto> getUserById(Long id) {
        return userRepository.findById(id)
                .map(UserDto::fromEntity);
    }

    // 创建用户
    public UserDto createUser(UserDto userDto) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(userDto.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }

        // 检查邮箱是否已存在
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new RuntimeException("邮箱已存在");
        }

        User user = userDto.toEntity();
        // 对密码进行加密
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);
        return UserDto.fromEntity(savedUser);
    }

    @com.hotelsystem.audit.Auditable(action = "CREATE_USER")
    public UserDto createUserAudited(UserDto userDto) {
        return createUser(userDto);
    }

    // 更新用户
    public UserDto updateUser(Long id, UserDto userDto) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 检查用户名是否被其他用户使用
        if (!existingUser.getUsername().equals(userDto.getUsername()) &&
                userRepository.existsByUsername(userDto.getUsername())) {
            throw new RuntimeException("用户名已被其他用户使用");
        }

        // 检查邮箱是否被其他用户使用
        if (!existingUser.getEmail().equals(userDto.getEmail()) &&
                userRepository.existsByEmail(userDto.getEmail())) {
            throw new RuntimeException("邮箱已被其他用户使用");
        }

        existingUser.setUsername(userDto.getUsername());
        existingUser.setFullName(userDto.getFullName());
        existingUser.setEmail(userDto.getEmail());
        existingUser.setPhone(userDto.getPhone());
        existingUser.setRole(userDto.getRole());
        existingUser.setIsActive(userDto.getIsActive());
        // 如果更新了密码，则加密
        if (userDto.getPassword() != null && !userDto.getPassword().isBlank()) {
            existingUser.setPassword(passwordEncoder.encode(userDto.getPassword()));
        }
        
        // 如果更新了密保问题，则更新
        if (userDto.getSecurityQuestion() != null) {
            existingUser.setSecurityQuestion(userDto.getSecurityQuestion());
        }
        
        // 如果更新了密保答案，则加密存储
        if (userDto.getSecurityAnswer() != null && !userDto.getSecurityAnswer().isBlank()) {
            existingUser.setSecurityAnswer(passwordEncoder.encode(userDto.getSecurityAnswer()));
        }

        User updatedUser = userRepository.save(existingUser);
        return UserDto.fromEntity(updatedUser);
    }
    
    // 更新当前用户的密码（需要验证旧密码）
    public void updateCurrentUserPassword(String username, String oldPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }
        
        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
    
    // 更新当前用户的密保（需要验证密码）
    public void updateCurrentUserSecurity(String username, String password, String securityQuestion, String securityAnswer) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        
        // 更新密保
        user.setSecurityQuestion(securityQuestion);
        user.setSecurityAnswer(passwordEncoder.encode(securityAnswer));
        userRepository.save(user);
    }

    @com.hotelsystem.audit.Auditable(action = "UPDATE_USER")
    public UserDto updateUserAudited(Long id, UserDto userDto) {
        return updateUser(id, userDto);
    }

    // 删除用户
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("用户不存在");
        }
        userRepository.deleteById(id);
    }

    @com.hotelsystem.audit.Auditable(action = "DELETE_USER")
    public void deleteUserAudited(Long id) {
        deleteUser(id);
    }

    // 根据用户名查找用户
    public Optional<UserDto> getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(UserDto::fromEntity);
    }
}
