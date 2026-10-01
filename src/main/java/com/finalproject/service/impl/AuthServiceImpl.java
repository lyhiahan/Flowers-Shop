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
    public String registerCustomer(RegisterRequest request) {
        // 1. Chuẩn hóa dữ liệu đầu vào
        String username = request.getUsername() != null ? request.getUsername().trim() : "";
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        String phone = request.getPhone() != null ? request.getPhone().trim() : "";
        if (phone.startsWith("+84")) {
            phone = "0" + phone.substring(3);
        } else if (phone.startsWith("84") && phone.length() == 11) {
            phone = "0" + phone.substring(2);
        }
        request.setUsername(username);
        request.setEmail(email);
        request.setPhone(phone);
        if (request.getAddress() == null || request.getAddress().trim().isEmpty()) {
            request.setAddress(request.getFullAddress());
        }

        // Kiểm tra trùng username, email hoặc số điện thoại trong Database
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Tên đăng nhập '" + username + "' đã tồn tại trong hệ thống. Vui lòng chọn tên đăng nhập khác!");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email '" + email + "' đã được sử dụng để đăng ký tài khoản. Vui lòng sử dụng email khác hoặc đăng nhập!");
        }
        if (!phone.isEmpty() && userRepository.existsByPhone(phone)) {
            throw new IllegalArgumentException("Số điện thoại '" + phone + "' đã được sử dụng bởi một tài khoản khác. Vui lòng kiểm tra lại!");
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
        try {
            emailService.sendOtpEmail(request.getEmail(), code);
        } catch (Exception ex) {
            log.warn("Không thể gửi email OTP qua SMTP đến {}: {}. Mã OTP dự phòng là: {}", request.getEmail(), ex.getMessage(), code);
        }
        return code;
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
    public String resendOtp(String email) {
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

            try {
                emailService.sendOtpEmail(email, newCode);
            } catch (Exception ex) {
                log.warn("Không thể gửi email OTP qua SMTP đến {}: {}. Mã OTP là: {}", email, ex.getMessage(), newCode);
            }
            return newCode;
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

        return createAndSendOtp(user, OtpType.ACTIVATE_ACCOUNT);
    }

    @Override
    @Transactional
    public String forgotPassword(String email) {
        String query = email != null ? email.trim() : "";
        User user = userRepository.findByEmail(query)
                .or(() -> userRepository.findByUsername(query))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản với email hoặc tên đăng nhập: " + query));

        userRepository.findLockedById(user.getId()).orElseThrow();
        if (otpRepository.countByUserIdAndCreatedAtAfter(user.getId(), LocalDateTime.now().minusMinutes(10)) >= 3) {
            throw new IllegalStateException("Vui lòng chờ 10 phút trước khi yêu cầu thêm mã OTP.");
        }
        createAndSendOtp(user, OtpType.RESET_PASSWORD);
        return "";
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
    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public void resetPassword(String email, String otpCode, String newPassword) {
        String query = email != null ? email.trim() : "";
        User user = userRepository.findByEmail(query)
                .or(() -> userRepository.findByUsername(query))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản với email: " + query));

        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new IllegalArgumentException("Mật khẩu mới phải có tối thiểu 6 ký tự!");
        }

        if (otpCode == null || !otpCode.trim().matches("[0-9]{6}")) {
            throw new IllegalArgumentException("Vui lòng nhập mã OTP đã gửi đến email của bạn!");
        }
        userRepository.findLockedById(user.getId()).orElseThrow();
        OtpVerification otp = otpRepository.findTopByUserIdAndTypeOrderByExpiredAtDesc(user.getId(), OtpType.RESET_PASSWORD)
                .orElseThrow(() -> new IllegalArgumentException("Mã xác thực không hợp lệ."));
        if (Boolean.TRUE.equals(otp.getIsUsed()) || otp.getExpiredAt().isBefore(LocalDateTime.now()) || otp.getFailedAttempts() >= 5) {
            throw new IllegalArgumentException("Mã OTP đã hết hạn, đã sử dụng hoặc bị khóa. Vui lòng yêu cầu mã mới.");
        }
        if (!otp.getCode().equals(otpCode.trim())) {
            otp.setFailedAttempts(otp.getFailedAttempts() + 1);
            otpRepository.save(otp);
            throw new IllegalArgumentException("Mã OTP không hợp lệ.");
        }
        otp.setIsUsed(true);
        otpRepository.save(otp);

        user.setPassword(passwordEncoder.encode(newPassword));
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

        try {
            emailService.sendOtpEmail(user.getEmail(), code);
        } catch (Exception ex) {
            log.warn("Không thể gửi email OTP qua SMTP đến {}: {}. Mã OTP là: {}", user.getEmail(), ex.getMessage(), code);
        }
        return code;
    }
}
