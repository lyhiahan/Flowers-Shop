package com.finalproject.config;

import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.assertj.core.api.Assertions.assertThat;

class RenderDeploymentConfigurationTests {
    private Properties load(String resource) throws Exception {
        var properties = new Properties();
        try (var stream = getClass().getResourceAsStream(resource)) { properties.load(stream); }
        return properties;
    }

    @Test void renderPortOverridesLocalPortAndLocalDefaultsRemainAvailable() throws Exception {
        var expression = load("/application.properties").getProperty("server.port");
        assertThat(new MockEnvironment().resolveRequiredPlaceholders(expression)).isEqualTo("8080");
        var environment = new MockEnvironment().withProperty("SERVER_PORT", "8081");
        assertThat(environment.resolveRequiredPlaceholders(expression)).isEqualTo("8081");
        environment.withProperty("PORT", "10000");
        assertThat(environment.resolveRequiredPlaceholders(expression)).isEqualTo("10000");
    }

    @Test void qrUsesRenderHttpsUrlUnlessCustomDomainIsExplicitlyConfigured() throws Exception {
        for (var resource : new String[]{"/application.properties", "/application-aiven.properties"}) {
            var expression = load(resource).getProperty("app.demo-payment.base-url");
            var environment = new MockEnvironment();
            assertThat(environment.resolveRequiredPlaceholders(expression)).isEmpty();
            environment.withProperty("RENDER_EXTERNAL_URL", "https://flowers-test.onrender.com");
            assertThat(environment.resolveRequiredPlaceholders(expression)).isEqualTo("https://flowers-test.onrender.com");
            environment.withProperty("DEMO_PAYMENT_BASE_URL", "https://flowers.example.org");
            assertThat(environment.resolveRequiredPlaceholders(expression)).isEqualTo("https://flowers.example.org");
        }
    }

    @Test void localHttpAndRenderHttpsCanUseDifferentCookieSettings() throws Exception {
        var properties = load("/application.properties");
        var environment = new MockEnvironment();
        assertThat(environment.resolveRequiredPlaceholders(properties.getProperty("server.servlet.session.cookie.secure")))
                .isEqualTo("false");
        environment.withProperty("SESSION_COOKIE_SECURE", "true").withProperty("SERVER_FORWARD_HEADERS_STRATEGY", "framework");
        assertThat(environment.resolveRequiredPlaceholders(properties.getProperty("server.servlet.session.cookie.secure")))
                .isEqualTo("true");
        assertThat(environment.resolveRequiredPlaceholders(properties.getProperty("server.forward-headers-strategy")))
                .isEqualTo("framework");
    }
}
