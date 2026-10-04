package com.finalproject.service.impl;

import com.finalproject.entity.*;
import com.finalproject.enums.*;
import com.finalproject.exception.ResourceNotFoundException;
import com.finalproject.repository.*;
import com.finalproject.service.CartService;
import com.finalproject.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final CartService cartService;
    private final com.finalproject.service.OrderWorkflowService workflow;
    private final com.finalproject.service.StoreAccessService access;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final StoreInventoryRepository storeInventoryRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final DeliveryRepository deliveryRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final com.finalproject.service.VoucherService voucherService;
    private final TransactionRepository transactionRepository;
    private final com.finalproject.service.NotificationService notificationService;
    private final jakarta.persistence.EntityManager entityManager;

    @Override
    @Transactional
    public List<Order> checkout(String username, List<Long> selectedCartItemIds, PaymentMethod paymentMethod) {
        return checkout(username, selectedCartItemIds, paymentMethod, null, null, null, null, null);
    }

    @Override
    @Transactional
    public List<Order> checkout(String username, List<Long> selectedCartItemIds, PaymentMethod paymentMethod,
                                 String recipientName, String recipientPhone, String recipientAddress, String greetingMessage) {
        return checkout(username, selectedCartItemIds, paymentMethod, recipientName, recipientPhone, recipientAddress, greetingMessage, null);
    }

    @Override
    @Transactional
    public List<Order> checkout(String username, List<Long> selectedCartItemIds, PaymentMethod paymentMethod,
                                 String recipientName, String recipientPhone, String recipientAddress, String greetingMessage, String voucherCode) {
        return checkout(username, selectedCartItemIds, paymentMethod, recipientName, recipientPhone, recipientAddress, greetingMessage, voucherCode, null);
    }

    @Override
    @Transactional
    public List<Order> checkout(String username, List<Long> selectedCartItemIds, PaymentMethod paymentMethod,
                                 String recipientName, String recipientPhone, String recipientAddress, String greetingMessage, String voucherCode,
                                 java.time.LocalDate deliveryDate) {
        return checkout(username, selectedCartItemIds, paymentMethod, recipientName, recipientPhone, recipientAddress, greetingMessage, voucherCode, deliveryDate, "DELIVERY", null);
    }

    @Override
    @Transactional
    public List<Order> checkout(String username, List<Long> selectedCartItemIds, PaymentMethod paymentMethod,
                                 String recipientName, String recipientPhone, String recipientAddress, String greetingMessage, String voucherCode,
                                 java.time.LocalDate deliveryDate, String shippingMethod, Long pickupStoreId) {
        // Preserve the existing service API for callers that do not select a delivery unit.
        return checkout(username, selectedCartItemIds, paymentMethod, recipientName, recipientPhone, recipientAddress,
                greetingMessage, voucherCode, deliveryDate, shippingMethod, pickupStoreId, null);
    }

    @Override
    @Transactional
    public List<Order> checkout(String username, List<Long> selectedCartItemIds, PaymentMethod paymentMethod,
                               String recipientName, String recipientPhone, String recipientAddress, String greetingMessage, String voucherCode,
                               LocalDate deliveryDate, String shippingMethod, Long pickupStoreId, Long deliveryUnitId) {
        if (paymentMethod != PaymentMethod.COD && paymentMethod != PaymentMethod.BANK_TRANSFER && paymentMethod != PaymentMethod.MOMO) {
            throw new IllegalArgumentException("Phương thức thanh toán chưa được hỗ trợ.");
        }
        if (!"PICKUP".equals(shippingMethod) && !"DELIVERY".equals(shippingMethod)) {
            throw new IllegalArgumentException("Hình thức nhận hàng không hợp lệ.");
        }
        User customer = userRepository.findByUsername(username).orElseThrow();
        requireActiveCustomer(customer);
        Cart cart = cartRepository.findLockedByUserId(customer.getId())
                .orElseThrow(() -> new IllegalStateException("Giỏ hàng đang trống."));
        List<CartItem> allItems = cartItemRepository.findByCartId(cart.getId());

        if (allItems == null || allItems.isEmpty()) {
            throw new IllegalStateException("Giỏ hàng của bạn đang trống!");
        }

        List<CartItem> items;
        if (selectedCartItemIds != null && !selectedCartItemIds.isEmpty()) {
            Set<Long> ownedIds = allItems.stream().map(CartItem::getId).collect(Collectors.toSet());
            if (!ownedIds.containsAll(selectedCartItemIds)) throw new IllegalArgumentException("Sản phẩm được chọn không thuộc giỏ hàng của bạn.");
            items = allItems.stream()
                    .filter(i -> selectedCartItemIds.contains(i.getId()))
                    .collect(Collectors.toList());
            if (items.isEmpty()) {
                throw new IllegalStateException("Vui lòng chọn ít nhất 1 sản phẩm để thanh toán!");
            }
        } else {
            items = new ArrayList<>(allItems);
        }

        User user = cart.getUser();
        List<Order> createdOrders = new ArrayList<>();

        // Xác định chi nhánh xử lý đơn hàng:
        // 1. Nếu khách chọn ĐẾN LẤY (PICKUP): Gán chi nhánh khách tự chọn
        // 2. Nếu khách chọn GIAO HÀNG (DELIVERY): Hệ thống tự tìm chi nhánh gần địa chỉ của khách nhất
        Store assignedStore;
        String finalRecipientAddress;
        boolean isPickup = "PICKUP".equalsIgnoreCase(shippingMethod);

        if (isPickup) {
            if (pickupStoreId == null) throw new IllegalArgumentException("Vui lòng chọn chi nhánh nhận hàng.");
            assignedStore = storeRepository.findById(pickupStoreId)
                    .orElseThrow(() -> new IllegalArgumentException("Chi nhánh không tồn tại."));
            finalRecipientAddress = "Nhận tại chi nhánh: " + assignedStore.getName() + " - " + assignedStore.getAddress();
        } else {
            finalRecipientAddress = (recipientAddress != null && !recipientAddress.trim().isEmpty()) ? recipientAddress.trim() : user.getAddress();
            assignedStore = findDeliveryStore(finalRecipientAddress, items);
        }

        if (!Boolean.TRUE.equals(assignedStore.getStatus()) || !Boolean.TRUE.equals(assignedStore.getIsOpen())) {
            throw new IllegalStateException("Chi nhánh hiện không nhận đơn.");
        }
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalImportPrice = BigDecimal.ZERO;
        List<OrderDetail> orderDetails = new ArrayList<>();

        CartItem firstItem = items.get(0);
        java.time.LocalDate finalDelivDate = deliveryDate != null ? deliveryDate : (firstItem.getDeliveryDate() != null ? firstItem.getDeliveryDate() : java.time.LocalDate.now());
        String finalRecipientName = (recipientName != null && !recipientName.trim().isEmpty()) ? recipientName.trim() : user.getFullName();
        String finalRecipientPhone = (recipientPhone != null && !recipientPhone.trim().isEmpty()) ? recipientPhone.trim() : user.getPhone();
        String finalGreeting = (greetingMessage != null && !greetingMessage.trim().isEmpty()) ? greetingMessage.trim() : null;

        if (finalDelivDate.isBefore(LocalDate.now())) throw new IllegalArgumentException("Ngày nhận hàng không được ở quá khứ.");
        if (finalRecipientName == null || finalRecipientName.isBlank() || finalRecipientPhone == null || finalRecipientPhone.isBlank()
                || finalRecipientAddress == null || finalRecipientAddress.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ tên, số điện thoại và địa chỉ nhận hàng.");
        }
        if (finalRecipientName.length() > 100 || finalRecipientAddress.length() > 255 || (finalGreeting != null && finalGreeting.length() > 500))
            throw new IllegalArgumentException("Tên tối đa 100, địa chỉ 255 và lời chúc 500 ký tự.");
        if (!finalRecipientPhone.matches("^(0|\\+?84)[35789][0-9]{8}$")) throw new IllegalArgumentException("Số điện thoại người nhận không hợp lệ.");
        Order order = Order.builder()
                .shippingMethod(shippingMethod)
                .user(user)
                .pickupStore(assignedStore)
                .orderType(OrderType.BUY)
                .status(OrderStatus.PENDING)
                .deliveryDate(finalDelivDate)
                .greetingMessage(finalGreeting)
                .recipientName(finalRecipientName)
                .recipientPhone(finalRecipientPhone)
                .recipientAddress(finalRecipientAddress)
                .createdAt(LocalDateTime.now())
                .build();

        // Lock each stock row once, in the same product order for all checkouts.
        // Delivery selection may already have loaded stale stock into this persistence context.
        Map<Long, StoreInventory> lockedStock = new HashMap<>();
        for (Long productId : items.stream().map(i -> i.getProduct().getId()).distinct().sorted().toList()) {
            StoreInventory stock = storeInventoryRepository.findLockedStock(assignedStore.getId(), productId)
                    .orElseThrow(() -> new IllegalStateException("Chi nhánh không có hàng."));
            entityManager.refresh(stock, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            lockedStock.put(productId, stock);
        }
        for (CartItem item : items) {
            Product product = item.getProduct();

            if (item.getOrderType() != OrderType.BUY || item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Giỏ hàng chỉ hỗ trợ mua hoa với số lượng dương.");
            }
            if (Boolean.TRUE.equals(product.getIsDeleted()) || !Boolean.TRUE.equals(product.getIsActive()) || !Boolean.TRUE.equals(product.getIsSelling())) {
                throw new IllegalStateException("Sản phẩm đã ngừng bán: " + product.getName());
            }
            StoreInventory inventory = lockedStock.get(product.getId());
            if (inventory.getQuantity() < item.getQuantity()) {
                throw new IllegalStateException("Không đủ tồn kho: " + product.getName());
            }
            inventory.setQuantity(inventory.getQuantity() - item.getQuantity());
            storeInventoryRepository.save(inventory);
            BigDecimal unitPrice = product.getSellingPrice();
            if (unitPrice == null || unitPrice.signum() <= 0 || unitPrice.stripTrailingZeros().scale() > 2)
                throw new IllegalStateException("Giá sản phẩm không hợp lệ: " + product.getName());
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);

            BigDecimal itemImportPrice = product.getImportPrice();
            if (itemImportPrice == null) {
                itemImportPrice = (product.getPriceBuy() != null) ? product.getPriceBuy().multiply(BigDecimal.valueOf(0.6)) : BigDecimal.ZERO;
            }
            totalImportPrice = totalImportPrice.add(itemImportPrice.multiply(BigDecimal.valueOf(item.getQuantity())));

            OrderDetail detail = OrderDetail.builder()
                    .order(order)
                    .product(product)
                    .quantity(item.getQuantity())
                    .unitPrice(unitPrice)
                    .orderType(OrderType.BUY)
                    .depositFee(BigDecimal.ZERO)
                    .build();
            orderDetails.add(detail);
        }

        // Áp dụng voucher nếu có (Đảm bảo giá sau giảm không bao giờ thấp hơn tổng giá nhập)
        com.finalproject.service.CheckoutQuote amounts = calculateAmounts(totalAmount, totalImportPrice,
                assignedStore, voucherCode, shippingMethod, deliveryUnitId);
        BigDecimal discount = amounts.discount();
        if (voucherCode != null && !voucherCode.trim().isEmpty()) {
            Voucher v = voucherService.findByCode(voucherCode.trim());
            if (v == null) throw new IllegalArgumentException("Mã giảm giá không hợp lệ.");
            if (v.getStore() != null && !v.getStore().getId().equals(assignedStore.getId())) {
                throw new IllegalArgumentException("Mã giảm giá không áp dụng cho chi nhánh này.");
            }
            order.setVoucher(v);
            order.setDiscountAmount(discount);
            v.setUsedCount(v.getUsedCount() + 1);
            voucherService.createVoucher(v);
        }
        BigDecimal finalAmount = amounts.total();
        order.setShippingFee(amounts.shippingFee());
        order.setDeliveryUnitId(isPickup ? null : deliveryUnitId);
        order.setTotalAmount(finalAmount);
        order.setTotalDeposit(BigDecimal.ZERO);
        order.setPaymentMethod(paymentMethod != null ? paymentMethod.name() : "COD");
        order.setPaymentStatus("UNPAID");
        order.setOrderDetails(orderDetails);
        Order savedOrder = orderRepository.save(order);

        // 3. Tạo bản ghi thanh toán (Payment)
        BigDecimal paymentTotal = finalAmount;
        Payment payment = Payment.builder()
                .order(savedOrder)
                .method(paymentMethod)
                .amount(paymentTotal)
                .status(PaymentStatus.PENDING)
                .paidAt(null)
                .build();
        paymentRepository.save(payment);

        createdOrders.add(savedOrder);

        // Gửi thông báo đến tài khoản khách hàng và chủ chi nhánh
        try {
            notificationService.sendNotification(user,
                    "🌸 Đặt hoa thành công!",
                    "Đơn hoa #" + savedOrder.getId() + " trị giá " + String.format("%,d", finalAmount.longValue()) + " đ đã được tạo thành công. Tiệm hoa đang tiến hành chuẩn bị.",
                    "ORDER_CREATED", savedOrder.getId());
            notificationService.sendNotificationToStoreManagers(assignedStore.getId(),
                    "🔔 Đơn hoa mới #" + savedOrder.getId(),
                    "Có đơn hoa mới từ khách " + finalRecipientName + " (" + finalRecipientPhone + "). Tổng: " + String.format("%,d", finalAmount.longValue()) + " đ.",
                    "ORDER_NEW", savedOrder.getId());
        } catch (Exception ex) {
            org.slf4j.LoggerFactory.getLogger(OrderServiceImpl.class).warn("Không gửi được notification: {}", ex.getMessage());
        }

        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            messagingTemplate.convertAndSend("/topic/store/" + assignedStore.getId() + "/orders",
                                    "Có đơn hoa mới #" + savedOrder.getId());
                        } catch (RuntimeException ex) {
                            org.slf4j.LoggerFactory.getLogger(OrderServiceImpl.class).warn("Không gửi được thông báo đơn #{}", savedOrder.getId());
                        }
                    }
                });
        // Xóa các sản phẩm đã thanh toán khỏi giỏ hàng
        for (CartItem processedItem : items) {
            cartService.removeCartItem(username, processedItem.getId());
        }
        return createdOrders;
    }

    @Override
    @Transactional(readOnly = true)
    public com.finalproject.service.CheckoutQuote quote(String username, List<Long> selectedCartItemIds,
                                                       String recipientAddress, String voucherCode,
                                                       String shippingMethod, Long pickupStoreId, Long deliveryUnitId) {
        if (!"PICKUP".equals(shippingMethod) && !"DELIVERY".equals(shippingMethod)) {
            throw new IllegalArgumentException("Hình thức nhận hàng không hợp lệ.");
        }
        User user = userRepository.findByUsername(username).orElseThrow();
        requireActiveCustomer(user);
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("Giỏ hàng đang trống."));
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId()).stream()
                .filter(i -> selectedCartItemIds == null || selectedCartItemIds.isEmpty() || selectedCartItemIds.contains(i.getId()))
                .toList();
        if (selectedCartItemIds != null && !selectedCartItemIds.isEmpty()
                && !items.stream().map(CartItem::getId).collect(Collectors.toSet()).containsAll(selectedCartItemIds)) {
            throw new IllegalArgumentException("Sản phẩm được chọn không thuộc giỏ hàng của bạn.");
        }
        if (items.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn sản phẩm để thanh toán.");
        Store store;
        if ("PICKUP".equals(shippingMethod)) {
            if (pickupStoreId == null) throw new IllegalArgumentException("Vui lòng chọn chi nhánh nhận hàng.");
            store = storeRepository.findById(pickupStoreId).orElseThrow(() -> new IllegalArgumentException("Chi nhánh không tồn tại."));
        } else {
            store = findDeliveryStore(recipientAddress != null && !recipientAddress.isBlank() ? recipientAddress : user.getAddress(), items);
        }
        if (!Boolean.TRUE.equals(store.getStatus()) || !Boolean.TRUE.equals(store.getIsOpen())) {
            throw new IllegalStateException("Chi nhánh hiện không nhận đơn.");
        }
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal importTotal = BigDecimal.ZERO;
        for (CartItem item : items) {
            Product p = item.getProduct();
            if (item.getOrderType() != OrderType.BUY || item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Giỏ hàng chỉ hỗ trợ mua hoa với số lượng dương.");
            }
            if (Boolean.TRUE.equals(p.getIsDeleted()) || !Boolean.TRUE.equals(p.getIsActive()) || !Boolean.TRUE.equals(p.getIsSelling())) {
                throw new IllegalStateException("Sản phẩm đã ngừng bán: " + p.getName());
            }
            StoreInventory inventory = storeInventoryRepository.findByStoreIdAndProductId(store.getId(), p.getId())
                    .orElseThrow(() -> new IllegalStateException("Chi nhánh không có hàng: " + p.getName()));
            if (inventory.getQuantity() < item.getQuantity()) throw new IllegalStateException("Không đủ tồn kho: " + p.getName());
            BigDecimal price = p.getSellingPrice();
            if (price == null || price.signum() <= 0 || price.stripTrailingZeros().scale() > 2)
                throw new IllegalStateException("Giá sản phẩm không hợp lệ: " + p.getName());
            subtotal = subtotal.add(price.multiply(BigDecimal.valueOf(item.getQuantity())));
            BigDecimal cost = p.getImportPrice() != null ? p.getImportPrice()
                    : p.getPriceBuy() != null ? p.getPriceBuy().multiply(BigDecimal.valueOf(0.6)) : BigDecimal.ZERO;
            importTotal = importTotal.add(cost.multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        return calculateAmounts(subtotal, importTotal, store, voucherCode, shippingMethod, deliveryUnitId);
    }

    private com.finalproject.service.CheckoutQuote calculateAmounts(BigDecimal subtotal, BigDecimal importTotal,
                                                                   Store store, String voucherCode,
                                                                   String shippingMethod, Long deliveryUnitId) {
        BigDecimal shippingFee = BigDecimal.ZERO;
        if ("DELIVERY".equals(shippingMethod) && deliveryUnitId != null) {
            Delivery delivery = deliveryRepository.findById(deliveryUnitId)
                    .orElseThrow(() -> new IllegalArgumentException("Đơn vị giao hàng không tồn tại."));
            if (!Boolean.TRUE.equals(delivery.getIsActive()) || Boolean.TRUE.equals(delivery.getIsDeleted())
                    || delivery.getPrice() == null || delivery.getPrice().signum() < 0) {
                throw new IllegalArgumentException("Đơn vị giao hàng hiện không khả dụng.");
            }
            shippingFee = delivery.getPrice();
        }
        BigDecimal discount = BigDecimal.ZERO;
        if (voucherCode != null && !voucherCode.isBlank()) {
            Voucher voucher = voucherService.findByCode(voucherCode.trim());
            if (voucher == null) throw new IllegalArgumentException("Mã giảm giá không hợp lệ.");
            if (voucher.getStore() != null && !voucher.getStore().getId().equals(store.getId())) {
                throw new IllegalArgumentException("Mã giảm giá không áp dụng cho chi nhánh này.");
            }
            discount = voucherService.calculateDiscount(voucher, subtotal, shippingFee, importTotal);
        }
        BigDecimal total = subtotal.add(shippingFee).subtract(discount).max(importTotal).max(BigDecimal.ZERO);
        return new com.finalproject.service.CheckoutQuote(subtotal, shippingFee, discount, total, store.getId());
    }

    private Store findDeliveryStore(String address, List<CartItem> items) {
        Store nearest = findNearestStore(address);
        java.util.Map<Long, Long> demand = new java.util.LinkedHashMap<>();
        for (CartItem item : items) {
            if (item.getQuantity() == null || item.getQuantity() <= 0 || item.getOrderType() != OrderType.BUY) {
                throw new IllegalArgumentException("Giỏ hàng chỉ hỗ trợ mua hoa với số lượng dương.");
            }
            demand.merge(item.getProduct().getId(), item.getQuantity().longValue(), Long::sum);
        }
        java.util.Map<Long, Store> candidates = new java.util.LinkedHashMap<>();
        candidates.put(nearest.getId(), nearest);
        for (CartItem item : items) {
            if (item.getStore() != null) candidates.putIfAbsent(item.getStore().getId(), item.getStore());
        }
        for (Store candidate : storeRepository.findByStatusTrue()) candidates.putIfAbsent(candidate.getId(), candidate);
        boolean closedStoreHasStock = false;
        for (Store candidate : candidates.values()) {
            boolean enough = demand.entrySet().stream().allMatch(entry ->
                    storeInventoryRepository.findByStoreIdAndProductId(candidate.getId(), entry.getKey())
                            .map(stock -> stock.getQuantity() != null && stock.getQuantity() >= entry.getValue()).orElse(false));
            if (!Boolean.TRUE.equals(candidate.getStatus()) || !Boolean.TRUE.equals(candidate.getIsOpen())) {
                closedStoreHasStock |= enough;
                continue;
            }
            if (enough) return candidate;
        }
        if (closedStoreHasStock) throw new IllegalStateException("Chi nhánh có đủ hàng hiện không nhận đơn. Vui lòng thử lại khi chi nhánh mở cửa.");
        throw new IllegalStateException("Các chi nhánh đang mở không có hàng hoặc không đủ tồn kho cho mẫu hoa đã chọn. Vui lòng giảm số lượng hoặc chọn mẫu khác.");
    }

    private void requireActiveCustomer(User user) {
        if (!Boolean.TRUE.equals(user.getIsActive()) || user.isRetiredDeliveryAccount()) {
            throw new org.springframework.security.access.AccessDeniedException("Tài khoản đã bị khóa hoặc vô hiệu hóa.");
        }
    }

    private Store findNearestStore(String address) {
        List<Store> stores = storeRepository.findByStatusTrue().stream().filter(s -> Boolean.TRUE.equals(s.getIsOpen())).toList();

        if (stores.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy chi nhánh nào trong hệ thống");
        }
        if (address == null || address.trim().isEmpty()) {
            return stores.get(0);
        }

        String norm = address.toLowerCase();

        // 1. Phân loại theo khu vực / quận
        // TP. Thủ Đức (Quận 2, Quận 9, Thủ Đức, Dĩ An, Bình Dương...)
        if (norm.contains("thủ đức") || norm.contains("thu duc") || norm.contains("quận 9") || norm.contains("q9") || norm.contains("q.9")
                || norm.contains("quận 2") || norm.contains("q2") || norm.contains("q.2") || norm.contains("dĩ an") || norm.contains("di an")
                || norm.contains("bình dương") || norm.contains("binh duong") || norm.contains("linh chiểu") || norm.contains("linh trung")) {
            for (Store s : stores) {
                if (s.getName().toLowerCase().contains("thủ đức") || s.getAddress().toLowerCase().contains("thủ đức")) {
                    return s;
                }
            }
        }

        // Quận 7 (Quận 7, Nhà Bè, Quận 8, Bình Chánh, Cần Giờ...)
        if (norm.contains("quận 7") || norm.contains("q7") || norm.contains("q.7") || norm.contains("nhà bè") || norm.contains("nha be")
                || norm.contains("quận 8") || norm.contains("q8") || norm.contains("q.8") || norm.contains("bình chánh") || norm.contains("binh chanh")
                || norm.contains("phú mỹ hưng") || norm.contains("phu my hung") || norm.contains("tân phú") || norm.contains("tân phong")) {
            for (Store s : stores) {
                if (s.getName().toLowerCase().contains("quận 7") || s.getAddress().toLowerCase().contains("quận 7")) {
                    return s;
                }
            }
        }

        // Quận 1 (Trung tâm: Quận 1, Quận 3, Quận 4, Quận 5, Quận 10, Quận 11, Bình Thạnh, Phú Nhuận, Tân Bình, Gò Vấp...)
        for (Store s : stores) {
            if (s.getName().toLowerCase().contains("quận 1") || s.getAddress().toLowerCase().contains("quận 1") || s.getName().toLowerCase().contains("bến thành")) {
                return s;
            }
        }

        return stores.get(0);
    }

    @Override
    public List<Order> getOrdersByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    @Override
    public List<Order> getOrdersByUsernameAndStatus(String username, OrderStatus status) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));
        if (status == null) {
            return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        }
        return orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), status);
    }

    @Override
    public List<Order> getOrdersByStore(Long storeId) {
        return orderRepository.findByPickupStoreIdOrderByCreatedAtDesc(storeId);
    }

    @Override
    public List<Order> getAllOrders() {
        return orderRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
    }

    @Override
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng ID: " + orderId));
    }

    @Override
    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus status) {
        workflow.transition(orderId, status);
    }

    @Override
    @Transactional
    public void requestRefund(String username, Long orderId) {
        workflow.requestRefund(username, orderId);
    }

    @Override
    @Transactional
    public void cancelOrder(String username, Long orderId) {
        Order order = getOrderById(orderId);
        // UC_011: Đơn hàng chỉ có thể hủy trong vòng 1 tiếng sau khi đặt
        if (order.getCreatedAt() != null && java.time.Duration.between(order.getCreatedAt(), java.time.LocalDateTime.now()).toHours() >= 1) {
            throw new IllegalStateException("Đơn hàng đã được đặt quá 1 tiếng, không thể hủy!");
        }
        workflow.cancelByCustomer(username, orderId);
    }

    @Override
    @Transactional
    public void scanAndMarkOverdueOrders() {
        LocalDate today = LocalDate.now();
        List<Order> overdueOrders = orderRepository.findOverdueOrders(OrderStatus.RENTING, today);

        for (Order order : overdueOrders) {
            order.setStatus(OrderStatus.OVERDUE);
            orderRepository.save(order);

            // Bắn WebSocket thông báo đến nhân viên chi nhánh
            messagingTemplate.convertAndSend("/topic/store/" + order.getPickupStore().getId() + "/orders",
                    "Đơn thuê #" + order.getId() + " của khách " + order.getUser().getFullName() + " đã QUÁ HẠN!");
        }
    }
}
