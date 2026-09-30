package com.finalproject.service;

import com.finalproject.dto.request.RegisterRequest;

public interface AuthService {
    String registerCustomer(RegisterRequest request);
    boolean verifyOtp(String email, String otpCode);
    String resendOtp(String email);
    String forgotPassword(String email);
    boolean verifyForgotPasswordOtp(String email, String otpCode);
    void resetPassword(String email, String otpCode, String newPassword);
}