package com.finalproject.service;

import com.finalproject.entity.RefreshToken;
import com.finalproject.entity.User;
import com.finalproject.repository.RefreshTokenRepository;
import com.finalproject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Quản lý vòng đời refresh token:
 * - Tạo token mới khi đăng nhập
 * - Xác thực token khi refresh
 * - Thu hồi toàn bộ token của user (khi logout / đổi mật khẩu / bị khóa)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Value("${app.jwt.refresh-token-expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    /** Tạo refresh token mới và lưu vào DB */
    @Transactional
    public RefreshToken createRefreshToken(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại: " + username));

        RefreshToken token = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiresAt(Instant.now().plusMillis(refreshTokenExpirationMs))
                .revoked(false)
                .build();

        return refreshTokenRepository.save(token);
    }

    /**
     * Xác thực refresh token:
     * - Tồn tại trong DB
     * - Chưa bị thu hồi
     * - Chưa hết hạn
     * - Tài khoản user còn hoạt động
     */
    @Transactional(readOnly = true)
    public RefreshToken verify(String tokenValue) {
        RefreshToken token = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token không hợp lệ hoặc đã hết hạn."));

        if (Boolean.TRUE.equals(token.getRevoked())) {
            throw new IllegalArgumentException("Refresh token đã bị thu hồi.");
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Refresh token đã hết hạn. Vui lòng đăng nhập lại.");
        }
        if (!Boolean.TRUE.equals(token.getUser().getIsActive())) {
            throw new IllegalArgumentException("Tài khoản đã bị khóa hoặc vô hiệu hóa.");
        }

        return token;
    }

    /** Thu hồi tất cả refresh token của user (logout / đổi mật khẩu) */
    @Transactional
    public void revokeAllTokensForUser(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            refreshTokenRepository.revokeAllByUserId(user.getId());
            log.info("[JWT] Đã thu hồi tất cả refresh token của user: {}", username);
        });
    }

    /** Dọn dẹp token hết hạn / đã thu hồi (có thể gọi định kỳ bằng @Scheduled) */
    @Transactional
    public int cleanupExpired() {
        int count = refreshTokenRepository.deleteExpiredAndRevoked(Instant.now());
        if (count > 0) log.info("[JWT] Đã dọn {} refresh token hết hạn/thu hồi", count);
        return count;
    }
}
