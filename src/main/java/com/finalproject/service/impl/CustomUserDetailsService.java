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

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Boolean.TRUE.equals(user.getIsActive()), // enabled = true chỉ khi đã xác thực OTP
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}