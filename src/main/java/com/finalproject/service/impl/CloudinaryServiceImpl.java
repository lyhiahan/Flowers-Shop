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

        // Fallback: Lưu file cục bộ vào thư mục tĩnh static/images/
        String extension = ".jpg";
        String origName = file.getOriginalFilename();
        if (origName != null && origName.contains(".")) {
            extension = origName.substring(origName.lastIndexOf("."));
        }
        String fileName = "upload_" + UUID.randomUUID().toString().substring(0, 8) + extension;

        Path targetDir = Paths.get("target/classes/static/images");
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }
        Files.copy(file.getInputStream(), targetDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);

        Path srcDir = Paths.get("src/main/resources/static/images");
        if (!Files.exists(srcDir)) {
            Files.createDirectories(srcDir);
        }
        Files.copy(targetDir.resolve(fileName), srcDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);

        return "/images/" + fileName;
    }
}