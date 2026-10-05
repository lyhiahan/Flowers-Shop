package com.finalproject.config;

import com.finalproject.entity.User;
import com.finalproject.enums.Role;
import com.finalproject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Rà soát và tách bạch quyền tài khoản Quản trị viên (Admin) khi hệ thống khởi động:
 * 1. Đảm bảo mọi tài khoản Admin không còn chứa vai trò USER trong bảng user_roles.
 * 2. Bảo toàn tuyệt đối lịch sử đơn hàng, thông tin tài khoản và nhật ký giao dịch cũ.
 */
@Component
@Order(10)
@RequiredArgsConstructor
@Slf4j
public class AdminRoleSanitizer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        try {
            // 1. Dùng SQL trực tiếp làm sạch bảng user_roles đối với mọi tài khoản Admin
            int removedCount = jdbcTemplate.update(
                    "DELETE FROM user_roles WHERE role = 'USER' AND user_id IN (" +
                    "  SELECT id FROM users WHERE role = 'ADMIN'" +
                    ")"
            );
            if (removedCount > 0) {
                log.info("AdminRoleSanitizer: Đã thu hồi {} quyền USER gán nhầm cho tài khoản Quản trị viên trong database.", removedCount);
            }

            // 2. Rà soát lại entity User để đảm bảo cache JPA đồng bộ
            List<User> adminUsers = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == Role.ADMIN || (u.getRoles() != null && u.getRoles().contains(Role.ADMIN)))
                    .toList();

            for (User admin : adminUsers) {
                boolean changed = false;
                if (admin.getRoles() != null && admin.getRoles().contains(Role.USER)) {
                    admin.getRoles().remove(Role.USER);
                    changed = true;
                }
                if (admin.getRole() == null || admin.getRole() != Role.ADMIN) {
                    if ((admin.getRoles() != null && admin.getRoles().contains(Role.ADMIN))) {
                        admin.setRole(Role.ADMIN);
                        changed = true;
                    }
                }
                if (changed) {
                    userRepository.save(admin);
                    log.info("AdminRoleSanitizer: Đã làm sạch vai trò cho tài khoản Admin '{}'.", admin.getUsername());
                }
            }
        } catch (Exception ex) {
            log.warn("AdminRoleSanitizer: Không thể thực hiện tự động rà soát vai trò: {}", ex.getMessage());
        }
    }
}
