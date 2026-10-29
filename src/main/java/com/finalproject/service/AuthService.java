package com.finalproject.service;

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
import com.finalproject.utils.OtpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final OtpVerificationRepository otpRepository;
    private final CartRepository cartRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final jakarta.validation.Validator validator;
    private final jakarta.persistence.EntityManager entityManager;
    private final java.time.Clock clock;

    private void lockAndRefreshUser(User user) {
        userRepository.findLockedById(user.getId()).orElseThrow();
        entityManager.refresh(user, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
    }

    // Lưu tạm thông tin đăng ký chờ xác thực OTP trong bộ nhớ, CHƯA LƯU VÀO DATABASE để tránh tài khoản rác
    private final Map<String, PendingRegistration> pendingRegistrations = new ConcurrentHashMap<>();

    @Transactional
    public String registerCustomer(RegisterRequest request) {
        if (request == null) throw new IllegalArgumentException("Vui lòng điền thông tin đăng ký.");
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
        var validationErrors = validator.validate(request);
        if (!validationErrors.isEmpty()) throw new IllegalArgumentException(validationErrors.stream()
                .map(jakarta.validation.ConstraintViolation::getMessage).sorted().collect(java.util.stream.Collectors.joining("; ")));
        if (request.getPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException("Mật khẩu tối đa 72 byte UTF-8.");
        request.setEmail(email);
        request.setPhone(phone);
        if (request.getAddress() == null || request.getAddress().trim().isEmpty()) {
            request.setAddress(request.getFullAddress());
        }
        if (request.getAddress().length() > 255) throw new IllegalArgumentException("Địa chỉ tối đa 255 ký tự.");

        // Username/email identify accounts; phone is contact information and may be shared.
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Tên đăng nhập '" + username + "' đã tồn tại trong hệ thống. Vui lòng chọn tên đăng nhập khác!");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email '" + email + "' đã được sử dụng để đăng ký tài khoản. Vui lòng sử dụng email khác hoặc đăng nhập!");
        }

        // 2. Tạo mã OTP và lưu tạm vào bộ nhớ (CHƯA LƯU VÀO DATABASE)
        String code = OtpUtil.generateOtp();
        LocalDateTime now = LocalDateTime.now(clock);
        // Keep only a detached, password-free request in the pending-registration cache.
        RegisterRequest pendingRequest = new RegisterRequest();
        org.springframework.beans.BeanUtils.copyProperties(request, pendingRequest);
        pendingRequest.setPassword(null);
        java.util.Set<String> activeCodes = new java.util.concurrent.CopyOnWriteArraySet<>();
        activeCodes.add(code);
        PendingRegistration pending = PendingRegistration.builder()
                .request(pendingRequest)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .otpCode(code)
                .activeOtpCodes(activeCodes)
                .createdAt(now)
                .lastSentAt(now)
                .expiredAt(now.plusMinutes(5))
                .resendCount(0)
                .build();

        pendingRegistrations.entrySet().removeIf(entry -> entry.getValue().getExpiredAt().isBefore(now));
        if (pendingRegistrations.putIfAbsent(request.getEmail().toLowerCase().trim(), pending) != null)
            throw new IllegalArgumentException("Email đang chờ xác thực. Vui lòng dùng mã đã gửi hoặc yêu cầu gửi lại.");

        // 3. Gửi mã OTP xác thực qua email; không ghi mã xác thực vào log.
        try {
            emailService.sendOtpEmail(request.getEmail(), code);
        } catch (Exception ex) {
            log.warn("Không thể gửi email OTP. Loại lỗi: {}", ex.getClass().getSimpleName());
            pendingRegistrations.remove(request.getEmail(), pending);
            throw new IllegalStateException("Không thể gửi email xác thực. Vui lòng thử đăng ký lại sau.");
        }
        return code;
    }

    @Transactional
    public synchronized boolean verifyOtp(String email, String otpCode) {
        if (email == null || email.isBlank() || otpCode == null) return false;
        String cleanCode = otpCode.replaceAll("[\\s\\-]+", "").trim();
        if (!cleanCode.matches("[0-9]{6}")) return false;
        String key = email.toLowerCase().trim();
        PendingRegistration pending = pendingRegistrations.get(key);

        if (pending != null) {
            // Kiểm tra mã OTP và thời hạn hiệu lực (5 phút)
            if (pending.getExpiredAt().isBefore(LocalDateTime.now(clock))) {
                pendingRegistrations.remove(key);
                return false;
            }
            if (pending.getFailedAttempts() >= 5) return false;
            if (!pending.matchesOtp(cleanCode)) {
                pending.setFailedAttempts(pending.getFailedAttempts() + 1);
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
                    .password(pending.getPasswordHash())
                    .fullName(req.getFullName())
                    .email(req.getEmail())
                    .phone(req.getPhone())
                    .address(req.getAddress())
                    .role(Role.USER)
                    .isEmailActive(true)
                    .isActive(true)
                    .createdDate(LocalDate.now(clock))
                    .createdAt(java.time.LocalDateTime.now(clock))
                    .creationTimeKnown(true)
                    .build();
            user.addRole(Role.USER);
            userRepository.save(user);

            // Khởi tạo giỏ hàng cho tài khoản vừa được kích hoạt
            Cart cart = Cart.builder()
                    .user(user)
                    .build();
            cartRepository.save(cart);

            // Lưu bản ghi xác thực OTP vào database
            OtpVerification otp = OtpVerification.builder()
                    .user(user)
                    .code(cleanCode)
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
        User user = userRepository.findByEmail(key).orElse(null);
        if (user != null) {
            lockAndRefreshUser(user);
            if (user.isRetiredDeliveryAccount() || Boolean.TRUE.equals(user.getAccountDisabledByAdmin())) return false;
            
            OtpVerification otp = otpRepository
                    .findTopByUserIdAndCodeAndTypeAndIsUsedFalseOrderByExpiredAtDesc(
                            user.getId(), cleanCode, OtpType.ACTIVATE_ACCOUNT)
                    .orElseGet(() -> otpRepository
                            .findTopByUserIdAndTypeAndIsUsedFalseOrderByExpiredAtDesc(
                                    user.getId(), OtpType.ACTIVATE_ACCOUNT)
                            .orElse(null));

            if (otp == null || otp.getExpiredAt().isBefore(LocalDateTime.now(clock)) || otp.getFailedAttempts() >= 5) {
                return false;
            }

            if (!otp.getCode().equals(cleanCode)) {
                otp.setFailedAttempts(otp.getFailedAttempts() + 1);
                otpRepository.save(otp); return false;
            }
            otp.setIsUsed(true);
            otpRepository.save(otp);

            user.setIsActive(true);
            user.setIsEmailActive(true);
            userRepository.save(user);
            return true;
        }

        return false;
    }

    @Transactional
    public synchronized String resendOtp(String email) {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Vui lòng nhập email.");
        String key = email.toLowerCase().trim();
        PendingRegistration pending = pendingRegistrations.get(key);

        if (pending != null) {
            LocalDateTime now = LocalDateTime.now(clock);
            if (pending.getCreatedAt().isBefore(now.minusMinutes(10))) {
                pending.setCreatedAt(now);
                pending.setResendCount(0);
                if (pending.getActiveOtpCodes() != null) pending.getActiveOtpCodes().clear();
            }
            if (pending.getResendCount() >= 3) {
                throw new IllegalStateException("Bạn đã yêu cầu gửi mã quá 3 lần. Vui lòng đăng ký lại.");
            }
            String newCode = OtpUtil.generateOtp();
            try {
                emailService.sendOtpEmail(email, newCode);
            } catch (Exception ex) {
                log.warn("Không thể gửi email OTP. Loại lỗi: {}", ex.getClass().getSimpleName());
                throw new IllegalStateException("Không thể gửi lại email xác thực. Mã cũ vẫn còn hiệu lực nếu chưa hết hạn.");
            }
            pending.addOtpCode(newCode);
            pending.setLastSentAt(now);
            pending.setFailedAttempts(0);
            pending.setExpiredAt(now.plusMinutes(5));
            pending.setResendCount(pending.getResendCount() + 1);
            return newCode;
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin đăng ký chờ kích hoạt cho email: " + email));
        lockAndRefreshUser(user);

        if (user.isRetiredDeliveryAccount() || Boolean.TRUE.equals(user.getAccountDisabledByAdmin())) {
            throw new IllegalStateException("Tài khoản đã bị quản trị viên khóa hoặc vô hiệu hóa. Vui lòng liên hệ quản trị viên.");
        }

        if (Boolean.TRUE.equals(user.getIsActive())) {
            throw new IllegalStateException("Tài khoản này đã được kích hoạt trước đó!");
        }

        LocalDateTime tenMinutesAgo = LocalDateTime.now(clock).minusMinutes(10);
        long recentRequests = otpRepository.countByUserIdAndCreatedAtAfter(user.getId(), tenMinutesAgo);
        if (recentRequests >= 3) {
            throw new IllegalStateException("Bạn đã yêu cầu gửi mã quá 3 lần. Vui lòng thử lại sau 10 phút.");
        }

        return createAndSendOtp(user, OtpType.ACTIVATE_ACCOUNT);
    }

    @Transactional
    public String forgotPassword(String email) {
        String query = email != null ? email.trim() : "";
        User user = userRepository.findByEmail(query)
                .or(() -> userRepository.findByUsername(query))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản với email hoặc tên đăng nhập: " + query));

        rejectRetiredDeliveryAccount(user);

        lockAndRefreshUser(user);
        if (otpRepository.countByUserIdAndCreatedAtAfter(user.getId(), LocalDateTime.now(clock).minusMinutes(10)) >= 3) {
            throw new IllegalStateException("Vui lòng chờ 10 phút trước khi yêu cầu thêm mã OTP.");
        }
        return createAndSendOtp(user, OtpType.RESET_PASSWORD);
    }

    @Transactional(readOnly = true)
    public boolean verifyForgotPasswordOtp(String email, String otpCode) {
        String query = email != null ? email.trim() : "";
        String cleanCode = otpCode != null ? otpCode.replaceAll("[\\s\\-]+", "").trim() : "";
        User user = userRepository.findByEmail(query)
                .or(() -> userRepository.findByUsername(query))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản với email: " + query));

        if (user.isRetiredDeliveryAccount()) return false;

        OtpVerification otp = otpRepository
                .findTopByUserIdAndCodeAndTypeAndIsUsedFalseOrderByExpiredAtDesc(
                        user.getId(), cleanCode, OtpType.RESET_PASSWORD)
                .orElse(null);

        return otp != null && otp.getFailedAttempts() < 5 && !otp.getExpiredAt().isBefore(LocalDateTime.now(clock));
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public void resetPassword(String email, String otpCode, String newPassword) {
        String query = email != null ? email.trim() : "";
        User user = userRepository.findByEmail(query)
                .or(() -> userRepository.findByUsername(query))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản với email: " + query));

        rejectRetiredDeliveryAccount(user);

        if (newPassword == null || newPassword.trim().length() < 6 || newPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Mật khẩu mới phải có tối thiểu 6 ký tự!");
        }

        String cleanCode = otpCode != null ? otpCode.replaceAll("[\\s\\-]+", "").trim() : "";
        if (!cleanCode.matches("[0-9]{6}")) {
            throw new IllegalArgumentException("Vui lòng nhập mã OTP đã gửi đến email của bạn!");
        }
        lockAndRefreshUser(user);
        rejectRetiredDeliveryAccount(user);
        OtpVerification otp = otpRepository.findTopByUserIdAndTypeOrderByExpiredAtDesc(user.getId(), OtpType.RESET_PASSWORD)
                .orElseThrow(() -> new IllegalArgumentException("Mã xác thực không hợp lệ."));
        if (Boolean.TRUE.equals(otp.getIsUsed()) || otp.getExpiredAt().isBefore(LocalDateTime.now(clock)) || otp.getFailedAttempts() >= 5) {
            throw new IllegalArgumentException("Mã OTP đã hết hạn, đã sử dụng hoặc bị khóa. Vui lòng yêu cầu mã mới.");
        }
        if (!otp.getCode().equals(cleanCode)) {
            otp.setFailedAttempts(otp.getFailedAttempts() + 1);
            otpRepository.save(otp);
            throw new IllegalArgumentException("Mã OTP không hợp lệ.");
        }
        otp.setIsUsed(true);
        otpRepository.save(otp);

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private void rejectRetiredDeliveryAccount(User user) {
        if (user.isRetiredDeliveryAccount()) {
            throw new IllegalArgumentException("Tài khoản giao hàng đã được vô hiệu hóa.");
        }
    }

    private String createAndSendOtp(User user, OtpType type) {
        String code = OtpUtil.generateOtp();

        OtpVerification otp = OtpVerification.builder()
                .user(user)
                .code(code)
                .type(type)
                .expiredAt(LocalDateTime.now(clock).plusMinutes(5))
                .createdAt(LocalDateTime.now(clock))
                .isUsed(false)
                .build();
        otpRepository.save(otp);

        try {
            emailService.sendOtpEmail(user.getEmail(), code);
        } catch (Exception ex) {
            log.warn("Không thể gửi email OTP. Loại lỗi: {}", ex.getClass().getSimpleName());
            throw new IllegalStateException("Không thể gửi email xác thực. Vui lòng thử lại sau.");
        }
        return code;
    }

}
