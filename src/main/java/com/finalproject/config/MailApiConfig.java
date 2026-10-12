package com.finalproject.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class MailApiConfig {
    @Bean("otpMailRestClient")
    @Lazy
    public RestClient otpMailRestClient() {
        var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8))
                .followRedirects(HttpClient.Redirect.NEVER).build();
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(12));
        return RestClient.builder().baseUrl("https://api.brevo.com").requestFactory(factory).build();
    }
}
