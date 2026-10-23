package com.finalproject.service;

import com.finalproject.entity.Product;
public final class ProductAvailability {
    private ProductAvailability() {}
    /** Branch-owned products are not chain-wide SKUs, even if legacy inventory rows exist elsewhere. */
    public static boolean supportsBranch(Product product, com.finalproject.entity.Store store) {
        return product != null && store != null && (product.getStore() == null
                || java.util.Objects.equals(product.getStore().getId(), store.getId()));
    }
    public static boolean isPublic(Product p) {
        return isVisible(p) && Boolean.TRUE.equals(p.getIsSelling());
    }
    /** Paused products remain viewable, but cannot be purchased. */
    public static boolean isVisible(Product p) {
        return p != null && !Boolean.TRUE.equals(p.getIsDeleted()) && Boolean.TRUE.equals(p.getIsActive())
                && (p.getStore() == null
                || (Boolean.TRUE.equals(p.getStore().getStatus()) && Boolean.TRUE.equals(p.getStore().getIsOpen())));
    }
}
