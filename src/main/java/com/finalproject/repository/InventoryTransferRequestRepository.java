package com.finalproject.repository;

import com.finalproject.entity.InventoryTransferRequest;
import com.finalproject.enums.TransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryTransferRequestRepository extends JpaRepository<InventoryTransferRequest, Long> {
    List<InventoryTransferRequest> findByFromStoreIdOrToStoreId(Long fromStoreId, Long toStoreId);
    List<InventoryTransferRequest> findByStatus(TransferStatus status);
}