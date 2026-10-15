package com.finalproject.service;

import com.finalproject.entity.StoreInventory;
import com.finalproject.repository.ProductRepository;
import com.finalproject.repository.StoreInventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private final StoreAccessService access;
    private final ProductRepository products;
    private final StoreInventoryRepository inventories;
    private final jakarta.persistence.EntityManager entityManager;

    @Transactional
    public void setStock(Long productId, int quantity) {
        if (quantity < 0 || quantity > 1000000) throw new IllegalArgumentException("Tồn kho phải từ 0 đến 1.000.000.");
        var product = products.findById(productId).orElseThrow();
        var store = access.assignedStore();
        access.requireManager(store);
        if (!ProductAvailability.supportsBranch(product, store) || Boolean.TRUE.equals(product.getIsDeleted()))
            throw new org.springframework.security.access.AccessDeniedException("Sản phẩm không thuộc danh mục đang hoạt động của chi nhánh này.");
        if (product.getStore() == null && inventories.findByStoreIdAndProductId(store.getId(), productId).isEmpty())
            throw new org.springframework.security.access.AccessDeniedException("Mẫu chung chưa có trong kho của chi nhánh này.");
        var inventory = inventories.findLockedStock(store.getId(), productId)
                .orElseGet(() -> StoreInventory.builder().store(store).product(product).quantity(0).build());
        if (inventory.getId() != null) entityManager.refresh(inventory, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (Boolean.TRUE.equals(inventory.getIsDeleted())) throw new IllegalArgumentException("Khôi phục sản phẩm trước khi chỉnh sửa tồn kho.");
        inventory.setQuantity(quantity);
        inventories.save(inventory);
    }
}
