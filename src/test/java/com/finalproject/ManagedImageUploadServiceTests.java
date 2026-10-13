package com.finalproject;

import com.finalproject.service.CloudinaryService;
import com.finalproject.service.ManagedImageUploadService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.mockito.Mockito.*;

class ManagedImageUploadServiceTests {
    @AfterEach void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive())
            TransactionSynchronizationManager.clearSynchronization();
    }

    @Test void rollbackDeletesNewAsset() throws Exception {
        var cloud = mock(CloudinaryService.class);
        var file = new MockMultipartFile("image", new byte[]{1});
        when(cloud.upload(file, "photos")).thenReturn(new CloudinaryService.UploadResult("https://example.com/image.png", "photos/new"));
        TransactionSynchronizationManager.initSynchronization();
        new ManagedImageUploadService(cloud).upload(file, "photos");
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        verify(cloud).delete("photos/new");
    }

    @Test void commitKeepsNewAsset() throws Exception {
        var cloud = mock(CloudinaryService.class);
        var file = new MockMultipartFile("image", new byte[]{1});
        when(cloud.upload(file, "photos")).thenReturn(new CloudinaryService.UploadResult("https://example.com/image.png", "photos/new"));
        TransactionSynchronizationManager.initSynchronization();
        new ManagedImageUploadService(cloud).upload(file, "photos");
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
        verify(cloud, never()).delete(any());
    }

    @Test void oldAssetIsDeletedOnlyAfterCommit() {
        var cloud = mock(CloudinaryService.class);
        TransactionSynchronizationManager.initSynchronization();
        new ManagedImageUploadService(cloud).deleteAfterCommit("photos/old");
        verifyNoInteractions(cloud);
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(cloud).delete("photos/old");
    }
}
