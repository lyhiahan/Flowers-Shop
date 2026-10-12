package com.finalproject.controller.auth;

import com.finalproject.service.JwtTokenService;
import com.finalproject.service.impl.CustomUserDetailsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class JwtAuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService tokens;
    private final CustomUserDetailsService users;

    public record LoginRequest(@NotBlank @Size(max = 100) String username,
                               @NotBlank @Size(max = 128) String password) {}
    public record RefreshRequest(@NotBlank @Size(min = 64, max = 64) String refreshToken) {}
    public record Account(String username, List<String> roles) {}

    @PostMapping("/login")
    public ResponseEntity<JwtTokenService.Tokens> login(@Valid @RequestBody LoginRequest request) {
        Authentication authenticated = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
        return tokenResponse(tokens.issue(authenticated));
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtTokenService.Tokens> refresh(@Valid @RequestBody RefreshRequest request) {
        return tokenResponse(tokens.refresh(request.refreshToken()));
    }

    @GetMapping("/me")
    public Account me(Authentication authentication) {
        var user = users.loadUserByUsername(authentication.getName());
        return new Account(user.getUsername(), user.getAuthorities().stream()
                .map(a -> a.getAuthority().replaceFirst("^ROLE_", "")).sorted().toList());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(JwtAuthenticationToken authentication) {
        tokens.revoke(authentication.getToken());
        return ResponseEntity.noContent().header(HttpHeaders.CACHE_CONTROL, "no-store").build();
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(Map.of("error", "invalid_credentials", "message", "Thông tin đăng nhập hoặc token không hợp lệ."));
    }

    private static ResponseEntity<JwtTokenService.Tokens> tokenResponse(JwtTokenService.Tokens tokens) {
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.PRAGMA, "no-cache").body(tokens);
    }

    @ExceptionHandler({org.springframework.web.bind.MethodArgumentNotValidException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class})
    public ResponseEntity<Map<String, String>> invalidInput() {
        return ResponseEntity.badRequest().body(Map.of("error", "invalid_request", "message", "Dữ liệu yêu cầu không hợp lệ."));
    }
}
