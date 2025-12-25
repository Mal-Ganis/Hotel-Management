package com.hotelsystem.security;

import com.hotelsystem.entity.User;
import com.hotelsystem.entity.Guest;
import com.hotelsystem.repository.GuestRepository;
import com.hotelsystem.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final GuestRepository guestRepository;

    public CustomUserDetailsService(UserRepository userRepository, GuestRepository guestRepository) {
        this.userRepository = userRepository;
        this.guestRepository = guestRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 先查系统用户
        return userRepository.findByUsername(username)
            .map(user -> {
                Collection<GrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
                );
                return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
                    .password(user.getPassword())
                    .authorities(authorities)
                    .accountExpired(false)
                    .accountLocked(false)
                    .credentialsExpired(false)
                    .disabled(!Boolean.TRUE.equals(user.getIsActive()))
                    .build();
            })
            // 若不是系统用户，则尝试作为宾客（通过 email 或 fullName 登录）
            .or(() -> {
                // 先尝试通过邮箱查找
                return guestRepository.findByEmail(username)
                    .map(guest -> {
                        Collection<GrantedAuthority> authorities = Collections.singletonList(
                            new SimpleGrantedAuthority("ROLE_GUEST")
                        );
                        return org.springframework.security.core.userdetails.User.withUsername(guest.getEmail())
                            .password(guest.getPassword() != null ? guest.getPassword() : "")
                            .authorities(authorities)
                            .accountExpired(false)
                            .accountLocked(false)
                            .credentialsExpired(false)
                            .disabled(false)
                            .build();
                    })
                    // 如果邮箱找不到，尝试通过姓名查找
                    .or(() -> {
                        var guests = guestRepository.findByFullNameContainingIgnoreCase(username);
                        if (!guests.isEmpty()) {
                            // 如果找到多个同名用户，使用第一个（实际应用中可能需要更精确的匹配）
                            var guest = guests.get(0);
                            Collection<GrantedAuthority> authorities = Collections.singletonList(
                                new SimpleGrantedAuthority("ROLE_GUEST")
                            );
                            return Optional.of(org.springframework.security.core.userdetails.User.withUsername(guest.getEmail())
                                .password(guest.getPassword() != null ? guest.getPassword() : "")
                                .authorities(authorities)
                                .accountExpired(false)
                                .accountLocked(false)
                                .credentialsExpired(false)
                                .disabled(false)
                                .build());
                        }
                        return Optional.empty();
                    });
            })
            .orElseThrow(() -> new UsernameNotFoundException("用户未找到: " + username));
    }
}
