package com.finalproject.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.finalproject.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Triển khai CloudinaryService:
 * - Kiểm tra định dạng (JPEG/PNG/WEBP/GIF) và kích thước file (tối đa 10 MB)
 * - Upload lên Cloudinary, trả về UploadResult (secureUrl + publicId)
 * - Hỗ trợ xóa ảnh cũ qua publicId
 * - Fallback về lưu cục bộ (chỉ khi Cloudinary chưa cấu hình – môi trường dev)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryServiceImpl implements CloudinaryService {

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024; // 10 MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".webp", ".gif"
    );

    private final Cloudinary cloudinary;


    // ─── Upload ───────────────────────────────────────────────────────────────

    @Override
    public UploadResult upload(MultipartFile file, String folder) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        // 1. Kiểm tra định dạng và kích thước
        validateFile(file);

        // 2. Kiểm tra xem Cloudinary đã cấu hình thật chưa
        boolean cloudinaryConfigured = isCloudinaryConfigured();

        if (cloudinaryConfigured) {
            try {
                Map<?, ?> uploadResult = cloudinary.uploader().upload(
                        file.getBytes(),
                        ObjectUtils.asMap(
                                "folder", folder != null ? folder : "rental_shop_general",
                                "resource_type", "image",
                                "allowed_formats", new String[]{"jpg", "jpeg", "png", "webp", "gif"},
                                "transformation", new com.cloudinary.Transformation().quality("auto")
                        )
                );
                if (uploadResult != null
                        && uploadResult.get("secure_url") != null
                        && uploadResult.get("public_id") != null) {
                    String secureUrl = uploadResult.get("secure_url").toString();
                    String publicId = uploadResult.get("public_id").toString();
                    log.info("Cloudinary upload OK: publicId={}, url={}", publicId, secureUrl);
                    return new UploadResult(secureUrl, publicId);
                }
                throw new IOException("Cloudinary không trả về thông tin ảnh hợp lệ.");
            } catch (Exception ex) {
                log.warn("Cloudinary upload failed: {}", ex.getMessage());
                throw new IOException("Không thể tải ảnh lên Cloudinary. Vui lòng kiểm tra cấu hình hoặc thử lại.", ex);
            }
        } else {
            log.warn("Cloudinary chưa cấu hình. Dùng lưu cục bộ.");
        }

        // 3. Fallback: lưu cục bộ (chỉ cho môi trường phát triển)
        return saveLocally(file);
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    @Override
    public void delete(String publicId) {
        if (publicId == null || publicId.isBlank() || !isCloudinaryConfigured()) {
            return;
        }
        // Bỏ qua URL local (không phải Cloudinary public_id)
        if (publicId.startsWith("/upload/") || publicId.startsWith("http")) {
            return;
        }
        try {
            Map<?, ?> result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Cloudinary delete publicId={}: result={}", publicId, result.get("result"));
        } catch (Exception ex) {
            // Best-effort: không throw, chỉ log
            log.warn("Cloudinary delete thất bại publicId={}: {}", publicId, ex.getMessage());
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Kiểm tra file hợp lệ: định dạng và kích thước
     */
    private void validateFile(MultipartFile file) {
        // Kiểm tra kích thước
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException(
                    String.format("File quá lớn (%.1f MB). Tối đa cho phép là 10 MB.",
                            file.getSize() / (1024.0 * 1024.0)));
        }

        // Kiểm tra Content-Type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Định dạng ảnh không hợp lệ! Chỉ chấp nhận JPG, PNG, WEBP, GIF. " +
                    "Nhận được: " + contentType);
        }

        // Kiểm tra phần mở rộng tên file (bảo vệ kép)
        String originalName = file.getOriginalFilename();
        if (originalName != null) {
            String lower = originalName.toLowerCase();
            boolean validExt = ALLOWED_EXTENSIONS.stream().anyMatch(lower::endsWith);
            if (!validExt) {
                throw new IllegalArgumentException(
                        "Phần mở rộng file không hợp lệ! Chấp nhận: .jpg .jpeg .png .webp .gif");
            }
        }
    }

    /**
     * Kiểm tra xem Cloudinary đã cấu hình thật chưa (không phải placeholder)
     */
    private boolean isCloudinaryConfigured() {
        try {
            Object key = cloudinary.config.apiKey;
            if (key == null) return false;
            String keyStr = key.toString().trim();
            return cloudinary.config.cloudName != null && !cloudinary.config.cloudName.isBlank()
                    && cloudinary.config.apiSecret != null && !cloudinary.config.apiSecret.isBlank()
                    && !"placeholder_secret".equals(cloudinary.config.apiSecret)
                    && !keyStr.isEmpty()
                    && !"123456789".equals(keyStr)
                    && !"test".equals(keyStr)
                    && !"placeholder".equals(keyStr);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Lưu ảnh cục bộ (chỉ dùng cho môi trường phát triển khi Cloudinary chưa sẵn sàng)
     */
    private UploadResult saveLocally(MultipartFile file) throws IOException {
        String extension = ".jpg";
        String origName = file.getOriginalFilename();
        if (origName != null && origName.contains(".")) {
            extension = origName.substring(origName.lastIndexOf(".")).toLowerCase();
        }
        String fileName = "img_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12) + extension;

        Path rootUploadDir = Paths.get("upload/avatar");
        if (!Files.exists(rootUploadDir)) {
            Files.createDirectories(rootUploadDir);
        }
        Files.copy(file.getInputStream(), rootUploadDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);

        // Đồng bộ sang static để dev server phục vụ được
        Path staticDir = Paths.get("src/main/resources/static/upload/avatar");
        if (!Files.exists(staticDir)) Files.createDirectories(staticDir);
        Files.copy(rootUploadDir.resolve(fileName), staticDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);

        Path targetDir = Paths.get("target/classes/static/upload/avatar");
        if (!Files.exists(targetDir)) Files.createDirectories(targetDir);
        try {
            Files.copy(rootUploadDir.resolve(fileName), targetDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) { /* target chưa tồn tại khi chạy test */ }

        String localUrl = "/upload/avatar/" + fileName;
        log.info("[Dev] Lưu ảnh cục bộ: {}", localUrl);
        return new UploadResult(localUrl, null); // publicId = null với ảnh cục bộ
    }
}
