package com.finalproject.service.impl;

import com.finalproject.service.EmailService;
import com.finalproject.service.BrevoOtpSender;
import com.finalproject.service.OtpEmailInput;
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
    private final BrevoOtpSender brevoSender;

    @Value("${app.mail.provider:brevo}")
    private String provider = "brevo";

    @Value("${app.mail.from-email:}")
    private String configuredSender = "";

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${app.mail.mock-enabled:false}")
    private boolean mockEnabled;

    @Override
    public void sendOtpEmail(String toEmail, String otpCode) {
        String recipient = OtpEmailInput.address(toEmail);
        if (otpCode == null || !otpCode.matches("[0-9]{6}"))
            throw new IllegalArgumentException("Mã OTP phải gồm 6 chữ số.");
        if (mockEnabled) {
            log.info("Chế độ kiểm thử: bỏ qua gửi email (không ghi mã OTP).");
            return;
        }
        String subject = "Mã xác thực - Cửa hàng hoa tươi";
        String text = "Mã xác thực của bạn: " + otpCode
                + "\nMã có hiệu lực trong 5 phút. Không chia sẻ mã này với bất kỳ ai."
                + "\nNếu bạn không yêu cầu, vui lòng bỏ qua email này.";
        if ("brevo".equalsIgnoreCase(provider)) {
            brevoSender.send(recipient, subject, text);
            return;
        }
        if (!"smtp".equalsIgnoreCase(provider))
            throw new IllegalStateException("Cấu hình dịch vụ gửi email không hợp lệ.");
        // Nếu mật khẩu mail chưa được cấu hình hoặc là placeholder, không gửi qua SMTP để tránh lỗi xác thực
        if (fromEmail == null || fromEmail.isBlank() || mailPassword == null || mailPassword.isBlank() || mailPassword.contains("placeholder")) {
            log.warn("Chưa cấu hình SMTP: email OTP chưa được gửi. Vui lòng cấu hình tài khoản mail.");
            throw new IllegalStateException("Chưa cấu hình dịch vụ email. Vui lòng liên hệ quản trị viên.");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(OtpEmailInput.address(configuredSender == null || configuredSender.isBlank() ? fromEmail : configuredSender));
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(text);

        try {
            mailSender.send(message);
            log.info("Đã gửi email OTP thành công tới: {}", toEmail);
        } catch (Exception ex) {
            log.warn("Không thể gửi email OTP qua SMTP. Loại lỗi: {}", ex.getClass().getSimpleName());
            throw new IllegalStateException("Không thể gửi email xác thực. Vui lòng thử lại sau.");
        }
    }
}
