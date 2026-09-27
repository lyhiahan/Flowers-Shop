package com.finalproject.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadDir = Paths.get("upload");
        String uploadPath = uploadDir.toFile().getAbsolutePath();

        // Cấu hình phục vụ file tĩnh từ thư mục /upload/** cả từ file hệ thống và từ classpath static
        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:" + uploadPath + "/", "classpath:/static/upload/");
    }
}
