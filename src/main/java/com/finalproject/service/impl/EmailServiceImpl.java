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

        // Luôn in mã OTP ra console để hỗ trợ kiểm thử và theo dõi nhanh
        System.out.println("\n===============================================================");
        System.out.println(">>> [MÃ OTP XÁC THỰC]: " + otpCode + " (Email: " + toEmail + ") <<<");
        System.out.println(">>> [LINK CHANGE PASSWORD]: " + resetLink + " <<<");
        System.out.println("===============================================================\n");

        try {
            jakarta.mail.internet.MimeMessage mimeMessage = mailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = 
                    new org.springframework.mail.javamail.MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("Yêu cầu tạo mật khẩu mới - Chuỗi Cửa Hàng Hoa Tươi");
            
            String htmlContent = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 25px; border: 1px solid #fecdd3; border-radius: 16px; background-color: #ffffff;'>"
                    + "<div style='text-align: center; margin-bottom: 20px;'>"
                    + "<h2 style='color: #e11d48; margin-bottom: 5px;'>🌸 Chuỗi Cửa Hàng Hoa Tươi Sài Gòn</h2>"
                    + "<p style='color: #6b7280; font-size: 14px; margin: 0;'>Yêu cầu tạo mật khẩu mới</p>"
                    + "</div>"
                    + "<p style='color: #374151; font-size: 15px;'>Xin chào,</p>"
                    + "<p style='color: #374151; font-size: 15px;'>Bạn nhận được email này vì đã yêu cầu tạo lại mật khẩu mới cho tài khoản <strong>" + toEmail + "</strong>.</p>"
                    + "<div style='background-color: #fff1f2; border-left: 4px solid #e11d48; padding: 12px 16px; margin: 20px 0; border-radius: 6px;'>"
                    + "  <span style='color: #9f1239; font-size: 14px;'>Mã xác thực OTP của bạn:</span>"
                    + "  <div style='font-size: 24px; font-weight: bold; color: #e11d48; letter-spacing: 4px; margin-top: 5px;'>" + otpCode + "</div>"
                    + "</div>"
                    + "<div style='text-align: center; margin: 30px 0;'>"
                    + "  <a href='" + resetLink + "' style='display: inline-block; background-color: #e11d48; color: #ffffff; padding: 14px 32px; text-decoration: none; border-radius: 30px; font-weight: bold; font-size: 16px; box-shadow: 0 4px 10px rgba(225,29,72,0.3);'>Change password</a>"
                    + "</div>"
                    + "<p style='font-size: 13px; color: #6b7280;'>Nếu nút trên không hoạt động, bạn có thể sao chép liên kết sau dán vào trình duyệt:<br><a href='" + resetLink + "' style='color: #e11d48;'>" + resetLink + "</a></p>"
                    + "<p style='font-size: 12px; color: #9ca3af; margin-top: 25px;'>Mã và liên kết có hiệu lực trong 5 phút. Nếu bạn không yêu cầu, vui lòng bỏ qua email này.</p>"
                    + "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            log.info("Đã gửi email OTP thành công tới: {}", toEmail);
        } catch (Exception ex) {
            log.warn("Không thể gửi email OTP qua SMTP: {}. Mã OTP: {}", ex.getMessage(), otpCode);
        }
    }
}