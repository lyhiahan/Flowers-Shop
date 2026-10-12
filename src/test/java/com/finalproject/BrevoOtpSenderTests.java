package com.finalproject;

import com.finalproject.service.BrevoOtpSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import java.io.IOException;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@ExtendWith(OutputCaptureExtension.class)
class BrevoOtpSenderTests {
    private static final String URL = "https://api.brevo.com/v3/smtp/email";
    private static final String KEY = "fake-api-key-for-unit-test-only";
    private static final String OTP = "918273";
    private MockRestServiceServer server;
    private BrevoOtpSender sender;

    @BeforeEach void setUp() {
        var builder = RestClient.builder().baseUrl("https://api.brevo.com");
        server = MockRestServiceServer.bindTo(builder).build();
        sender = new BrevoOtpSender(builder.build());
        ReflectionTestUtils.setField(sender, "apiKey", KEY);
        ReflectionTestUtils.setField(sender, "fromEmail", "verified-sender@example.test");
        ReflectionTestUtils.setField(sender, "fromName", "Hoa Tươi Sài Gòn");
    }
    private void send(String recipient) { sender.send(recipient, "Mã xác thực", "Mã xác thực của bạn: " + OTP + "\nHiệu lực 5 phút."); }

    @ParameterizedTest
    @ValueSource(strings={"person+flowers@gmail.com", "buyer@outlook.com", "person@example.org"})
    void sendsToExactlyRequestedMailboxUsingVerifiedSenderAndApiKey(String recipient, CapturedOutput output) {
        server.expect(requestTo(URL)).andExpect(method(HttpMethod.POST)).andExpect(header("api-key", KEY))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.to.length()").value(1)).andExpect(jsonPath("$.to[0].email").value(recipient))
                .andExpect(jsonPath("$.sender.email").value("verified-sender@example.test"))
                .andExpect(jsonPath("$.sender.name").value("Hoa Tươi Sài Gòn"))
                .andExpect(jsonPath("$.textContent").value(org.hamcrest.Matchers.containsString(OTP)))
                .andExpect(jsonPath("$.htmlContent").doesNotExist())
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body("{\"messageId\":\"unit-message\"}"));
        send(recipient);
        server.verify();
        assertThat(output.getAll()).doesNotContain(KEY, OTP);
    }

    @Test void usesOnlyPlainTextBodyAndNormalizesSurroundingWhitespace() {
        server.expect(requestTo(URL)).andExpect(jsonPath("$.to[0].email").value("buyer@example.test"))
                .andExpect(jsonPath("$.textContent").value("<script>" + OTP + "</script>"))
                .andExpect(jsonPath("$.htmlContent").doesNotExist())
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body("{\"messageId\":\"unit-message\"}"));
        sender.send(" buyer@example.test ", "OTP", "<script>" + OTP + "</script>");
        server.verify();
    }

    @Test void missingKeyFailsWithoutSendingOrLoggingSecrets(CapturedOutput output) {
        ReflectionTestUtils.setField(sender, "apiKey", "");
        assertThatThrownBy(() -> send("buyer@example.test")).isInstanceOf(IllegalStateException.class).hasMessageContaining("Chưa cấu hình");
        server.verify(); assertThat(output.getAll()).doesNotContain(KEY, OTP);
    }

    @Test void incompleteKeyFailsBeforeMakingAProviderRequest(CapturedOutput output) {
        ReflectionTestUtils.setField(sender, "apiKey", "*");
        assertThatThrownBy(() -> send("buyer@example.test")).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Chưa cấu hình");
        server.verify(); assertThat(output.getAll()).doesNotContain(KEY, OTP);
    }

    @Test void missingOrInvalidSenderFailsWithoutSending() {
        ReflectionTestUtils.setField(sender, "fromEmail", "");
        assertThatThrownBy(() -> send("buyer@example.test")).hasMessageContaining("địa chỉ gửi");
        ReflectionTestUtils.setField(sender, "fromEmail", "invalid-email");
        assertThatThrownBy(() -> send("buyer@example.test")).isInstanceOf(IllegalStateException.class);
        server.verify();
    }

    @ParameterizedTest @ValueSource(ints={400,401,403,429,500})
    void vendorFailureIsNotSuccessAndDoesNotExposeResponseBody(int status, CapturedOutput output) {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatusCode.valueOf(status))
                .contentType(MediaType.APPLICATION_JSON).body("{\"message\":\"" + KEY + " " + OTP + "\"}"));
        assertThatThrownBy(() -> send("buyer@example.test")).isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining(KEY).hasMessageNotContaining(OTP);
        server.verify(); assertThat(output.getAll()).doesNotContain(KEY, OTP);
    }

    @Test void networkFailureFailsSafelyWithoutExposingExceptionBody(CapturedOutput output) {
        server.expect(requestTo(URL)).andRespond(withException(new IOException(KEY + " " + OTP)));
        assertThatThrownBy(() -> send("buyer@example.test")).hasMessageContaining("Không thể kết nối")
                .hasMessageNotContaining(KEY).hasMessageNotContaining(OTP);
        server.verify(); assertThat(output.getAll()).doesNotContain(KEY, OTP);
    }

    @ParameterizedTest @ValueSource(strings={"{}", "{\"messageId\":\"\"}"})
    void missingAcknowledgmentIsNotReportedAsSent(String responseBody) {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(responseBody));
        assertThatThrownBy(() -> send("buyer@example.test")).isInstanceOf(IllegalStateException.class);
        server.verify();
    }

    @Test void redirectsAreNotAcknowledgedOrFollowed() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.FOUND).location(java.net.URI.create("https://other.example.test/"))
                .contentType(MediaType.APPLICATION_JSON).body("{\"messageId\":\"not-accepted\"}"));
        assertThatThrownBy(() -> send("buyer@example.test")).isInstanceOf(IllegalStateException.class);
        server.verify();
    }

    @Test void malformedAcknowledgmentFailsWithoutLoggingBody(CapturedOutput output) {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(KEY + " " + OTP));
        assertThatThrownBy(() -> send("buyer@example.test")).isInstanceOf(IllegalStateException.class);
        server.verify(); assertThat(output.getAll()).doesNotContain(KEY, OTP);
    }
}
