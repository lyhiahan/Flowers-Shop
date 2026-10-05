package com.finalproject.service;

import com.finalproject.entity.Product;
public final class ProductAvailability {
    private ProductAvailability() {}
    public static boolean isPublic(Product p) {
        return p != null && !Boolean.TRUE.equals(p.getIsDeleted()) && Boolean.TRUE.equals(p.getIsActive())
                && Boolean.TRUE.equals(p.getIsSelling()) && (p.getStore() == null
                || (Boolean.TRUE.equals(p.getStore().getStatus()) && Boolean.TRUE.equals(p.getStore().getIsOpen())));
    }
}
