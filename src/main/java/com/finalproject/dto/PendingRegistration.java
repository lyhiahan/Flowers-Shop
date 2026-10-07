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
    private String otpCode;
    private LocalDateTime expiredAt;
    private LocalDateTime createdAt;
    private int resendCount;
    private int failedAttempts;
}
