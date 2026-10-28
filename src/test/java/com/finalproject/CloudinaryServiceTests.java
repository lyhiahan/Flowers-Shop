package com.finalproject;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.cloudinary.utils.ObjectUtils;
import com.finalproject.service.CloudinaryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.mock.web.MockMultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CloudinaryServiceTests {
    @Test void cloudFailureIsReportedInsteadOfSilentlySavingLocally() throws Exception {
        Cloudinary sdk = spy(new Cloudinary(ObjectUtils.asMap("cloud_name", "configured-cloud", "api_key", "configured-key", "api_secret", "configured-secret")));
        Uploader uploader = mock(Uploader.class); doReturn(uploader).when(sdk).uploader();
        when(uploader.upload(any(), anyMap())).thenThrow(new IOException("Upload unavailable"));
        assertThatThrownBy(() -> new CloudinaryService(sdk).upload(image(), "products"))
                .isInstanceOf(IOException.class).hasMessageContaining("Cloudinary");
    }

    @Test void uploadPreservesCloudUrlAndPublicIdAndRejectsNonImageFiles() throws Exception {
        Cloudinary sdk = spy(new Cloudinary(ObjectUtils.asMap("cloud_name", "configured-cloud", "api_key", "configured-key", "api_secret", "configured-secret")));
        Uploader uploader = mock(Uploader.class); doReturn(uploader).when(sdk).uploader();
        when(uploader.upload(any(), anyMap())).thenReturn(Map.of("secure_url", "https://res.cloudinary.com/configured-cloud/image/upload/test.png", "public_id", "products/test"));
        var service = new CloudinaryService(sdk);
        var result = service.upload(image(), "products");
        assertThat(result.secureUrl()).startsWith("https://res.cloudinary.com/");
        assertThat(result.publicId()).isEqualTo("products/test");
        assertThatThrownBy(() -> service.upload(new MockMultipartFile("imageFile", "bad.txt", "text/plain", new byte[]{1}), "products"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @EnabledIfSystemProperty(named = "cloudinary.live", matches = "true")
    void uploadToConfiguredAccountAndRemoveOnlyTheTestImage() throws Exception {
        Properties properties = new Properties();
        try (var in = Files.newInputStream(Path.of("src/main/resources/application.properties"))) { properties.load(in); }
        Cloudinary sdk = new Cloudinary(ObjectUtils.asMap("cloud_name", credential(properties, "cloudinary.cloud-name"),
                "api_key", credential(properties, "cloudinary.api-key"), "api_secret", credential(properties, "cloudinary.api-secret"), "secure", true));
        var result = new CloudinaryService(sdk).upload(image(), "rental_shop_checks");
        try {
            assertThat(result.publicId()).startsWith("rental_shop_checks/");
            assertThat(result.secureUrl()).startsWith("https://res.cloudinary.com/" + sdk.config.cloudName + "/");
            var request = java.net.http.HttpRequest.newBuilder(java.net.URI.create(result.secureUrl())).GET().build();
            assertThat(java.net.http.HttpClient.newHttpClient().send(request, java.net.http.HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(200);
        } finally {
            assertThat(sdk.uploader().destroy(result.publicId(), ObjectUtils.emptyMap()).get("result")).isEqualTo("ok");
        }
    }

    private static String credential(Properties properties, String name) {
        String value = properties.getProperty(name);
        if (!value.startsWith("${")) return value;
        int colon = value.indexOf(':');
        String variable = value.substring(2, colon);
        return System.getenv().getOrDefault(variable, value.substring(colon + 1, value.length() - 1));
    }

    private static MockMultipartFile image() throws IOException {
        var bytes = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_RGB), "png", bytes);
        return new MockMultipartFile("imageFile", "cloudinary-check.png", "image/png", bytes.toByteArray());
    }
}
