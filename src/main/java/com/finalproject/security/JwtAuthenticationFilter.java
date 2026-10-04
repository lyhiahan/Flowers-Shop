package com.finalproject.security;

import com.finalproject.entity.User;
import com.finalproject.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Filter chạy TRƯỚC UsernamePasswordAuthenticationFilter.
 * Chỉ xử lý các request có header Authorization: Bearer <token>.
 * Các trang Thymeleaf (session-based) không bị ảnh hưởng.
 *
 * Quy trình:
 * 1. Đọc Bearer token từ Authorization header.
 * 2. Xác thực chữ ký và hạn token.
 * 3. Tải user từ DB, kiểm tra isActive (tài khoản bị khóa → 401 dù token chưa hết hạn).
 * 4. Đặt Authentication vào SecurityContext.
 */
// @Component (Vô hiệu hóa filter này vì đã chuyển toàn bộ sang Session-based authentication)
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = extractBearerToken(request);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            String username = jwtTokenProvider.getUsernameFromToken(token);
            String rolesStr = jwtTokenProvider.getRolesFromToken(token);

            // Kiểm tra tài khoản còn hoạt động không (bị khóa → từ chối dù token hợp lệ)
            User user = userRepository.findByUsername(username).orElse(null);
            if (user == null || !Boolean.TRUE.equals(user.getIsActive())) {
                log.debug("[JWT] Tài khoản {} không tồn tại hoặc đã bị khóa – từ chối truy cập API.", username);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\":\"Tài khoản không tồn tại hoặc đã bị khóa.\",\"status\":401}");
                return;
            }

            // Xây dựng danh sách quyền từ roles trong token
            List<SimpleGrantedAuthority> authorities = List.of();
            if (StringUtils.hasText(rolesStr)) {
                authorities = Arrays.stream(rolesStr.split(","))
                        .map(String::trim)
                        .filter(r -> !r.isEmpty())
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("[JWT] Xác thực thành công: user={}, roles={}", username, rolesStr);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Chỉ xử lý các request vào /api/** để tránh can thiệp vào luồng session Thymeleaf.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/");
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return null;
    }
}
