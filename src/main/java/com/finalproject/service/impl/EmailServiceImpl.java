package com.finalproject.service.impl;

import com.finalproject.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendOtpEmail(String toEmail, String otpCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Mã xác thực tài khoản - Rental Shop");
        message.setText("Mã kích hoạt tài khoản của bạn là: " + otpCode + "\nMã có hiệu lực trong 120 giây. Vui lòng không chia sẻ mã này cho bất kỳ ai.");
        mailSender.send(message);
    }
}