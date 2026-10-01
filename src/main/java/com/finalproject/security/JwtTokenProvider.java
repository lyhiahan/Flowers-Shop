package com.finalproject.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * Tạo và xác thực JWT access token.
 *
 * Secret lấy từ biến môi trường JWT_SECRET (tối thiểu 32 ký tự).
 * Token chứa: sub (username), roles, iat, exp.
 * Không lưu bất kỳ thông tin nhạy cảm nào (password, email thật, ...) vào token.
 */
@Component
@Slf4j
public class JwtTokenProvider {

    private final SecretKey signingKey;
    private final long accessTokenExpirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiration-ms:900000}") long accessTokenExpirationMs) {

        // Đảm bảo key đủ dài cho HS256 (≥ 32 bytes). Pad nếu cần, nhưng log cảnh báo.
        byte[] keyBytes;
        if (secret.length() >= 43) {
            // Thử parse Base64 trước (khuyến nghị khi deploy thật)
            try {
                keyBytes = Decoders.BASE64.decode(secret);
            } catch (Exception e) {
                keyBytes = secret.getBytes();
            }
        } else {
            keyBytes = secret.getBytes();
        }

        // Đảm bảo tối thiểu 32 bytes cho HMAC-SHA256
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpirationMs = accessTokenExpirationMs;

        if (secret.startsWith("dev-only")) {
            log.warn("[JWT] Đang dùng secret mặc định – CHỈ DÀNH CHO MÔI TRƯỜNG DEV! Hãy set biến môi trường JWT_SECRET trước khi deploy.");
        }
    }

    // ─── Tạo token ────────────────────────────────────────────────────────────

    /**
     * Tạo access token từ Authentication object (sau khi đăng nhập thành công).
     */
    public String generateAccessToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return buildToken(userDetails.getUsername(), extractRoles(userDetails), accessTokenExpirationMs);
    }

    /**
     * Tạo access token từ username và roles (dùng khi refresh).
     */
    public String generateAccessToken(String username, String roles) {
        return buildToken(username, roles, accessTokenExpirationMs);
    }

    private String buildToken(String username, String roles, long expirationMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    // ─── Đọc token ────────────────────────────────────────────────────────────

    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public String getRolesFromToken(String token) {
        return parseClaims(token).get("roles", String.class);
    }

    public Date getExpirationFromToken(String token) {
        return parseClaims(token).getExpiration();
    }

    // ─── Xác thực token ───────────────────────────────────────────────────────

    /**
     * Kiểm tra token hợp lệ (chữ ký + chưa hết hạn).
     * Không kiểm tra trạng thái tài khoản – việc đó do JwtAuthenticationFilter đảm nhiệm.
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.debug("JWT đã hết hạn: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.warn("JWT không được hỗ trợ: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.warn("JWT không đúng định dạng: {}", ex.getMessage());
        } catch (SecurityException ex) {
            log.warn("Chữ ký JWT không hợp lệ: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.warn("JWT rỗng hoặc null: {}", ex.getMessage());
        }
        return false;
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String extractRoles(UserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
    }
}
