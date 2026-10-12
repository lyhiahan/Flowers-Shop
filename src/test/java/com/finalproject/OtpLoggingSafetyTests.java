package com.finalproject;

import com.finalproject.service.impl.EmailServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class OtpLoggingSafetyTests {
    private static final String OTP = "918273";
    private final JavaMailSender sender = mock(JavaMailSender.class);
    private EmailServiceImpl service(String password) {
        var service = new EmailServiceImpl(sender, mock(com.finalproject.service.BrevoOtpSender.class));
        ReflectionTestUtils.setField(service, "provider", "smtp");
        ReflectionTestUtils.setField(service, "mailPassword", password);
        ReflectionTestUtils.setField(service, "fromEmail", "sender@example.test");
        return service;
    }

    @Test void missingSmtpWarnsWithoutLoggingOtp(CapturedOutput output) {
        assertThatThrownBy(() -> service("").sendOtpEmail("recipient@example.test", OTP))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Chưa cấu hình");
        verifyNoInteractions(sender);
        assertThat(output.getAll()).contains("Chưa cấu hình SMTP").doesNotContain(OTP);
    }

    @Test void successfulEmailContainsOtpButLogsDoNot(CapturedOutput output) {
        service("configured-for-test").sendOtpEmail("recipient@example.test", OTP);
        var message = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(message.capture());
        assertThat(message.getValue().getText()).contains(OTP);
        assertThat(message.getValue().getTo()).containsExactly("recipient@example.test");
        assertThat(output.getAll()).doesNotContain(OTP, "configured-for-test");
    }

    @Test void smtpExceptionDoesNotLeakMessageOrOtp(CapturedOutput output) {
        doThrow(new MailSendException("SMTP rejected message containing " + OTP))
                .when(sender).send(any(SimpleMailMessage.class));
        assertThatThrownBy(() -> service("configured-for-test").sendOtpEmail("recipient@example.test", OTP))
                .isInstanceOf(IllegalStateException.class).hasMessageNotContaining(OTP);
        assertThat(output.getAll()).contains("MailSendException").doesNotContain(OTP, "SMTP rejected", "configured-for-test");
    }

    @Test void explicitTestModeDoesNotSendOrLogOtp(CapturedOutput output) {
        var service = service("");
        ReflectionTestUtils.setField(service, "mockEnabled", true);
        service.sendOtpEmail("recipient@example.test", OTP);
        verifyNoInteractions(sender);
        assertThat(output.getAll()).doesNotContain(OTP);
    }
}
