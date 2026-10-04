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
    private final StoreInventoryRepository storeInventoryRepository;

    @Override
    public Cart getCartByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> cartRepository.save(Cart.builder().user(user).items(new ArrayList<>()).build()));
    }

    @Override
    public int getCartItemCount(String username) {
        return Math.toIntExact(cartItemRepository.countUnitsByUsername(username));
    }
    @Override
    @Transactional
    public void addToCart(String username, AddToCartRequest request) {
        if (request == null || request.getProductId() == null || request.getProductId() <= 0) throw new IllegalArgumentException("Vui lòng chọn sản phẩm hợp lệ.");
        Cart cart = getCartForUpdate(username);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại"));

        if (request.getOrderType() != com.finalproject.enums.OrderType.BUY) throw new IllegalArgumentException("Hệ thống chỉ hỗ trợ mua hoa.");
        if (Boolean.TRUE.equals(product.getIsDeleted()) || !Boolean.TRUE.equals(product.getIsActive()) || !Boolean.TRUE.equals(product.getIsSelling())) throw new IllegalStateException("Sản phẩm đã ngừng bán.");
        if (request.getQuantity() == null || request.getQuantity() <= 0 || request.getQuantity() > 10000) throw new IllegalArgumentException("Số lượng không hợp lệ.");
        if (request.getDeliveryDate() != null && request.getDeliveryDate().isBefore(java.time.LocalDate.now())) throw new IllegalArgumentException("Ngày nhận hàng không được ở quá khứ.");
        Long storeId = request.getStoreId() != null ? request.getStoreId() : (product.getStore() != null ? product.getStore().getId() : 1L);
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Chi nhánh không tồn tại"));

        // Kiểm tra tồn kho tại chi nhánh: Không cho phép thêm nếu hết hàng hoặc không có hàng
        StoreInventory inventory = storeInventoryRepository.findByStoreIdAndProductId(store.getId(), product.getId())
                .orElse(null);
        int availableStock = (inventory != null && inventory.getQuantity() != null) ? inventory.getQuantity() : 0;
        if (availableStock <= 0) {
            throw new IllegalStateException("Mẫu hoa '" + product.getName() + "' hiện đang tạm hết hàng tại " + store.getName() + ". Quý khách vui lòng chọn chi nhánh khác hoặc mẫu hoa khác.");
        }

        int addQty = (request.getQuantity() != null && request.getQuantity() > 0) ? request.getQuantity() : 1;
        requireAvailableStock(cart, product, store, null, addQty);
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
            int newTotal = existingItem.getQuantity() + addQty;
            if (newTotal > availableStock) {
                throw new IllegalStateException("Tổng số lượng trong giỏ (" + newTotal + " bó) vượt quá tồn kho còn lại (" + availableStock + " bó) tại " + store.getName() + ".");
            }
            existingItem.setQuantity(newTotal);
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
            cart.getItems().add(cartItem);
        }
    }

    @Override
    @Transactional
    public void updateQuantity(String username, Long cartItemId, Integer quantity) {
        if (quantity == null || quantity < 0) throw new IllegalArgumentException("Số lượng không được âm hoặc bỏ trống.");
        Cart cart = getCartForUpdate(username);
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Mục giỏ hàng không tồn tại"));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền thao tác trên mục giỏ hàng này");
        }

        if (quantity == null || quantity <= 0) {
            if (cart.getItems() != null) {
                cart.getItems().removeIf(ci -> ci.getId() != null && ci.getId().equals(cartItemId));
            }
            cartItemRepository.delete(item);
            cartItemRepository.flush();
        } else {
            if (quantity > 10000) throw new IllegalArgumentException("Số lượng không hợp lệ.");
            requireAvailableStock(cart, item.getProduct(), item.getStore(), item.getId(), quantity);
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }
    }

    @Override
    @Transactional
    public void removeCartItem(String username, Long cartItemId) {
        Cart cart = getCartForUpdate(username);
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElse(null);

        if (item == null) {
            return;
        }

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền thao tác trên mục giỏ hàng này");
        }

        if (cart.getItems() != null) {
            cart.getItems().removeIf(ci -> ci.getId() != null && ci.getId().equals(cartItemId));
        }
        cartItemRepository.delete(item);
        cartItemRepository.flush();
    }

    @Override
    @Transactional
    public void clearCart(String username) {
        Cart cart = getCartForUpdate(username);
        if (cart.getItems() != null) {
            cart.getItems().clear();
        }
        cartItemRepository.deleteByCartId(cart.getId());
        cartItemRepository.flush();
    }
    private void requireAvailableStock(Cart cart, Product product, Store store, Long excludedItemId, int quantity) {
        int available = storeInventoryRepository.findByStoreIdAndProductId(store.getId(), product.getId())
                .map(stock -> Math.max(0, stock.getQuantity())).orElse(0);
        long requested = quantity;
        for (CartItem line : cart.getItems()) {
            if (!java.util.Objects.equals(line.getId(), excludedItemId)
                    && line.getProduct().getId().equals(product.getId()) && line.getStore().getId().equals(store.getId())) {
                requested += line.getQuantity();
            }
        }
        if (requested > available) throw new IllegalArgumentException("Chi nhánh chỉ còn " + available
                + " mẫu " + product.getName() + ". Số lượng trong giỏ không được vượt quá tồn kho.");
    }

    private Cart getCartForUpdate(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        if (!Boolean.TRUE.equals(user.getIsActive()) || user.isRetiredDeliveryAccount()) {
            throw new org.springframework.security.access.AccessDeniedException("Tài khoản đã bị khóa hoặc vô hiệu hóa.");
        }
        Cart cart = cartRepository.findLockedByUserId(user.getId())
                .orElseGet(() -> cartRepository.save(Cart.builder().user(user).items(new ArrayList<>()).build()));
        return cart;
    }
}
