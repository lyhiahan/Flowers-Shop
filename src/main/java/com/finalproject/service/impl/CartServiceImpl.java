package com.finalproject.service.impl;

import com.finalproject.dto.request.AddToCartRequest;
import com.finalproject.entity.*;
import com.finalproject.exception.ResourceNotFoundException;
import com.finalproject.repository.*;
import com.finalproject.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;

    @Override
    public Cart getCartByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> cartRepository.save(Cart.builder().user(user).items(new ArrayList<>()).build()));
    }

    @Override
    @Transactional
    public void addToCart(String username, AddToCartRequest request) {
        Cart cart = getCartByUsername(username);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại"));

        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new ResourceNotFoundException("Chi nhánh không tồn tại"));

        CartItem cartItem = CartItem.builder()
                .cart(cart)
                .product(product)
                .store(store)
                .orderType(request.getOrderType())
                .quantity(request.getQuantity())
                .rentStartDate(request.getRentStartDate())
                .rentEndDate(request.getRentEndDate())
                .build();

        cartItemRepository.save(cartItem);
    }

    @Override
    @Transactional
    public void removeCartItem(String username, Long cartItemId) {
        cartItemRepository.deleteById(cartItemId);
    }

    @Override
    @Transactional
    public void clearCart(String username) {
        Cart cart = getCartByUsername(username);
        cartItemRepository.deleteByCartId(cart.getId());
    }
}