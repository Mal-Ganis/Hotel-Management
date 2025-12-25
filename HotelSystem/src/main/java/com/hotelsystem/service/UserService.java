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

        User updatedUser = userRepository.save(existingUser);
        return UserDto.fromEntity(updatedUser);
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
