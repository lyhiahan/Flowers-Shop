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
        access.requireManager(product.getStore());
        var store = product.getStore();
        var inventory = inventories.findLockedStock(store.getId(), productId)
                .orElseGet(() -> StoreInventory.builder().store(store).product(product).quantity(0).build());
        if (inventory.getId() != null) entityManager.refresh(inventory, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        inventory.setQuantity(quantity);
        inventories.save(inventory);
    }
}
