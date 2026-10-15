package com.finalproject.service;

import com.finalproject.entity.*;
import com.finalproject.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class StoreProductService {
    private final StoreAccessService access;
    private final ProductRepository products;
    private final StoreInventoryRepository inventories;
    private final OrderDetailRepository details;
    private final jakarta.persistence.EntityManager entityManager;

    public static Specification<Product> catalog(Long storeId, String tab) {
        return catalog(storeId, tab, null, null);
    }

    public static Specification<Product> catalog(Long storeId, String tab, String keyword, Long categoryId) {
        return (root, query, cb) -> {
            var owner = root.join("store", jakarta.persistence.criteria.JoinType.LEFT);
            var present = query.subquery(Long.class); var local = present.from(StoreInventory.class);
            present.select(local.get("id")).where(cb.equal(local.get("product").get("id"), root.get("id")), cb.equal(local.get("store").get("id"), storeId));
            var removed = query.subquery(Long.class); var hidden = removed.from(StoreInventory.class);
            removed.select(hidden.get("id")).where(cb.equal(hidden.get("product").get("id"), root.get("id")),
                    cb.equal(hidden.get("store").get("id"), storeId), cb.isTrue(hidden.get("isDeleted")));
            var stopped = query.subquery(Long.class); var paused = stopped.from(StoreInventory.class);
            stopped.select(paused.get("id")).where(cb.equal(paused.get("product").get("id"), root.get("id")),
                    cb.equal(paused.get("store").get("id"), storeId), cb.isFalse(paused.get("isSelling")));
            // In SQL Server, BIT columns must use explicit comparisons (= 1 / = 0 / IS NULL)
            // so Hibernate renders proper boolean conditions instead of bare BIT or coalesce() expressions.
            // Legacy rows predate soft deletion; NULL means not deleted (see Product.getIsDeleted()).
            var deleted = cb.or(cb.isTrue(root.get("isDeleted")), cb.exists(removed));
            var notDeleted = cb.and(cb.or(cb.isNull(root.get("isDeleted")), cb.isFalse(root.get("isDeleted"))), cb.not(cb.exists(removed)));
            var selling = cb.and(cb.isTrue(root.get("isActive")),
                    cb.isTrue(root.get("isSelling")), cb.not(cb.exists(stopped)));
            var notSelling = cb.or(cb.isFalse(root.get("isActive")),
                    cb.isFalse(root.get("isSelling")), cb.exists(stopped));
            var scope = cb.or(cb.equal(owner.get("id"), storeId), cb.and(cb.isNull(owner.get("id")), cb.exists(present)));

            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(scope);
            if ("deleted".equals(tab)) predicates.add(deleted);
            else if ("stored".equals(tab)) { predicates.add(notDeleted); predicates.add(notSelling); }
            else if ("all".equals(tab)) predicates.add(notDeleted);
            else { predicates.add(notDeleted); predicates.add(selling); }

            if (keyword != null && !keyword.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + keyword.trim().toLowerCase() + "%"));
            }
            if (categoryId != null && categoryId > 0) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    @Transactional(readOnly=true)
    public Product requireSelectedProduct(Long id) {
        Store store = access.assignedStore(); access.requireManager(store);
        var product = products.findById(id).orElseThrow();
        if (!ProductAvailability.supportsBranch(product, store)
                || (product.getStore() == null && inventories.findByStoreIdAndProductId(store.getId(), id).isEmpty()))
            throw new AccessDeniedException("Mẫu hoa không thuộc danh mục của chi nhánh đang chọn.");
        return product;
    }

    private StoreInventory lockedInventory(Product product) {
        Store store = access.assignedStore();
        var inventory = inventories.findLockedStock(store.getId(), product.getId())
                .orElseGet(() -> inventories.saveAndFlush(StoreInventory.builder().store(store).product(product).quantity(0).build()));
        entityManager.refresh(inventory, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        return inventory;
    }

    @Transactional
    public void setSelling(Long id, boolean selling) {
        var product = requireSelectedProduct(id); var inventory = lockedInventory(product);
        if (Boolean.TRUE.equals(product.getIsDeleted()) || Boolean.TRUE.equals(inventory.getIsDeleted()))
            throw new IllegalArgumentException("Khôi phục sản phẩm trước khi mở bán.");
        if (selling && product.getStore() == null && !ProductAvailability.isPublic(product))
            throw new IllegalArgumentException("Mẫu chung đang bị hệ thống ngừng bán hoặc chưa được duyệt.");
        inventory.setIsSelling(selling);
        if (product.getStore() != null) product.setIsSelling(selling);
    }

    @Transactional
    public void delete(Long id) {
        var product = requireSelectedProduct(id); var inventory = lockedInventory(product);
        if (details.hasOpenOrdersAtStore(id, inventory.getStore().getId()))
            throw new IllegalArgumentException("Sản phẩm còn đơn hàng đang xử lý. Hãy ngừng bán và hoàn tất đơn trước khi xóa.");
        inventory.setIsDeleted(true); inventory.setIsSelling(false);
        if (product.getStore() != null) { product.setIsDeleted(true); product.setIsSelling(false); }
        // No stock reset, order deletion or Cloudinary deletion.
    }

    @Transactional
    public void restore(Long id) {
        var product = requireSelectedProduct(id); var inventory = lockedInventory(product);
        inventory.setIsDeleted(false); inventory.setIsSelling(false);
        if (product.getStore() != null) { product.setIsDeleted(false); product.setIsSelling(false); }
    }

    @Transactional
    public void updateShared(Long id, int quantity, boolean selling) {
        if (quantity < 0 || quantity > 1000000) throw new IllegalArgumentException("Tồn kho phải từ 0 đến 1.000.000.");
        var product = requireSelectedProduct(id);
        if (product.getStore() != null) throw new IllegalArgumentException("Dùng form sửa sản phẩm riêng của chi nhánh.");
        var inventory = lockedInventory(product);
        setSelling(id, selling); inventory.setQuantity(quantity);
    }
}
