package com.finalproject.service;

import com.finalproject.dto.request.AddToCartRequest;
import com.finalproject.entity.Cart;

public interface CartService {
    Cart getCartByUsername(String username);
    void addToCart(String username, AddToCartRequest request);
    void removeCartItem(String username, Long cartItemId);
    void clearCart(String username);
}