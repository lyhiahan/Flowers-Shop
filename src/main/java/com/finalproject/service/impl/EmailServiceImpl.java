package com.finalproject.service.impl;

import com.finalproject.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendOtpEmail(String toEmail, String otpCode) {
        String resetLink = "http://localhost:8080/reset-password?email=" + toEmail + "&otpCode=" + otpCode;
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Yêu cầu xác thực tài khoản / Tạo mật khẩu mới - Bloom Flower");
            message.setText("Chào bạn,\n\nMã xác thực OTP của bạn là: " + otpCode + "\n\nNếu bạn đang yêu cầu khôi phục mật khẩu, vui lòng bấm vào liên kết Change password bên dưới để tạo mật khẩu mới:\n" + resetLink + "\n\nMã có hiệu lực trong 5 phút. Vui lòng không chia sẻ mã này cho bất kỳ ai.\n\nTrân trọng,\nBloom Flower Chain");
            mailSender.send(message);
            log.info("Đã gửi email OTP thành công tới: {}", toEmail);
        } catch (Exception ex) {
            log.warn("Không thể gửi email OTP qua SMTP: {}. Mã OTP: {}", ex.getMessage(), otpCode);
            System.out.println("===============================================================");
            System.out.println(">>> [MÃ OTP XÁC THỰC]: " + otpCode + " (Email: " + toEmail + ") <<<");
            System.out.println(">>> [LINK CHANGE PASSWORD]: " + resetLink + " <<<");
            System.out.println("===============================================================");
        }
    }
}