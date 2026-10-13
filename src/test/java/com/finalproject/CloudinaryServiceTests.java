package com.finalproject;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.cloudinary.Transformation;
import com.finalproject.service.impl.CloudinaryServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CloudinaryServiceTests {
    private Cloudinary configuredClient() {
        return spy(new Cloudinary(Map.of("cloud_name", "example-cloud", "api_key", "example-key",
                "api_secret", "example-secret", "secure", true)));
    }

    private MockMultipartFile image() {
        return new MockMultipartFile("image", "flower.png", "image/png", new byte[]{1, 2, 3});
    }

    @Test
    void successfulUploadReturnsRemoteUrlAndPublicId() throws Exception {
        Cloudinary client = configuredClient();
        Uploader uploader = mock(Uploader.class);
        doReturn(uploader).when(client).uploader();
        when(uploader.upload(any(), anyMap())).thenReturn(Map.of(
                "secure_url", "https://res.cloudinary.com/example-cloud/image/upload/flower.png",
                "public_id", "flowers/flower"));

        var result = new CloudinaryServiceImpl(client).upload(image(), "flowers");
        assertThat(result.publicId()).isEqualTo("flowers/flower");
        assertThat(result.secureUrl()).startsWith("https://res.cloudinary.com/");
        verify(uploader).upload(any(), argThat(options ->
                options.get("transformation") instanceof Transformation
                        && "q_auto".equals(options.get("transformation").toString())));
    }

    @Test
    void configuredUploadFailureIsReportedInsteadOfSavingLocally() throws Exception {
        Cloudinary client = configuredClient();
        Uploader uploader = mock(Uploader.class);
        doReturn(uploader).when(client).uploader();
        when(uploader.upload(any(), anyMap())).thenThrow(new IOException("Connection failed"));

        assertThatThrownBy(() -> new CloudinaryServiceImpl(client).upload(image(), "flowers"))
                .isInstanceOf(IOException.class).hasMessageContaining("Cloudinary");
    }

    @Test
    void incompleteRemoteResponseIsRejected() throws Exception {
        Cloudinary client = configuredClient();
        Uploader uploader = mock(Uploader.class);
        doReturn(uploader).when(client).uploader();
        when(uploader.upload(any(), anyMap())).thenReturn(Map.of("public_id", "flowers/flower"));

        assertThatThrownBy(() -> new CloudinaryServiceImpl(client).upload(image(), "flowers"))
                .isInstanceOf(IOException.class).hasMessageContaining("public_id");
    }

    @Test
    void invalidFileIsRejectedBeforeCallingCloudinary() {
        Cloudinary client = configuredClient();
        var file = new MockMultipartFile("image", "flower.html", "text/html", new byte[]{1});
        assertThatThrownBy(() -> new CloudinaryServiceImpl(client).upload(file, "flowers"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(client, never()).uploader();
    }
}
