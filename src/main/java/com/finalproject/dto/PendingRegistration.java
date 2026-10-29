package com.finalproject.dto;

import com.finalproject.dto.request.RegisterRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingRegistration {
    private RegisterRequest request;
    @lombok.ToString.Exclude
    private String passwordHash;
    private String otpCode;
    @Builder.Default
    private java.util.Set<String> activeOtpCodes = new java.util.concurrent.CopyOnWriteArraySet<>();
    private LocalDateTime expiredAt;
    private LocalDateTime createdAt;
    private LocalDateTime lastSentAt;
    private int resendCount;
    private int failedAttempts;

    public boolean matchesOtp(String inputCode) {
        if (inputCode == null || inputCode.isBlank()) return false;
        String clean = inputCode.replaceAll("[\\s\\-]+", "").trim();
        if (otpCode != null && otpCode.equals(clean)) return true;
        return activeOtpCodes != null && activeOtpCodes.contains(clean);
    }

    public void addOtpCode(String code) {
        if (code == null) return;
        this.otpCode = code;
        if (this.activeOtpCodes == null) {
            this.activeOtpCodes = new java.util.concurrent.CopyOnWriteArraySet<>();
        }
        this.activeOtpCodes.add(code);
    }
}
