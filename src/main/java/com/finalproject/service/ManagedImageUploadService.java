package com.finalproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/** Remove newly uploaded assets if the corresponding database transaction fails. */
@Service
@RequiredArgsConstructor
public class ManagedImageUploadService {
    private final CloudinaryService cloudinary;

    public CloudinaryService.UploadResult upload(MultipartFile file, String folder) throws IOException {
        if (file == null || file.isEmpty()) return null;
        var result = cloudinary.upload(file, folder);
        if (result == null) throw new IOException("Không tải được ảnh. Vui lòng thử lại.");
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) cloudinary.delete(result.publicId());
                }
            });
        }
        return result;
    }

    public void deleteAfterCommit(String publicId) {
        if (publicId == null || publicId.isBlank()) return;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { cloudinary.delete(publicId); }
            });
        } else {
            cloudinary.delete(publicId);
        }
    }
}
