package com.finalproject.service.impl;

import com.finalproject.dto.PendingRegistration;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final OtpVerificationRepository otpRepository;
    private final CartRepository cartRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    // Lưu tạm thông tin đăng ký chờ xác thực OTP trong bộ nhớ, CHƯA LƯU VÀO DATABASE để tránh tài khoản rác
    private final Map<String, PendingRegistration> pendingRegistrations = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public void registerCustomer(RegisterRequest request) {
        // 1. Kiểm tra trùng username, email hoặc số điện thoại trong Database (Exception Flow: 8b)
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Thông tin tài khoản không hợp lệ, tên đăng nhập đã tồn tại!");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Thông tin tài khoản không hợp lệ, email đăng ký đã tồn tại!");
        }
        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()
                && userRepository.existsByPhone(request.getPhone().trim())) {
            throw new IllegalArgumentException("Thông tin tài khoản không hợp lệ, số điện thoại đăng ký đã tồn tại!");
        }

        // 2. Tạo mã OTP và lưu tạm vào bộ nhớ (CHƯA LƯU VÀO DATABASE)
        String code = OtpUtil.generateOtp();
        LocalDateTime now = LocalDateTime.now();
        PendingRegistration pending = PendingRegistration.builder()
                .request(request)
                .otpCode(code)
                .createdAt(now)
                .expiredAt(now.plusMinutes(5))
                .resendCount(0)
                .build();

        pendingRegistrations.put(request.getEmail().toLowerCase().trim(), pending);

        // 3. Gửi mã OTP xác thực qua email (và in ra console)
        emailService.sendOtpEmail(request.getEmail(), code);
    }

    @Override
    @Transactional
    public boolean verifyOtp(String email, String otpCode) {
        String key = email.toLowerCase().trim();
        PendingRegistration pending = pendingRegistrations.get(key);

        if (pending != null) {
            // Kiểm tra mã OTP và thời hạn hiệu lực (5 phút)
            if (pending.getExpiredAt().isBefore(LocalDateTime.now())) {
                pendingRegistrations.remove(key);
                return false;
            }
            if (!pending.getOtpCode().equals(otpCode)) {
                return false;
            }

            // KHI VÀ CHỈ KHI NGƯỜI DÙNG NHẬP ĐÚNG OTP -> BÂY GIỜ MỚI CHÍNH THỨC TẠO VÀ LƯU VÀO DATABASE
            RegisterRequest req = pending.getRequest();

            if (userRepository.existsByUsername(req.getUsername()) || userRepository.existsByEmail(req.getEmail())) {
                pendingRegistrations.remove(key);
                throw new IllegalArgumentException("Tài khoản hoặc email này đã tồn tại trong hệ thống!");
            }

            User user = User.builder()
                    .username(req.getUsername())
                    .password(passwordEncoder.encode(req.getPassword()))
                    .fullName(req.getFullName())
                    .email(req.getEmail())
                    .phone(req.getPhone())
                    .address(req.getAddress())
                    .role(Role.CUSTOMER)
                    .isActive(true)
                    .createdDate(LocalDate.now())
                    .build();
            userRepository.save(user);

            // Khởi tạo giỏ hàng cho tài khoản vừa được kích hoạt
            Cart cart = Cart.builder()
                    .user(user)
                    .build();
            cartRepository.save(cart);

            // Lưu bản ghi xác thực OTP vào database
            OtpVerification otp = OtpVerification.builder()
                    .user(user)
                    .code(otpCode)
                    .type(OtpType.ACTIVATE_ACCOUNT)
                    .createdAt(pending.getCreatedAt())
                    .expiredAt(pending.getExpiredAt())
                    .isUsed(true)
                    .build();
            otpRepository.save(otp);

            // Xóa khỏi danh sách chờ sau khi đăng ký thành công
            pendingRegistrations.remove(key);
            return true;
        }

        // Hỗ trợ trường hợp tài khoản đã tồn tại ở DB nhưng chưa kích hoạt
        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            OtpVerification otp = otpRepository
                    .findTopByUserIdAndCodeAndTypeAndIsUsedFalseOrderByExpiredAtDesc(
                            user.getId(), otpCode, OtpType.ACTIVATE_ACCOUNT)
                    .orElse(null);

            if (otp == null || otp.getExpiredAt().isBefore(LocalDateTime.now())) {
                return false;
            }

            otp.setIsUsed(true);
            otpRepository.save(otp);

            user.setIsActive(true);
            userRepository.save(user);
            return true;
        }

        return false;
    }

    @Override
    @Transactional
    public void resendOtp(String email) {
        String key = email.toLowerCase().trim();
        PendingRegistration pending = pendingRegistrations.get(key);

        if (pending != null) {
            if (pending.getResendCount() >= 3) {
                throw new IllegalStateException("Bạn đã yêu cầu gửi mã quá 3 lần. Vui lòng đăng ký lại.");
            }
            String newCode = OtpUtil.generateOtp();
            pending.setOtpCode(newCode);
            pending.setExpiredAt(LocalDateTime.now().plusMinutes(5));
            pending.setResendCount(pending.getResendCount() + 1);

            emailService.sendOtpEmail(email, newCode);
            return;
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin đăng ký chờ kích hoạt cho email: " + email));

        if (Boolean.TRUE.equals(user.getIsActive())) {
            throw new IllegalStateException("Tài khoản này đã được kích hoạt trước đó!");
        }

        LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);
        long recentRequests = otpRepository.countByUserIdAndCreatedAtAfter(user.getId(), tenMinutesAgo);
        if (recentRequests >= 3) {
            throw new IllegalStateException("Bạn đã yêu cầu gửi mã quá 3 lần. Vui lòng thử lại sau 10 phút.");
        }

        createAndSendOtp(user, OtpType.ACTIVATE_ACCOUNT);
    }

    @Override
    @Transactional
    public String forgotPassword(String email) {
        String query = email != null ? email.trim() : "";
        User user = userRepository.findByEmail(query)
                .or(() -> userRepository.findByUsername(query))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản với email hoặc tên đăng nhập: " + query));

        return createAndSendOtp(user, OtpType.RESET_PASSWORD);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verifyForgotPasswordOtp(String email, String otpCode) {
        String query = email != null ? email.trim() : "";
        User user = userRepository.findByEmail(query)
                .or(() -> userRepository.findByUsername(query))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản với email: " + query));

        OtpVerification otp = otpRepository
                .findTopByUserIdAndCodeAndTypeAndIsUsedFalseOrderByExpiredAtDesc(
                        user.getId(), otpCode != null ? otpCode.trim() : "", OtpType.RESET_PASSWORD)
                .orElse(null);

        return otp != null && !otp.getExpiredAt().isBefore(LocalDateTime.now());
    }

    @Override
    @Transactional
    public void resetPassword(String email, String otpCode, String newPassword) {
        String query = email != null ? email.trim() : "";
        User user = userRepository.findByEmail(query)
                .or(() -> userRepository.findByUsername(query))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản với email: " + query));

        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new IllegalArgumentException("Mật khẩu mới phải có tối thiểu 6 ký tự!");
        }

        OtpVerification otp = otpRepository
                .findTopByUserIdAndCodeAndTypeAndIsUsedFalseOrderByExpiredAtDesc(
                        user.getId(), otpCode != null ? otpCode.trim() : "", OtpType.RESET_PASSWORD)
                .orElseThrow(() -> new IllegalArgumentException("Mã OTP hoặc liên kết xác thực không hợp lệ!"));

        if (otp.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Mã OTP hoặc liên kết đã hết hạn (quá 5 phút)! Vui lòng yêu cầu lại.");
        }

        otp.setIsUsed(true);
        otpRepository.save(otp);

        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        userRepository.save(user);
    }

    private String createAndSendOtp(User user, OtpType type) {
        String code = OtpUtil.generateOtp();

        OtpVerification otp = OtpVerification.builder()
                .user(user)
                .code(code)
                .type(type)
                .expiredAt(LocalDateTime.now().plusMinutes(5))
                .createdAt(LocalDateTime.now())
                .isUsed(false)
                .build();
        otpRepository.save(otp);

        emailService.sendOtpEmail(user.getEmail(), code);
        return code;
    }
}