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
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final CartService cartService;
    private final com.finalproject.service.OrderWorkflowService workflow;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final StoreInventoryRepository storeInventoryRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final DeliveryRepository deliveryRepository;
    private final com.finalproject.repository.ShippingTariffRepository shippingTariffRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final com.finalproject.service.VoucherService voucherService;
    private final com.finalproject.service.NotificationService notificationService;
    private final jakarta.persistence.EntityManager entityManager;
    private final java.time.Clock clock;

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
        customer = userRepository.findLockedById(customer.getId()).orElseThrow();
        entityManager.refresh(customer, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
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
        requireFulfillmentBranch(items, assignedStore);
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalImportPrice = BigDecimal.ZERO;
        List<OrderDetail> orderDetails = new ArrayList<>();

        CartItem firstItem = items.get(0);
        java.time.LocalDate finalDelivDate = deliveryDate != null ? deliveryDate : (firstItem.getDeliveryDate() != null ? firstItem.getDeliveryDate() : java.time.LocalDate.now());
        String rawRecipientName = (recipientName != null && !recipientName.trim().isEmpty()) ? recipientName.trim() : user.getFullName();
        String rawRecipientPhone = (recipientPhone != null && !recipientPhone.trim().isEmpty()) ? recipientPhone.trim() : user.getPhone();
        String finalGreeting = (greetingMessage != null && !greetingMessage.trim().isEmpty()) ? greetingMessage.trim() : null;

        String finalRecipientName = rawRecipientName == null ? null : rawRecipientName.trim();
        String finalRecipientPhone = cleanPhone(rawRecipientPhone);

        if (finalDelivDate.isBefore(LocalDate.now())) throw new IllegalArgumentException("Ngày nhận hàng không được ở quá khứ.");
        if (finalRecipientName == null || finalRecipientName.isBlank() || finalRecipientPhone == null || finalRecipientPhone.isBlank()
                || finalRecipientAddress == null || finalRecipientAddress.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ tên, số điện thoại và địa chỉ nhận hàng.");
        }
        if (finalRecipientName.length() > 100 || finalRecipientAddress.length() > 255 || (finalGreeting != null && finalGreeting.length() > 500))
            throw new IllegalArgumentException("Tên tối đa 100, địa chỉ 255 và lời chúc 500 ký tự.");
        if (!isValidPhoneNumber(finalRecipientPhone)) throw new IllegalArgumentException("Số điện thoại người nhận không hợp lệ.");
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
                .createdAt(LocalDateTime.now(clock))
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
            if (!com.finalproject.service.ProductAvailability.isPublic(product)) {
                throw new IllegalStateException("Sản phẩm đã ngừng bán: " + product.getName());
            }
            StoreInventory inventory = lockedStock.get(product.getId());
            if (!inventory.isAvailableForSale()) throw new IllegalStateException("Chi nhánh đã ngừng bán hoặc xóa mẫu hoa này.");
            if (inventory.getQuantity() < item.getQuantity()) {
                throw new IllegalStateException("Không đủ tồn kho: " + product.getName());
            }
            inventory.setQuantity(inventory.getQuantity() - item.getQuantity());
            storeInventoryRepository.save(inventory);
            BigDecimal unitPrice = product.getSellingPrice();
            if (!com.finalproject.utils.ProductPrices.valid(unitPrice))
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
                assignedStore, voucherCode, shippingMethod, deliveryUnitId, finalRecipientAddress, items);
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
        if (paymentMethod == PaymentMethod.MOMO) order.setPaymentExpiresAt(LocalDateTime.now(clock).plusMinutes(30));
        if (paymentMethod == PaymentMethod.BANK_TRANSFER) order.setPaymentExpiresAt(LocalDateTime.now(clock).plusHours(24));
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
        requireFulfillmentBranch(items, store);
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal importTotal = BigDecimal.ZERO;
        Map<Long, Long> required = new HashMap<>();
        for (CartItem line : items) {
            if (line.getQuantity() == null || line.getQuantity() <= 0) throw new IllegalArgumentException("Số lượng phải lớn hơn 0.");
            required.merge(line.getProduct().getId(), line.getQuantity().longValue(), Long::sum);
        }
        for (CartItem item : items) {
            Product p = item.getProduct();
            if (item.getOrderType() != OrderType.BUY || item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Giỏ hàng chỉ hỗ trợ mua hoa với số lượng dương.");
            }
            if (!com.finalproject.service.ProductAvailability.isPublic(p)) {
                throw new IllegalStateException("Sản phẩm đã ngừng bán: " + p.getName());
            }
            StoreInventory inventory = storeInventoryRepository.findByStoreIdAndProductId(store.getId(), p.getId())
                    .orElseThrow(() -> new IllegalStateException("Chi nhánh không có hàng: " + p.getName()));
            if (!inventory.isAvailableForSale()) throw new IllegalStateException("Chi nhánh đã ngừng bán hoặc xóa mẫu hoa này.");
            if (inventory.getQuantity() == null || inventory.getQuantity() < required.get(p.getId())) throw new IllegalStateException("Không đủ tổng tồn kho: " + p.getName());
            BigDecimal price = p.getSellingPrice();
            if (!com.finalproject.utils.ProductPrices.valid(price))
                throw new IllegalStateException("Giá sản phẩm không hợp lệ: " + p.getName());
            subtotal = subtotal.add(price.multiply(BigDecimal.valueOf(item.getQuantity())));
            BigDecimal cost = p.getImportPrice() != null ? p.getImportPrice()
                    : p.getPriceBuy() != null ? p.getPriceBuy().multiply(BigDecimal.valueOf(0.6)) : BigDecimal.ZERO;
            importTotal = importTotal.add(cost.multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        String effectiveAddress = (recipientAddress != null && !recipientAddress.isBlank()) ? recipientAddress : user.getAddress();
        return calculateAmounts(subtotal, importTotal, store, voucherCode, shippingMethod, deliveryUnitId, effectiveAddress, items);
    }

    @Override @Transactional
    public List<Order> checkout(String username, List<Long> ids, PaymentMethod method, String name, String phone,
            String address, String greeting, String voucher, LocalDate date, String shipping, Long storeId, Long deliveryId,
            java.time.LocalTime time) {
        if (date == null || time == null) throw new IllegalArgumentException("Vui lòng chọn ngày và giờ nhận hoa.");
        if (java.time.LocalDateTime.of(date, time).isBefore(java.time.LocalDateTime.now(clock)))
            throw new IllegalArgumentException("Ngày giờ nhận hoa không được ở quá khứ.");
        var created = checkout(username, ids, method, name, phone, address, greeting, voucher, date, shipping, storeId, deliveryId);
        created.forEach(order -> order.setDeliveryTime(time));
        orderRepository.saveAll(created);
        return created;
    }

    private boolean isBulkyProduct(Product p) {
        if (p == null) return false;
        String name = (p.getName() != null) ? p.getName().toLowerCase() : "";
        String catName = (p.getCategory() != null && p.getCategory().getName() != null)
                ? p.getCategory().getName().toLowerCase() : "";
        return name.contains("kệ") || name.contains("ke hoa") || name.contains("2 tầng") || name.contains("hai tầng")
                || name.contains("chậu lan") || name.contains("chau lan") || name.contains("cổng hoa") || name.contains("cong hoa")
                || name.contains("khung") || name.contains("cồng kềnh") || name.contains("chậu lớn")
                || catName.contains("kệ") || catName.contains("chậu") || catName.contains("khung") || catName.contains("phụ kiện");
    }

    private double estimateDistanceKm(Store store, String address) {
        if (address == null || address.isBlank()) return 3.0; // Bán kính gần chi nhánh
        String norm = address.toLowerCase();

        // 1. Kiểm tra các khu vực ngoại vi/ngoài tỉnh cách xa > 15km
        if (norm.contains("củ chi") || norm.contains("cu chi")
                || norm.contains("cần giờ") || norm.contains("can gio")
                || norm.contains("hóc môn") || norm.contains("hoc mon")
                || norm.contains("hà nội") || norm.contains("ha noi")
                || norm.contains("đà nẵng") || norm.contains("da nang")
                || norm.contains("long an") || norm.contains("tiền giang") || norm.contains("bến tre")
                || norm.contains("vũng tàu") || norm.contains("đồng nai") || norm.contains("ngoài 15km")
                || norm.contains("> 15km") || norm.contains(">15km")) {
            return 25.0; // Ngoài 15km
        }

        String storeName = (store != null && store.getName() != null) ? store.getName().toLowerCase() : "";

        // 2. Chi nhánh Cao Lãnh (Đồng Tháp)
        if (storeName.contains("cao lãnh") || storeName.contains("cao lanh")) {
            if (norm.contains("cao lãnh") || norm.contains("cao lanh") || norm.contains("phường 1")
                    || norm.contains("phường 2") || norm.contains("phường 3") || norm.contains("phường 4")
                    || norm.contains("an bình") || norm.contains("mỹ trà")) {
                return 3.0; // < 5km
            }
            if (norm.contains("lấp vò") || norm.contains("sa đéc") || norm.contains("tháp mười")
                    || norm.contains("đồng tháp") || norm.contains("huyện cao lãnh")) {
                return 9.0; // 5 - 15km
            }
            return 25.0; // Ngoài 15km
        }

        // 3. Chi nhánh TP. Thủ Đức (Làng Đại Học)
        if (storeName.contains("thủ đức") || storeName.contains("thu duc")) {
            if (norm.contains("thủ đức") || norm.contains("thu duc") || norm.contains("linh chiểu")
                    || norm.contains("linh trung") || norm.contains("linh tây") || norm.contains("bình thọ")
                    || norm.contains("tam phú") || norm.contains("dĩ an") || norm.contains("quận 9") || norm.contains("q9")) {
                return 3.5; // < 5km
            }
            if (norm.contains("quận 2") || norm.contains("q2") || norm.contains("bình thạnh")
                    || norm.contains("gò vấp") || norm.contains("quận 1") || norm.contains("q1")) {
                return 8.5; // 5 - 15km
            }
            if (norm.contains("quận 7") || norm.contains("quận 8") || norm.contains("bình chánh")
                    || norm.contains("nhà bè")) {
                return 18.0; // > 15km
            }
            return 8.0;
        }

        // 4. Chi nhánh Quận 7 (Phú Mỹ Hưng)
        if (storeName.contains("quận 7") || storeName.contains("q7") || storeName.contains("phú mỹ hưng")) {
            if (norm.contains("quận 7") || norm.contains("q7") || norm.contains("phú mỹ hưng")
                    || norm.contains("tân phong") || norm.contains("tân phú") || norm.contains("tân hưng")
                    || norm.contains("nhà bè")) {
                return 3.0; // < 5km
            }
            if (norm.contains("quận 4") || norm.contains("q4") || norm.contains("quận 1") || norm.contains("q1")
                    || norm.contains("quận 8") || norm.contains("q8") || norm.contains("quận 5")
                    || norm.contains("bình chánh")) {
                return 7.5; // 5 - 15km
            }
            if (norm.contains("thủ đức") || norm.contains("quận 12") || norm.contains("gò vấp")) {
                return 18.0; // > 15km
            }
            return 8.0;
        }

        // 5. Chi nhánh Bến Thành (Quận 1) - Trung tâm
        if (norm.contains("quận 1") || norm.contains("q1") || norm.contains("q.1") || norm.contains("bến thành")
                || norm.contains("quận 3") || norm.contains("q3") || norm.contains("quận 4") || norm.contains("q4")) {
            return 2.5; // < 5km
        }
        if (norm.contains("quận 5") || norm.contains("q5") || norm.contains("quận 10") || norm.contains("q10")
                || norm.contains("bình thạnh") || norm.contains("phú nhuận") || norm.contains("tân bình")
                || norm.contains("quận 8") || norm.contains("gò vấp") || norm.contains("quận 7")) {
            return 7.0; // 5 - 15km
        }
        if (norm.contains("quận 12") || norm.contains("thủ đức") || norm.contains("bình tân")) {
            return 12.0; // 5 - 15km
        }

        return 4.0;
    }

    private com.finalproject.service.CheckoutQuote calculateAmounts(BigDecimal subtotal, BigDecimal importTotal,
                                                                   Store store, String voucherCode,
                                                                   String shippingMethod, Long deliveryUnitId,
                                                                   String recipientAddress, List<CartItem> items) {
        BigDecimal shippingFee = BigDecimal.ZERO;
        if ("DELIVERY".equals(shippingMethod) && deliveryUnitId != null) {
            Delivery delivery = deliveryRepository.findById(deliveryUnitId)
                    .orElseThrow(() -> new IllegalArgumentException("Đơn vị giao hàng không tồn tại."));
            if (!Boolean.TRUE.equals(delivery.getIsActive()) || Boolean.TRUE.equals(delivery.getIsDeleted())) {
                throw new IllegalArgumentException("Đơn vị giao hàng hiện không khả dụng.");
            }

            // 1. Kiểm tra độ phủ biểu phí của chi nhánh thực hiện đơn
            long totalTariffsInDb = shippingTariffRepository.count();
            List<ShippingTariff> storeTariffs = (store != null)
                    ? shippingTariffRepository.searchTariffs(store.getId(), null)
                    : Collections.emptyList();

            // Nếu hệ thống đã có biểu phí nhưng chi nhánh được chọn chưa được cấu hình
            if (totalTariffsInDb > 0 && storeTariffs.isEmpty()) {
                String branchName = (store != null && store.getName() != null) ? store.getName() : "Chi nhánh";
                throw new IllegalArgumentException("Chi nhánh [" + branchName + "] hiện chưa có cấu hình biểu phí giao hàng. Quý khách vui lòng chọn hình thức 'Đến lấy tại cửa hàng' hoặc liên hệ hotline để được hỗ trợ.");
            }

            // 2. Tính khoảng cách và kiểm tra phạm vi giao hoa tươi (giới hạn 15km)
            double distanceKm = estimateDistanceKm(store, recipientAddress);
            if (distanceKm > 15.0) {
                String branchName = (store != null && store.getName() != null) ? store.getName() : "chi nhánh";
                throw new IllegalArgumentException("Địa chỉ nhận hàng cách " + branchName + " trên 15km (ngoài phạm vi giao hoa tươi trực tiếp bằng xe máy). Để hoa không héo và giữ dáng cắm, quý khách vui lòng chọn 'Đến lấy tại cửa hàng' hoặc liên hệ hotline chi nhánh.");
            }

            boolean isNearZone = distanceKm <= 5.0;

            // 3. Tra cứu biểu phí phù hợp từ ShippingTariff (không lấy trực tiếp từ delivery.price)
            ShippingTariff matchedTariff = null;
            if (!storeTariffs.isEmpty()) {
                List<ShippingTariff> activeTariffs = storeTariffs.stream()
                        .filter(t -> Boolean.TRUE.equals(t.getIsActive()))
                        .sorted(java.util.Comparator.comparingInt((ShippingTariff t) -> t.getStore() == null ? 1 : 0)
                                .thenComparing(ShippingTariff::getId))
                        .toList();

                // Ưu tiên 1: Trùng chi nhánh + trùng đơn vị vận chuyển + trùng cự ly (< 5km hoặc 5-15km)
                matchedTariff = activeTariffs.stream()
                        .filter(t -> t.getDelivery() != null && t.getDelivery().getId().equals(deliveryUnitId))
                        .filter(t -> isNearZone ? (t.getDestinationArea() != null && t.getDestinationArea().contains("< 5km"))
                                                : (t.getDestinationArea() != null && (t.getDestinationArea().contains("5 - 15km") || t.getDestinationArea().contains("xa hơn"))))
                        .findFirst()
                        .orElse(null);
            }

            if (matchedTariff != null) {
                shippingFee = matchedTariff.getBaseFee() != null ? matchedTariff.getBaseFee() : BigDecimal.ZERO;
                // Kiểm tra điều kiện phụ phí hàng lớn (Kệ hoa 1-2 tầng, Chậu lan lớn, Cổng hoa)
                boolean hasBulkyItem = items != null && items.stream().anyMatch(item -> isBulkyProduct(item.getProduct()));
                if (hasBulkyItem && matchedTariff.getBulkySurcharge() != null && matchedTariff.getBulkySurcharge().signum() > 0) {
                    shippingFee = shippingFee.add(matchedTariff.getBulkySurcharge());
                }
            } else {
                if (totalTariffsInDb > 0) {
                    throw new IllegalArgumentException("Không có biểu phí đang hoạt động phù hợp với đơn vị vận chuyển và khu vực nhận hàng. Vui lòng chọn đơn vị khác hoặc nhận tại cửa hàng.");
                }
                // Backward compatibility only for installations without any tariff configuration.
                shippingFee = (delivery.getPrice() != null && delivery.getPrice().signum() >= 0)
                        ? delivery.getPrice() : BigDecimal.ZERO;
            }
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
        BigDecimal total = subtotal.add(shippingFee).subtract(discount).max(BigDecimal.ZERO);
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
            if (!items.stream().allMatch(item -> com.finalproject.service.ProductAvailability.supportsBranch(item.getProduct(), candidate))) continue;
            boolean enough = demand.entrySet().stream().allMatch(entry ->
                    storeInventoryRepository.findByStoreIdAndProductId(candidate.getId(), entry.getKey())
                            .map(stock -> stock.isAvailableForSale() && stock.getQuantity() != null && stock.getQuantity() >= entry.getValue()).orElse(false));
            if (!Boolean.TRUE.equals(candidate.getStatus()) || !Boolean.TRUE.equals(candidate.getIsOpen())) {
                closedStoreHasStock |= enough;
                continue;
            }
            if (enough) return candidate;
        }
        if (closedStoreHasStock) throw new IllegalStateException("Chi nhánh có đủ hàng hiện không nhận đơn. Vui lòng thử lại khi chi nhánh mở cửa.");
        throw new IllegalStateException("Các chi nhánh đang mở không có hàng hoặc không đủ tồn kho cho mẫu hoa đã chọn. Vui lòng giảm số lượng hoặc chọn mẫu khác.");
    }

    private void requireFulfillmentBranch(List<CartItem> items, Store store) {
        for (CartItem item : items) {
            Product product = item.getProduct();
            if (!com.finalproject.service.ProductAvailability.supportsBranch(product, store)) {
                throw new IllegalStateException("Mẫu hoa '" + product.getName() + "' chỉ bán tại chi nhánh "
                        + product.getStore().getName() + ". Vui lòng chọn đúng chi nhánh nhận hàng.");
            }
        }
    }

    private void requireActiveCustomer(User user) {
        if (!Boolean.TRUE.equals(user.getIsActive()) || user.isRetiredDeliveryAccount()) {
            throw new org.springframework.security.access.AccessDeniedException("Tài khoản đã bị khóa hoặc vô hiệu hóa.");
        }
        boolean isAdmin = (user.getRole() == Role.ADMIN)
                || (user.getRoles() != null && user.getRoles().contains(Role.ADMIN));
        if (isAdmin) {
            throw new org.springframework.security.access.AccessDeniedException("Tài khoản Quản trị viên không thể thực hiện tạo đơn hoặc mua hàng.");
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
        // TP. Cao Lãnh (Đồng Tháp, Cao Lãnh, Sa Đéc, An Bình, Mỹ Trà, Lấp Vò...)
        if (norm.contains("cao lãnh") || norm.contains("cao lanh") || norm.contains("đồng tháp") || norm.contains("dong thap")
                || norm.contains("sa đéc") || norm.contains("sa dec") || norm.contains("an bình") || norm.contains("lấp vò")) {
            for (Store s : stores) {
                if (s.getName().toLowerCase().contains("cao lãnh") || s.getName().toLowerCase().contains("cao lanh")
                        || s.getAddress().toLowerCase().contains("cao lãnh") || s.getAddress().toLowerCase().contains("cao lanh")
                        || s.getAddress().toLowerCase().contains("đồng tháp") || s.getAddress().toLowerCase().contains("dong thap")) {
                    return s;
                }
            }
        }

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
    public void updateOrderStatus(Long orderId, OrderStatus status, String cancellationReason) {
        workflow.transition(orderId, status, cancellationReason);
    }

    @Override
    @Transactional
    public void requestRefund(String username, Long orderId) {
        workflow.requestRefund(username, orderId);
    }

    @Override
    @Transactional
    public void cancelOrder(String username, Long orderId) {
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
    private String extractFirstNonBlank(String input) {
        if (input == null) return null;
        if (input.contains(",")) {
            var values = java.util.Arrays.stream(input.split(",")).map(String::trim).filter(value -> !value.isEmpty()).distinct().toList();
            if (values.size() == 1) return values.get(0);
            return input.trim(); // Conflicting numbers must fail validation, never silently select one.
        }
        return input.trim();
    }

    private String cleanPhone(String phone) {
        if (phone == null) return null;
        String extracted = extractFirstNonBlank(phone);
        if (extracted == null) return null;
        String cleaned = extracted.replaceAll("[\\s.\\-\\(\\)]", "");
        if (cleaned.startsWith("+84")) {
            cleaned = "0" + cleaned.substring(3);
        } else if (cleaned.startsWith("84") && cleaned.length() >= 11) {
            cleaned = "0" + cleaned.substring(2);
        }
        return cleaned;
    }

    private boolean isValidPhoneNumber(String phone) {
        if (phone == null) return false;
        return phone.matches("^0(?:[35789][0-9]{8}|2[0-9]{9})$");
    }
}
