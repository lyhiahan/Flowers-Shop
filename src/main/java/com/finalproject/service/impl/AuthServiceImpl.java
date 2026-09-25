package com.finalproject.service.impl;

import com.finalproject.dto.request.RegisterRequest;
import com.finalproject.entity.Cart;
import com.finalproject.entity.OtpVerification;
import com.finalproject.entity.User;
import com.finalproject.enums.OtpType;
import com.finalproject.enums.Role;
import com.finalproject.repository.CartRepository;
import com.finalproject.repository.OtpVerificationRepository;
import com.finalproject.repository.UserRepository;
import com.finalproject.service.AuthService;
import com.finalproject.service.EmailService;
import com.finalproject.utils.OtpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final OtpVerificationRepository otpRepository;
    private final CartRepository cartRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void registerCustomer(RegisterRequest request) {
        // 1. Kiểm tra trùng username, email hoặc số điện thoại
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Thông tin không hợp lệ: Tên đăng nhập đã tồn tại trong hệ thống!");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Thông tin không hợp lệ: Email đăng ký đã tồn tại trong hệ thống!");
        }
        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()
                && userRepository.existsByPhone(request.getPhone().trim())) {
            throw new IllegalArgumentException("Thông tin không hợp lệ: Số điện thoại đăng ký đã tồn tại trong hệ thống!");
        }

        // 2. Tạo User ở trạng thái chưa kích hoạt (isActive = false)
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .role(Role.CUSTOMER)
                .isActive(false)
                .createdDate(LocalDate.now())
                .build();
        userRepository.save(user);

        // 3. Khởi tạo giỏ hàng riêng cho khách
        Cart cart = Cart.builder()
                .user(user)
                .build();
        cartRepository.save(cart);

        // 4. Tạo và gửi mã OTP kích hoạt qua email
        createAndSendOtp(user, OtpType.ACTIVATE_ACCOUNT);
    }

    @Override
    @Transactional
    public boolean verifyOtp(String email, String otpCode) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với email: " + email));

        OtpVerification otp = otpRepository
                .findTopByUserIdAndCodeAndTypeAndIsUsedFalseOrderByExpiredAtDesc(
                        user.getId(), otpCode, OtpType.ACTIVATE_ACCOUNT)
                .orElse(null);

        // Kiểm tra OTP có tồn tại và còn hạn (120s) hay không
        if (otp == null || otp.getExpiredAt().isBefore(LocalDateTime.now())) {
            return false;
        }

        // Đánh dấu mã đã sử dụng
        otp.setIsUsed(true);
        otpRepository.save(otp);

        // Kích hoạt tài khoản
        user.setIsActive(true);
        userRepository.save(user);

        return true;
    }

    @Override
    @Transactional
    public void resendOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản với email: " + email));

        if (Boolean.TRUE.equals(user.getIsActive())) {
            throw new IllegalStateException("Tài khoản này đã được kích hoạt trước đó!");
        }

        // Giới hạn chống spam: tối đa 3 lần trong vòng 10 phút
        LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);
        long recentRequests = otpRepository.countByUserIdAndCreatedAtAfter(user.getId(), tenMinutesAgo);
        if (recentRequests >= 3) {
            throw new IllegalStateException("Bạn đã yêu cầu gửi mã quá 3 lần. Vui lòng thử lại sau 10 phút.");
        }

        createAndSendOtp(user, OtpType.ACTIVATE_ACCOUNT);
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản với email: " + email));

        createAndSendOtp(user, OtpType.RESET_PASSWORD);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verifyForgotPasswordOtp(String email, String otpCode) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với email: " + email));

        OtpVerification otp = otpRepository
                .findTopByUserIdAndCodeAndTypeAndIsUsedFalseOrderByExpiredAtDesc(
                        user.getId(), otpCode, OtpType.RESET_PASSWORD)
                .orElse(null);

        return otp != null && !otp.getExpiredAt().isBefore(LocalDateTime.now());
    }

    @Override
    @Transactional
    public void resetPassword(String email, String otpCode, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản với email: " + email));

        OtpVerification otp = otpRepository
                .findTopByUserIdAndCodeAndTypeAndIsUsedFalseOrderByExpiredAtDesc(
                        user.getId(), otpCode, OtpType.RESET_PASSWORD)
                .orElseThrow(() -> new IllegalArgumentException("Mã OTP không hợp lệ!"));

        if (otp.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Mã OTP đã hết hạn! Vui lòng yêu cầu mã mới.");
        }

        otp.setIsUsed(true);
        otpRepository.save(otp);

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private void createAndSendOtp(User user, OtpType type) {
        String code = OtpUtil.generateOtp();

        OtpVerification otp = OtpVerification.builder()
                .user(user)
                .code(code)
                .type(type)
                .expiredAt(LocalDateTime.now().plusSeconds(120))
                .createdAt(LocalDateTime.now())
                .isUsed(false)
                .build();
        otpRepository.save(otp);

        emailService.sendOtpEmail(user.getEmail(), code);
    }
}