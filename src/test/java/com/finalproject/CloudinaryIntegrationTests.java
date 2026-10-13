package com.finalproject;

import com.cloudinary.Cloudinary;
import com.finalproject.config.CloudinaryConfig;
import com.finalproject.service.CloudinaryService;
import com.finalproject.service.impl.CloudinaryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Opt-in only: uses local .env/server variables and deletes its own test asset. */
@EnabledIfSystemProperty(named = "cloudinary.integration", matches = "true")
@SpringJUnitConfig(classes = CloudinaryIntegrationTests.TestConfiguration.class,
        initializers = ConfigDataApplicationContextInitializer.class)
class CloudinaryIntegrationTests {
    @Configuration(proxyBeanMethods = false)
    @Import({CloudinaryConfig.class, CloudinaryServiceImpl.class})
    static class TestConfiguration {
        @Bean
        static PropertySourcesPlaceholderConfigurer placeholders() {
            return new PropertySourcesPlaceholderConfigurer();
        }
    }

    @Autowired Cloudinary client;
    @Autowired CloudinaryService service;

    @Test
    void localConfigurationCanUploadAndDeleteAnActualImage() throws Exception {
        byte[] png = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a5XcAAAAASUVORK5CYII=");
        var image = new MockMultipartFile("image", "verification.png", "image/png", png);
        String folder = "rental_shop_verification/" + UUID.randomUUID();
        var result = service.upload(image, folder);
        try {
            assertThat(result.publicId()).startsWith(folder + "/");
            assertThat(result.secureUrl()).startsWith("https://res.cloudinary.com/" + client.config.cloudName + "/");
        } finally {
            var deleted = client.uploader().destroy(result.publicId(), Map.of("invalidate", true));
            assertThat(deleted.get("result")).isEqualTo("ok");
        }
    }
}
