package com.finalproject.service.impl;

import com.finalproject.entity.User;
import com.finalproject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String input) throws UsernameNotFoundException {
        if (input == null || input.trim().isEmpty()) {
            throw new UsernameNotFoundException("Vui lòng nhập email hoặc tên đăng nhập");
        }
        String cleanInput = input.trim();
        User user = userRepository.findByUsername(cleanInput)
                .or(() -> userRepository.findByEmail(cleanInput))
                .orElseThrow(() -> new UsernameNotFoundException("Email, tên đăng nhập hoặc mật khẩu không chính xác: " + cleanInput));

        boolean isAdmin = (user.getRole() == com.finalproject.enums.Role.ADMIN)
                || (user.getRoles() != null && user.getRoles().contains(com.finalproject.enums.Role.ADMIN));

        java.util.Set<org.springframework.security.core.GrantedAuthority> authorities = new java.util.HashSet<>();

        if (isAdmin) {
            // ADMIN: Không được tự động cấp ROLE_USER (Tách bạch quản trị và mua hàng)
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        } else {
            // Người dùng thông thường (Customer, Vendor, Manager) giữ quyền Khách hàng
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
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
        }

        return new com.finalproject.security.SessionUserDetails(
                user.getUsername(),
                user.getPassword(),
                Boolean.TRUE.equals(user.getIsActive()) && !Boolean.TRUE.equals(user.getAccountDisabledByAdmin())
                        && !user.isRetiredDeliveryAccount(),
                authorities
        );
    }
}
