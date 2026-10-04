package com.finalproject.service;

import com.finalproject.dto.request.AddToCartRequest;
import com.finalproject.entity.Cart;

public interface CartService {
    Cart getCartByUsername(String username);
    int getCartItemCount(String username);
    /** Thêm sản phẩm vào giỏ. Trả về tổng số lượng mới của toàn giỏ sau khi lưu. */
    int addToCart(String username, AddToCartRequest request);
    void updateQuantity(String username, Long cartItemId, Integer quantity);
    void removeCartItem(String username, Long cartItemId);
    void clearCart(String username);
}