package com.finalproject.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Service tích hợp Cloudinary để upload/xóa ảnh trên đám mây.
 * Thiết kế: Mọi ảnh upload lên đều lưu kèm public_id để có thể xóa sau.
 */
public interface CloudinaryService {

    /**
     * Kết quả upload: chứa URL an toàn (HTTPS) và public_id để quản lý xóa/thay.
     */
    record UploadResult(String secureUrl, String publicId) {}

    /**
     * Upload ảnh lên Cloudinary (folder mặc định), kiểm tra định dạng và kích thước.
     * @param file   File ảnh từ form upload
     * @param folder Thư mục Cloudinary (vd: "products", "avatars", "stores")
     * @return UploadResult chứa secureUrl và publicId
     * @throws IOException nếu đọc file thất bại
     * @throws IllegalArgumentException nếu file không hợp lệ (sai định dạng/kích thước)
     */
    UploadResult upload(MultipartFile file, String folder) throws IOException;

    /**
     * Xóa ảnh cũ trên Cloudinary theo public_id.
     * Nếu xóa thất bại chỉ log warning, không throw exception (best-effort).
     * @param publicId public_id của ảnh cần xóa
     */
    void delete(String publicId);

    /**
     * @deprecated Dùng {@link #upload(MultipartFile, String)} thay thế.
     *             Giữ lại để backward-compat với các controller cũ trong quá trình chuyển đổi.
     */
    @Deprecated(since = "2.0", forRemoval = true)
    default String uploadImage(MultipartFile file) throws IOException {
        UploadResult result = upload(file, "rental_shop_general");
        return result != null ? result.secureUrl() : null;
    }
}