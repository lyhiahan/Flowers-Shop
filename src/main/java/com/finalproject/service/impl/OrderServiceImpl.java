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
    private final SimpMessagingTemplate messagingTemplate;
    private final com.finalproject.service.VoucherService voucherService;
    private final TransactionRepository transactionRepository;

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
        if (paymentMethod != PaymentMethod.COD && paymentMethod != PaymentMethod.BANK_TRANSFER) {
            throw new IllegalArgumentException("Phương thức thanh toán chưa được hỗ trợ.");
        }
        if (!"PICKUP".equals(shippingMethod) && !"DELIVERY".equals(shippingMethod)) {
            throw new IllegalArgumentException("Hình thức nhận hàng không hợp lệ.");
        }
        User customer = userRepository.findByUsername(username).orElseThrow();
        Cart cart = cartRepository.findLockedByUserId(customer.getId())
                .orElseThrow(() -> new IllegalStateException("Giỏ hàng đang trống."));
        List<CartItem> allItems = cartItemRepository.findByCartId(cart.getId());

        if (allItems == null || allItems.isEmpty()) {
            throw new IllegalStateException("Giỏ hàng của bạn đang trống!");
        }

        List<CartItem> items;
        if (selectedCartItemIds != null && !selectedCartItemIds.isEmpty()) {
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
            assignedStore = findNearestStore(finalRecipientAddress);
        }

        if (!Boolean.TRUE.equals(assignedStore.getStatus()) || !Boolean.TRUE.equals(assignedStore.getIsOpen())) {
            throw new IllegalStateException("Chi nhánh hiện không nhận đơn.");
        }
        BigDecimal totalAmount = BigDecimal.ZERO;
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

        for (CartItem item : items) {
            Product product = item.getProduct();

            if (item.getOrderType() != OrderType.BUY || item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Giỏ hàng chỉ hỗ trợ mua hoa với số lượng dương.");
            }
            if (!Boolean.TRUE.equals(product.getIsActive()) || !Boolean.TRUE.equals(product.getIsSelling())) {
                throw new IllegalStateException("Sản phẩm đã ngừng bán: " + product.getName());
            }
            StoreInventory inventory = storeInventoryRepository.findByStoreIdAndProductId(assignedStore.getId(), product.getId())
                    .orElseThrow(() -> new IllegalStateException("Chi nhánh không có hàng: " + product.getName()));
            if (inventory.getQuantity() < item.getQuantity()) {
                throw new IllegalStateException("Không đủ tồn kho: " + product.getName());
            }
            inventory.setQuantity(inventory.getQuantity() - item.getQuantity());
            storeInventoryRepository.save(inventory);
            BigDecimal unitPrice = product.getSellingPrice();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);

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

        // Áp dụng voucher nếu có
        BigDecimal discount = BigDecimal.ZERO;
        if (voucherCode != null && !voucherCode.trim().isEmpty()) {
            Voucher v = voucherService.findByCode(voucherCode.trim());
            if (v == null) throw new IllegalArgumentException("Mã giảm giá không hợp lệ.");
            if (v.getStore() != null && !v.getStore().getId().equals(assignedStore.getId())) {
                throw new IllegalArgumentException("Mã giảm giá không áp dụng cho chi nhánh này.");
            }
            discount = voucherService.calculateDiscount(v, totalAmount, BigDecimal.ZERO);
            order.setVoucher(v);
            order.setDiscountAmount(discount);
            v.setUsedCount(v.getUsedCount() + 1);
            voucherService.createVoucher(v);
        }
        BigDecimal finalAmount = totalAmount.subtract(discount).max(BigDecimal.ZERO);
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
    public void assignShipper(Long orderId, Long shipperId) {
        Order order = workflow.lockedOrder(orderId);
        access.requireManager(order.getPickupStore());
        User shipper = userRepository.findById(shipperId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên giao hàng"));

        if (shipper.getRole() != Role.SHIPPER || !Boolean.TRUE.equals(shipper.getIsActive())
                || shipper.getStore() == null || !shipper.getStore().getId().equals(order.getPickupStore().getId())) {
            throw new IllegalArgumentException("Shipper phải đang hoạt động và thuộc chi nhánh xử lý đơn.");
        }
        if (order.getStatus() != OrderStatus.PREPARING || "PICKUP".equals(order.getShippingMethod())) {
            throw new IllegalStateException("Chỉ phân công giao hàng cho đơn đang chuẩn bị.");
        }
        if (!"COD".equals(order.getPaymentMethod()) && !"PAID".equals(order.getPaymentStatus())) {
            throw new IllegalStateException("Cần xác nhận chuyển khoản trước khi giao đơn cho shipper.");
        }
        order.setShipper(shipper);
        if (order.getStatus() == OrderStatus.CONFIRMED || order.getStatus() == OrderStatus.PREPARING) {
            order.setStatus(OrderStatus.PICKED_UP);
        }
        orderRepository.save(order);
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
