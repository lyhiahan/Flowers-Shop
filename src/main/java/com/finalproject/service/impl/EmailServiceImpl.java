package com.finalproject.service.impl;

import com.finalproject.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Override
    public void sendOtpEmail(String toEmail, String otpCode) {
        // Luôn in mã OTP ra Console / Terminal Spring Tool để theo dõi và hỗ trợ kiểm thử
        System.out.println("\n***************************************************************");
        System.out.println("********** [SPRING TOOL CONSOLE - GỬI EMAIL OTP XÁC THỰC] **********");
        System.out.println(">>> GỬI ĐẾN EMAIL : " + toEmail);
        System.out.println(">>> MÃ OTP (6 SỐ): [ " + otpCode + " ]");
        System.out.println(">>> HIỆU LỰC      : 5 PHÚT");
        System.out.println("***************************************************************\n");

        // Nếu mật khẩu mail chưa được cấu hình hoặc là placeholder, không gửi qua SMTP để tránh lỗi xác thực
        if (mailPassword == null || mailPassword.isBlank() || mailPassword.contains("placeholder")) {
            log.info("Chế độ thử nghiệm cục bộ (chưa cấu hình mật khẩu ứng dụng Gmail). Đã sinh mã OTP [{}] cho email {}", otpCode, toEmail);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Mã xác thực - Cửa hàng hoa tươi");
        message.setText("Mã xác thực của bạn: " + otpCode
                + "\nMã có hiệu lực trong 5 phút. Không chia sẻ mã này với bất kỳ ai."
                + "\nNếu bạn không yêu cầu, vui lòng bỏ qua email này.");

        try {
            mailSender.send(message);
            log.info("Đã gửi email OTP thành công tới: {}", toEmail);
        } catch (Exception ex) {
            log.warn("Không thể gửi email OTP qua SMTP đến {}: {}. Mã OTP là: {}", toEmail, ex.getMessage(), otpCode);
        }
    }
}
