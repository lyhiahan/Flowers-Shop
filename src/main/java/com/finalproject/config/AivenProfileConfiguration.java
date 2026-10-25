package com.finalproject.config;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import java.util.Arrays;

@Configuration(proxyBeanMethods = false)
@Profile("aiven")
public class AivenProfileConfiguration {
    @Bean
    static BeanFactoryPostProcessor isolatedAivenProfile(Environment environment) {
        // The dev profile imports SQL Server secrets; the test profile runs legacy role migrations.
        if (Arrays.stream(environment.getActiveProfiles()).anyMatch(p -> p.equals("dev") || p.equals("test"))) {
            throw new IllegalStateException("Use the aiven profile alone, not together with dev or test.");
        }
        return beanFactory -> {};
    }
}
