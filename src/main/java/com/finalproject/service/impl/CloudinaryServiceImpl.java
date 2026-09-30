package com.finalproject.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.finalproject.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    @Override
    public String uploadImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap("folder", "rental_shop_products")
            );
            if (uploadResult != null && uploadResult.get("secure_url") != null) {
                return uploadResult.get("secure_url").toString();
            }
        } catch (Exception ex) {
            log.warn("Cloudinary upload failed: {}, fallback to local storage", ex.getMessage());
        }

        // Fallback: Lưu file cục bộ vào thư mục upload/avatar/ theo chuẩn WebMvcConfig của dự án
        String extension = ".jpg";
        String origName = file.getOriginalFilename();
        if (origName != null && origName.contains(".")) {
            extension = origName.substring(origName.lastIndexOf("."));
        }
        String fileName = "avatar_" + UUID.randomUUID().toString().substring(0, 8) + extension;

        // 1. Lưu vào thư mục upload/avatar ở gốc dự án (WebMvcConfig phục vụ từ file:upload/)
        Path rootUploadDir = Paths.get("upload/avatar");
        if (!Files.exists(rootUploadDir)) {
            Files.createDirectories(rootUploadDir);
        }
        Files.copy(file.getInputStream(), rootUploadDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);

        // 2. Lưu đồng thời vào static/upload/avatar để đồng bộ mã nguồn và target
        Path staticUploadDir = Paths.get("src/main/resources/static/upload/avatar");
        if (!Files.exists(staticUploadDir)) {
            Files.createDirectories(staticUploadDir);
        }
        Files.copy(rootUploadDir.resolve(fileName), staticUploadDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);

        Path targetUploadDir = Paths.get("target/classes/static/upload/avatar");
        if (!Files.exists(targetUploadDir)) {
            Files.createDirectories(targetUploadDir);
        }
        Files.copy(rootUploadDir.resolve(fileName), targetUploadDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);

        return "/upload/avatar/" + fileName;
    }
}