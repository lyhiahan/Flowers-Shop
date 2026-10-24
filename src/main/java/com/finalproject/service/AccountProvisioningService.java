package com.finalproject.service;

import com.finalproject.entity.User;
import com.finalproject.enums.Role;
import com.finalproject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountProvisioningService {
    private final UserRepository users;
    private final PasswordEncoder passwords;

    @Transactional
    public User create(String actor, String username, String email, String fullName, String phone,
                       String password, String role) {
        User admin = users.findByUsername(actor).orElseThrow(() -> new AccessDeniedException("Vui lòng đăng nhập."));
        if (!admin.hasRole(Role.ADMIN) || !Boolean.TRUE.equals(admin.getIsActive())
                || Boolean.TRUE.equals(admin.getAccountDisabledByAdmin()) || admin.isRetiredDeliveryAccount()) {
            throw new AccessDeniedException("Chỉ quản trị viên được tạo tài khoản.");
        }
        String name = username == null ? "" : username.trim();
        String mail = email == null ? "" : email.trim().toLowerCase(java.util.Locale.ROOT);
        String display = fullName == null ? "" : fullName.trim();
        String number = phone == null ? "" : phone.trim();
        if (!name.matches("[a-zA-Z0-9_.-]{3,50}")) throw new IllegalArgumentException("Tên đăng nhập phải có 3–50 ký tự chữ, số, dấu chấm, gạch dưới hoặc gạch ngang.");
        if (mail.length() > 100 || !mail.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) throw new IllegalArgumentException("Email không hợp lệ.");
        if (display.isBlank() || display.length() > 100) throw new IllegalArgumentException("Họ tên bắt buộc nhập, tối đa 100 ký tự.");
        if (!number.isEmpty() && !number.matches("0[0-9]{9}")) throw new IllegalArgumentException("Số điện thoại phải gồm 10 chữ số, bắt đầu bằng 0.");
        if (password == null || password.isBlank() || password.length() < 8
                || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 8 ký tự, tối đa 72 byte.");
        }
        if (!"USER".equals(role) && !"MANAGER".equals(role) && !"STAFF".equals(role)) throw new IllegalArgumentException("Chỉ được tạo tài khoản Khách hàng, Quản lý chi nhánh hoặc Nhân viên cửa hàng.");
        if (users.existsByUsername(name) || users.existsByEmail(mail)) throw new IllegalArgumentException("Tên đăng nhập hoặc email đã được sử dụng.");
        // Database unique constraints also reject simultaneous duplicate submissions.
        return users.saveAndFlush(User.builder().username(name).email(mail).fullName(display).phone(number.isEmpty() ? null : number)
                .password(passwords.encode(password)).role(Role.valueOf(role)).isActive(true).isEmailActive(true).build());
    }
}
