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
    private final com.finalproject.repository.StoreRepository stores;
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

    /**
     * Add physical stock to one branch without rewriting sales or stock at other branches.
     * Only the admin endpoint calls this method; managers use their assigned-store flow.
     */
    @Transactional
    public int restock(Long productId, Long storeId, int addedQuantity) {
        if (addedQuantity <= 0 || addedQuantity > 1000000)
            throw new IllegalArgumentException("Số lượng nhập thêm phải từ 1 đến 1.000.000.");
        var product = products.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));
        if (Boolean.TRUE.equals(product.getIsDeleted()))
            throw new IllegalArgumentException("Sản phẩm đã bị xóa; hãy khôi phục trước khi nhập hàng.");
        var store = stores.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi nhánh."));
        if (!ProductAvailability.supportsBranch(product, store))
            throw new IllegalArgumentException("Sản phẩm chỉ thuộc chi nhánh sở hữu, không thể nhập sang chi nhánh khác.");
        var inventory = inventories.findLockedStock(storeId, productId)
                .orElseGet(() -> StoreInventory.builder().store(store).product(product).quantity(0).build());
        if (inventory.getId() != null) entityManager.refresh(inventory, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (Boolean.TRUE.equals(inventory.getIsDeleted()))
            throw new IllegalArgumentException("Kho sản phẩm tại chi nhánh đã lưu trữ; hãy khôi phục trước khi nhập hàng.");
        int current = inventory.getQuantity() == null ? 0 : inventory.getQuantity();
        if (current < 0 || addedQuantity > 1000000 - current)
            throw new IllegalArgumentException("Tồn kho sau nhập không được vượt quá 1.000.000.");
        inventory.setQuantity(current + addedQuantity);
        inventories.save(inventory);
        return inventory.getQuantity();
    }

    @Transactional
    public int restockForManager(Long productId, int addedQuantity) {
        var store = access.assignedStore();
        access.requireManager(store);
        var product = products.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));
        if (product.getStore() == null && inventories.findByStoreIdAndProductId(store.getId(), productId).isEmpty())
            throw new org.springframework.security.access.AccessDeniedException(
                    "Sản phẩm chung chưa có trong kho của chi nhánh này.");
        return restock(productId, store.getId(), addedQuantity);
    }
}
