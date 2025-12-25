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
            // 若不是系统用户，则尝试作为宾客（通过 email 登录）
            .or(() -> {
                return guestRepository.findByEmail(username).map(guest -> {
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
                });
            })
            .orElseThrow(() -> new UsernameNotFoundException("用户未找到: " + username));
    }
}
