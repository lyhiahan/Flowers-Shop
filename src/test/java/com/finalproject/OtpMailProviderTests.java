package com.finalproject;

import com.finalproject.service.*;
import com.finalproject.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OtpMailProviderTests {
    private final JavaMailSender smtp = mock(JavaMailSender.class);
    private final BrevoOtpSender api = mock(BrevoOtpSender.class);
    private final EmailService service = new EmailService(smtp, api);

    @Test void defaultApiProviderDoesNotRequireOrCallGmailSmtp() {
        service.sendOtpEmail(" buyer+flowers@outlook.com ", "123456");
        verify(api).send(eq("buyer+flowers@outlook.com"), anyString(), contains("123456"));
        verifyNoInteractions(smtp);
    }

    @Test void apiDeliveryFailureIsNotSwallowedOrSentViaFallbackSmtp() {
        doThrow(new IllegalStateException("API failure")).when(api).send(anyString(), anyString(), anyString());
        assertThatThrownBy(() -> service.sendOtpEmail("buyer@example.test", "123456")).hasMessage("API failure");
        verifyNoInteractions(smtp);
    }

    @Test void invalidProviderFailsWithoutSending() {
        ReflectionTestUtils.setField(service, "provider", "unknown");
        assertThatThrownBy(() -> service.sendOtpEmail("buyer@example.test", "123456")).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(smtp, api);
    }

    @ParameterizedTest
    @ValueSource(strings={"not-an-email", "Name <buyer@example.test>", "a@example.test,b@example.test", "group:a@example.test;", "buyer@example.test\r\nBcc: attacker@example.test"})
    void invalidOrMultipleRecipientsNeverSend(String address) {
        assertThatThrownBy(() -> service.sendOtpEmail(address, "123456")).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(smtp, api);
    }

    @ParameterizedTest @ValueSource(strings={"12345", "1234567", "abcdef", "١٢٣٤٥٦"})
    void invalidOtpNeverSends(String code) {
        assertThatThrownBy(() -> service.sendOtpEmail("buyer@example.test", code)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(smtp, api);
    }
}
