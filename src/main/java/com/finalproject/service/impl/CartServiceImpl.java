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
    public int getCartItemCount(String username) {
        try {
            Cart cart = getCartByUsername(username);
            if (cart == null || cart.getItems() == null) {
                return 0;
            }
            return cart.getItems().stream()
                    .mapToInt(item -> item.getQuantity() != null ? item.getQuantity() : 1)
                    .sum();
        } catch (Exception ex) {
            return 0;
        }
    }

    @Override
    @Transactional
    public void addToCart(String username, AddToCartRequest request) {
        Cart cart = getCartByUsername(username);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại"));

        Long storeId = request.getStoreId() != null ? request.getStoreId() : (product.getStore() != null ? product.getStore().getId() : 1L);
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Chi nhánh không tồn tại"));

        int addQty = (request.getQuantity() != null && request.getQuantity() > 0) ? request.getQuantity() : 1;
        java.time.LocalDate startDate = request.getRentStartDate();
        java.time.LocalDate endDate = request.getRentEndDate();

        if (request.getOrderType() == com.finalproject.enums.OrderType.RENT) {
            if (startDate == null) {
                startDate = java.time.LocalDate.now();
            }
            if (endDate == null || endDate.isBefore(startDate)) {
                endDate = startDate.plusDays(3);
            }
        } else {
            startDate = null;
            endDate = null;
        }

        java.time.LocalDate delivDate = request.getDeliveryDate();
        if (delivDate == null) {
            delivDate = java.time.LocalDate.now();
        }
        String greetingMsg = request.getGreetingMessage();

        // Kiểm tra xem sản phẩm tương tự đã có trong giỏ chưa
        final java.time.LocalDate finalStart = startDate;
        final java.time.LocalDate finalEnd = endDate;
        final java.time.LocalDate finalDeliv = delivDate;
        final String finalGreeting = greetingMsg;
        CartItem existingItem = null;
        if (cart.getItems() != null) {
            for (CartItem item : cart.getItems()) {
                if (item.getProduct().getId().equals(product.getId())
                        && item.getStore().getId().equals(store.getId())
                        && item.getOrderType() == request.getOrderType()
                        && java.util.Objects.equals(item.getRentStartDate(), finalStart)
                        && java.util.Objects.equals(item.getRentEndDate(), finalEnd)
                        && java.util.Objects.equals(item.getDeliveryDate(), finalDeliv)) {
                    existingItem = item;
                    break;
                }
            }
        }

        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + addQty);
            if (greetingMsg != null && !greetingMsg.isBlank()) {
                existingItem.setGreetingMessage(greetingMsg);
            }
            cartItemRepository.save(existingItem);
        } else {
            CartItem cartItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .store(store)
                    .orderType(request.getOrderType())
                    .quantity(addQty)
                    .rentStartDate(finalStart)
                    .rentEndDate(finalEnd)
                    .deliveryDate(finalDeliv)
                    .greetingMessage(finalGreeting)
                    .build();
            cartItemRepository.save(cartItem);
        }
    }

    @Override
    @Transactional
    public void updateQuantity(String username, Long cartItemId, Integer quantity) {
        Cart cart = getCartByUsername(username);
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Mục giỏ hàng không tồn tại"));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền thao tác trên mục giỏ hàng này");
        }

        if (quantity == null || quantity <= 0) {
            cartItemRepository.delete(item);
        } else {
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }
    }

    @Override
    @Transactional
    public void removeCartItem(String username, Long cartItemId) {
        Cart cart = getCartByUsername(username);
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Mục giỏ hàng không tồn tại"));

        if (item.getCart().getId().equals(cart.getId())) {
            cartItemRepository.delete(item);
        }
    }

    @Override
    @Transactional
    public void clearCart(String username) {
        Cart cart = getCartByUsername(username);
        cartItemRepository.deleteByCartId(cart.getId());
    }
}