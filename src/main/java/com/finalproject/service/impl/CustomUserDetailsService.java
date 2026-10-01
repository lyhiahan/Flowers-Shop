package com.finalproject.service.impl;

import com.finalproject.entity.User;
import com.finalproject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String input) throws UsernameNotFoundException {
        if (input == null || input.trim().isEmpty()) {
            throw new UsernameNotFoundException("Vui lòng nhập email, số điện thoại hoặc tên đăng nhập");
        }
        String cleanInput = input.trim();
        User user = userRepository.findByUsername(cleanInput)
                .or(() -> userRepository.findByEmail(cleanInput))
                .or(() -> userRepository.findByPhone(cleanInput))
                .orElseThrow(() -> new UsernameNotFoundException("Email, số điện thoại hoặc mật khẩu không chính xác: " + cleanInput));

        java.util.Set<org.springframework.security.core.GrantedAuthority> authorities = new java.util.HashSet<>();
        // Mọi người dùng đang hoạt động luôn giữ quyền Khách hàng để mua hoa và sử dụng tính năng thông thường
        authorities.add(new SimpleGrantedAuthority("ROLE_CUSTOMER"));

        if (user.getRole() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        }
        if (user.getRoles() != null) {
            for (com.finalproject.enums.Role r : user.getRoles()) {
                if (r != null) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + r.name()));
                }
            }
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Boolean.TRUE.equals(user.getIsActive()), // enabled = true chỉ khi đã xác thực OTP
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked
                authorities
        );
    }
}