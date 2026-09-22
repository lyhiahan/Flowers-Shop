package com.finalproject.service;

public interface EmailService {
    void sendOtpEmail(String toEmail, String otpCode);
}