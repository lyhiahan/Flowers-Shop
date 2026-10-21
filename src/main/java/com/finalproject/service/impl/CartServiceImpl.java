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
        if (isAdminUser(user)) {
            throw new org.springframework.security.access.AccessDeniedException("Tài khoản Quản trị viên không thể sử dụng giỏ hàng.");
        }
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> cartRepository.save(Cart.builder().user(user).items(new ArrayList<>()).build()));
    }

    @Override
    public int getCartItemCount(String username) {
        if (username == null || username.isBlank()) return 0;
        User user = userRepository.findByUsername(username).orElse(null);
        if (user != null && isAdminUser(user)) {
            return 0;
        }
        return Math.toIntExact(cartItemRepository.countUnitsByUsername(username));
    }
    @Override
    @Transactional
    public int addToCart(String username, AddToCartRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không tồn tại."));
        if (isAdminUser(user)) {
            throw new org.springframework.security.access.AccessDeniedException("Tài khoản Quản trị viên không thể thực hiện thao tác mua hàng hoặc sử dụng giỏ hàng.");
        }

        // --- Kiểm tra đầu vào trước khi lấy khóa ---
        if (request == null || request.getProductId() == null || request.getProductId() <= 0)
            throw new IllegalArgumentException("Vui lòng chọn sản phẩm hợp lệ.");
        if (request.getOrderType() != com.finalproject.enums.OrderType.BUY)
            throw new IllegalArgumentException("Hệ thống chỉ hỗ trợ mua hoa.");
        int addQty = (request.getQuantity() != null && request.getQuantity() > 0) ? request.getQuantity() : 1;
        if (addQty > 10000) throw new IllegalArgumentException("Số lượng không hợp lệ.");
        if (request.getDeliveryDate() != null && request.getDeliveryDate().isBefore(java.time.LocalDate.now()))
            throw new IllegalArgumentException("Ngày nhận hàng không được ở quá khứ.");

        // --- Ưu tiên 4: Chỉ lấy các trường cần thiết của sản phẩm (tên, trạng thái, chi nhánh mặc định) ---
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại"));
        if (Boolean.TRUE.equals(product.getIsDeleted()) || !Boolean.TRUE.equals(product.getIsActive())
                || !Boolean.TRUE.equals(product.getIsSelling()))
            throw new IllegalStateException("Sản phẩm đã ngừng bán.");

        if (request.getOrderType() != com.finalproject.enums.OrderType.BUY) throw new IllegalArgumentException("Hệ thống chỉ hỗ trợ mua hoa.");
        if (!com.finalproject.service.ProductAvailability.isPublic(product)) throw new IllegalStateException("Sản phẩm đã ngừng bán.");
        if (request.getQuantity() == null || request.getQuantity() <= 0 || request.getQuantity() > 10000) throw new IllegalArgumentException("Số lượng không hợp lệ.");
        if (request.getDeliveryDate() != null && request.getDeliveryDate().isBefore(java.time.LocalDate.now())) throw new IllegalArgumentException("Ngày nhận hàng không được ở quá khứ.");
        Long storeId = request.getStoreId() != null ? request.getStoreId() : (product.getStore() != null ? product.getStore().getId()
                : storeInventoryRepository.findByProductId(product.getId()).stream()
                    .filter(s -> Boolean.TRUE.equals(s.getStore().getStatus()) && Boolean.TRUE.equals(s.getStore().getIsOpen())
                            && s.isAvailableForSale()
                            && s.getQuantity() != null && s.getQuantity() >= addQty)
                    .sorted(java.util.Comparator.comparing(s -> s.getStore().getId())).map(s -> s.getStore().getId()).findFirst()
                    .orElseThrow(() -> new IllegalStateException("Không có chi nhánh đang mở đủ tồn kho cho sản phẩm này.")));
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Chi nhánh không tồn tại"));

        if (!Boolean.TRUE.equals(store.getStatus()) || !Boolean.TRUE.equals(store.getIsOpen())) throw new IllegalStateException("Chi nhánh hiện không nhận đơn.");
        if (!com.finalproject.service.ProductAvailability.supportsBranch(product, store))
            throw new IllegalStateException("Mẫu hoa này chỉ bán tại chi nhánh " + product.getStore().getName() + ". Vui lòng chọn đúng chi nhánh.");
        // --- Ưu tiên 3: Lấy tồn kho một lần ---
        StoreInventory inventory = storeInventoryRepository.findByStoreIdAndProductId(store.getId(), product.getId())
                .orElse(null);
        int availableStock = (inventory != null && inventory.isAvailableForSale() && inventory.getQuantity() != null) ? inventory.getQuantity() : 0;
        if (availableStock <= 0) {
            throw new IllegalStateException("Mẫu hoa '" + product.getName()
                    + "' hiện đang tạm hết hàng tại " + store.getName()
                    + ". Quý khách vui lòng chọn chi nhánh khác hoặc mẫu hoa khác.");
        }

        java.time.LocalDate delivDate = request.getDeliveryDate() != null
                ? request.getDeliveryDate() : java.time.LocalDate.now();

        // --- Ưu tiên 5: Lấy khóa giỏ ngay trước khi ghi, giảm thời gian giữ khóa ---
        Cart cart = getCartForUpdate(username);

        java.util.List<CartItem> matchingLines = cartItemRepository.findMatchingLines(
                cart.getId(), product.getId(), storeId,
                com.finalproject.enums.OrderType.BUY);

        long alreadyInCart = cartItemRepository.sumQtyByCartProductStore(cart.getId(), product.getId(), storeId);
        if (alreadyInCart + addQty > availableStock) {
            throw new IllegalArgumentException("Số lượng vượt quá tồn kho; chỉ còn " + availableStock + " sản phẩm tại chi nhánh này.");
        }
        if (!matchingLines.isEmpty()) {
            CartItem existingItem = matchingLines.getFirst();
            long newTotal = addQty;
            for (CartItem line : matchingLines) newTotal += line.getQuantity();
            existingItem.setQuantity(Math.toIntExact(newTotal));
            existingItem.setDeliveryDate(delivDate);
            String greetingMsg = request.getGreetingMessage();
            if (greetingMsg == null || greetingMsg.isBlank()) {
                for (CartItem line : matchingLines) {
                    if (line.getGreetingMessage() != null && !line.getGreetingMessage().isBlank()) {
                        greetingMsg = line.getGreetingMessage();
                    }
                }
            }
            if (greetingMsg != null && !greetingMsg.isBlank()) {
                existingItem.setGreetingMessage(greetingMsg);
            }
            cartItemRepository.save(existingItem);
            // Consolidate legacy date-based rows under the same cart lock.
            if (matchingLines.size() > 1) {
                var duplicates = matchingLines.subList(1, matchingLines.size());
                if (cart.getItems() != null) {
                    cart.getItems().removeIf(line -> duplicates.stream().anyMatch(duplicate -> duplicate.getId().equals(line.getId())));
                }
                cartItemRepository.deleteAll(duplicates);
            }
        } else {
            // --- Ưu tiên 3: Kiểm tra tồn kho tổng hợp: SL đã có + SL mới thêm ---
            CartItem cartItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .store(store)
                    .orderType(com.finalproject.enums.OrderType.BUY)
                    .quantity(addQty)
                    .deliveryDate(delivDate)
                    .greetingMessage(request.getGreetingMessage())
                    .build();
            cartItemRepository.save(cartItem);
        }

        // --- Ưu tiên 2: Trả tổng số lượng mới bằng 1 query tổng hợp, không đọc toàn bộ giỏ ---
        return Math.toIntExact(cartItemRepository.sumQuantityByCartId(cart.getId()));
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
        if (!com.finalproject.service.ProductAvailability.supportsBranch(product, store))
            throw new IllegalStateException("Chi nhánh trong giỏ không bán mẫu hoa này. Vui lòng xóa mục cũ và thêm lại từ trang sản phẩm.");
        int available = storeInventoryRepository.findByStoreIdAndProductId(store.getId(), product.getId())
                .filter(StoreInventory::isAvailableForSale).map(stock -> Math.max(0, stock.getQuantity())).orElse(0);
        long requested = quantity;
        for (CartItem line : cart.getItems()) {
            if (!java.util.Objects.equals(line.getId(), excludedItemId)
                    && line.getProduct().getId().equals(product.getId()) && line.getStore().getId().equals(store.getId())) {
                requested += line.getQuantity();
            }
        }
        if (requested > available) throw new IllegalArgumentException("Số lượng vượt quá tồn kho; chỉ còn " + available
                + " sản phẩm tại chi nhánh này");
    }

    private Cart getCartForUpdate(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        if (!Boolean.TRUE.equals(user.getIsActive()) || user.isRetiredDeliveryAccount()) {
            throw new org.springframework.security.access.AccessDeniedException("Tài khoản đã bị khóa hoặc vô hiệu hóa.");
        }
        if (isAdminUser(user)) {
            throw new org.springframework.security.access.AccessDeniedException("Tài khoản Quản trị viên không thể thực hiện thao tác mua hàng hoặc sử dụng giỏ hàng.");
        }
        Cart cart = cartRepository.findLockedByUserId(user.getId())
                .orElseGet(() -> cartRepository.save(Cart.builder().user(user).items(new ArrayList<>()).build()));
        return cart;
    }

    private boolean isAdminUser(User user) {
        if (user == null) return false;
        return user.getRole() == com.finalproject.enums.Role.ADMIN
                || (user.getRoles() != null && user.getRoles().contains(com.finalproject.enums.Role.ADMIN));
    }
}
