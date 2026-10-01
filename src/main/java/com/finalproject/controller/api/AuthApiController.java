package com.finalproject.controller.api;

import com.finalproject.entity.RefreshToken;
import com.finalproject.security.JwtTokenProvider;
import com.finalproject.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API xác thực bằng JWT.
 * Không ảnh hưởng đến luồng session/formLogin của Thymeleaf.
 *
 * POST /api/auth/login     – đổi username+password lấy access token + refresh token
 * POST /api/auth/refresh   – dùng refresh token lấy access token mới
 * POST /api/auth/logout    – thu hồi tất cả refresh token của user
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    // ─── DTO records ──────────────────────────────────────────────────────────

    public record LoginRequest(String username, String password) {}
    public record RefreshRequest(String refreshToken) {}

    // ─── POST /api/auth/login ─────────────────────────────────────────────────

    /**
     * Luồng:
     * Client gửi { "username": "...", "password": "..." }
     * → Spring Security xác thực
     * → Server phát access token (15 phút) + refresh token (7 ngày)
     * → Client lưu access token an toàn (memory / httpOnly cookie tùy FE)
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request.username() == null || request.username().isBlank()
                || request.password() == null || request.password().isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Username và password không được để trống.", "status", 400));
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username().trim(),
                            request.password()
                    )
            );

            // Tạo access token
            String accessToken = jwtTokenProvider.generateAccessToken(authentication);

            // Tạo refresh token và lưu DB
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(request.username().trim());

            String roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.joining(","));

            log.info("[JWT] Login thành công: user={}", request.username().trim());

            return ResponseEntity.ok(Map.of(
                    "accessToken", accessToken,
                    "refreshToken", refreshToken.getToken(),
                    "tokenType", "Bearer",
                    "expiresIn", 900,                   // giây
                    "username", request.username().trim(),
                    "roles", roles
            ));

        } catch (DisabledException ex) {
            log.warn("[JWT] Login thất bại – tài khoản bị khóa: {}", request.username());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("error", "Tài khoản đã bị khóa hoặc chưa kích hoạt. Vui lòng liên hệ quản trị viên.", "status", 401));
        } catch (BadCredentialsException ex) {
            log.warn("[JWT] Login thất bại – sai thông tin: {}", request.username());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("error", "Tên đăng nhập hoặc mật khẩu không chính xác.", "status", 401));
        } catch (Exception ex) {
            log.error("[JWT] Lỗi đăng nhập", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", "Lỗi máy chủ. Vui lòng thử lại sau.", "status", 500));
        }
    }

    // ─── POST /api/auth/refresh ───────────────────────────────────────────────

    /**
     * Client gửi { "refreshToken": "..." }
     * → Server xác thực refresh token (chưa hết hạn, chưa bị thu hồi, tài khoản active)
     * → Server trả access token mới
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest request) {
        if (request.refreshToken() == null || request.refreshToken().isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "refreshToken không được để trống.", "status", 400));
        }

        try {
            RefreshToken refreshToken = refreshTokenService.verify(request.refreshToken());

            String username = refreshToken.getUser().getUsername();
            String roles = refreshToken.getUser().getRoles() == null ? "" :
                    refreshToken.getUser().getRoles().stream()
                            .map(r -> "ROLE_" + r.name())
                            .collect(Collectors.joining(","));

            String newAccessToken = jwtTokenProvider.generateAccessToken(username, roles);

            return ResponseEntity.ok(Map.of(
                    "accessToken", newAccessToken,
                    "tokenType", "Bearer",
                    "expiresIn", 900,
                    "username", username
            ));

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("error", ex.getMessage(), "status", 401));
        } catch (Exception ex) {
            log.error("[JWT] Lỗi refresh token", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", "Lỗi máy chủ.", "status", 500));
        }
    }

    // ─── POST /api/auth/logout ────────────────────────────────────────────────

    /**
     * Thu hồi tất cả refresh token của user đang đăng nhập (xác thực qua Bearer token).
     * Client cũng phải tự xóa access token (vì stateless).
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("error", "Chưa đăng nhập.", "status", 401));
        }

        String username = authentication.getName();
        refreshTokenService.revokeAllTokensForUser(username);
        log.info("[JWT] Logout API: user={}", username);

        return ResponseEntity.ok(Map.of(
                "message", "Đăng xuất thành công. Tất cả phiên đăng nhập đã được thu hồi.",
                "timestamp", Instant.now().toString()
        ));
    }
}
