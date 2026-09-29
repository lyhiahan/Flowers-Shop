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
        message.setSubject("Mã xác thực - Cửa hàng hoa tươi");
        message.setText("Mã xác thực của bạn: " + otpCode
                + "\nMã có hiệu lực trong 5 phút. Không chia sẻ mã này với bất kỳ ai."
                + "\nNếu bạn không yêu cầu, vui lòng bỏ qua email này.");
        try { mailSender.send(message); }
        catch (org.springframework.mail.MailException ex) {
            throw new IllegalStateException("Không thể gửi email xác thực. Vui lòng thử lại sau.");
        }
    }
}
