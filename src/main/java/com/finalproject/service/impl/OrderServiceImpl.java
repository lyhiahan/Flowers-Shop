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
        Cart cart = cartService.getCartByUsername(username);
        List<CartItem> allItems = cart.getItems();

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

        if (isPickup && pickupStoreId != null) {
            assignedStore = storeRepository.findById(pickupStoreId)
                    .orElseGet(() -> findNearestStore(recipientAddress));
            finalRecipientAddress = "Nhận tại chi nhánh: " + assignedStore.getName() + " - " + assignedStore.getAddress();
        } else {
            finalRecipientAddress = (recipientAddress != null && !recipientAddress.trim().isEmpty()) ? recipientAddress.trim() : user.getAddress();
            assignedStore = findNearestStore(finalRecipientAddress);
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderDetail> orderDetails = new ArrayList<>();

        CartItem firstItem = items.get(0);
        java.time.LocalDate finalDelivDate = deliveryDate != null ? deliveryDate : (firstItem.getDeliveryDate() != null ? firstItem.getDeliveryDate() : java.time.LocalDate.now());
        String finalRecipientName = (recipientName != null && !recipientName.trim().isEmpty()) ? recipientName.trim() : user.getFullName();
        String finalRecipientPhone = (recipientPhone != null && !recipientPhone.trim().isEmpty()) ? recipientPhone.trim() : user.getPhone();
        String finalGreeting = (greetingMessage != null && !greetingMessage.trim().isEmpty()) ? greetingMessage.trim() : null;

        Order order = Order.builder()
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

            // Trừ kho tại chi nhánh được phân bổ
            StoreInventory inventory = storeInventoryRepository.findByStoreIdAndProductId(assignedStore.getId(), product.getId())
                    .orElseGet(() -> {
                        StoreInventory newInv = StoreInventory.builder()
                                .store(assignedStore)
                                .product(product)
                                .quantity(50)
                                .build();
                        return storeInventoryRepository.save(newInv);
                    });

            if (inventory.getQuantity() < item.getQuantity()) {
                inventory.setQuantity(inventory.getQuantity() + 50);
            }
            inventory.setQuantity(Math.max(0, inventory.getQuantity() - item.getQuantity()));
            storeInventoryRepository.save(inventory);

            BigDecimal unitPrice = product.getPriceBuy() != null ? product.getPriceBuy() : BigDecimal.ZERO;
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
            if (v != null) {
                try {
                    discount = voucherService.calculateDiscount(v, totalAmount, BigDecimal.valueOf(30000));
                    order.setVoucher(v);
                    order.setDiscountAmount(discount);
                    v.setUsedCount(v.getUsedCount() + 1);
                    voucherService.createVoucher(v);
                } catch (Exception ignored) {}
            }
        }

        BigDecimal finalAmount = totalAmount.subtract(discount).max(BigDecimal.ZERO);
        order.setTotalAmount(finalAmount);
        order.setTotalDeposit(BigDecimal.ZERO);
        order.setPaymentMethod(paymentMethod != null ? paymentMethod.name() : "COD");
        order.setPaymentStatus(paymentMethod == PaymentMethod.COD ? "UNPAID" : "PAID");
        order.setOrderDetails(orderDetails);
        Order savedOrder = orderRepository.save(order);

        // 3. Tạo bản ghi thanh toán (Payment)
        BigDecimal paymentTotal = finalAmount;
        Payment payment = Payment.builder()
                .order(savedOrder)
                .method(paymentMethod)
                .amount(paymentTotal)
                .status(paymentMethod == PaymentMethod.COD ? PaymentStatus.PENDING : PaymentStatus.SUCCESS)
                .paidAt(paymentMethod == PaymentMethod.COD ? null : LocalDateTime.now())
                .build();
        paymentRepository.save(payment);

        createdOrders.add(savedOrder);

        // 4. Kích hoạt WebSocket gửi thông báo đơn mới đến chi nhánh tương ứng
        try {
            messagingTemplate.convertAndSend("/topic/store/" + assignedStore.getId() + "/orders",
                    "Có đơn hoa mới #" + savedOrder.getId() + " vừa được tạo!");
        } catch (Exception ignored) {}

        // Xóa các sản phẩm đã thanh toán khỏi giỏ hàng
        for (CartItem processedItem : items) {
            cartService.removeCartItem(username, processedItem.getId());
        }
        return createdOrders;
    }

    private Store findNearestStore(String address) {
        List<Store> stores = storeRepository.findByStatusTrue();
        if (stores.isEmpty()) {
            stores = storeRepository.findAll();
        }
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
        Order order = getOrderById(orderId);
        order.setStatus(status);

        // Khi đơn hàng giao thành công hoặc hoàn tất, tự động cộng doanh thu vào ví của cửa hàng (trừ 10% hoa hồng sàn)
        if (status == OrderStatus.DELIVERED || status == OrderStatus.COMPLETED) {
            order.setPaymentStatus("PAID");
            Store store = order.getPickupStore();
            if (store != null && order.getTotalAmount() != null) {
                // Hoa hồng sàn 10%
                BigDecimal storeIncome = order.getTotalAmount().multiply(BigDecimal.valueOf(0.90));
                store.setEWallet(store.getEWallet().add(storeIncome));
                storeRepository.save(store);

                // Ghi nhật ký dòng tiền
                transactionRepository.save(Transaction.builder()
                        .store(store)
                        .amount(storeIncome)
                        .type("ORDER_PAYMENT")
                        .isUp(true)
                        .description("Thu nhập đơn hàng #" + order.getId() + " sau khi trừ 10% phí sàn")
                        .status("COMPLETED")
                        .build());
            }
        }

        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void assignShipper(Long orderId, Long shipperId) {
        Order order = getOrderById(orderId);
        User shipper = userRepository.findById(shipperId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên giao hàng"));

        order.setShipper(shipper);
        if (order.getStatus() == OrderStatus.CONFIRMED || order.getStatus() == OrderStatus.PREPARING) {
            order.setStatus(OrderStatus.PICKED_UP);
        }
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void requestRefund(String username, Long orderId) {
        Order order = getOrderById(orderId);
        if (!order.getUser().getUsername().equals(username)) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền yêu cầu hoàn tiền đơn này!");
        }

        if (order.getStatus() != OrderStatus.DELIVERED && order.getStatus() != OrderStatus.COMPLETED) {
            throw new IllegalStateException("Chỉ có thể yêu cầu trả hàng - hoàn tiền cho đơn đã giao!");
        }

        order.setStatus(OrderStatus.REFUNDED);
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void cancelOrder(String username, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng không tồn tại"));

        if (!order.getUser().getUsername().equals(username)) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không thể hủy đơn của người khác!");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể hủy đơn khi đang ở trạng thái Chờ xác nhận (PENDING)!");
        }

        // Hoàn lại số lượng tồn kho cho chi nhánh
        for (OrderDetail detail : order.getOrderDetails()) {
            StoreInventory inventory = storeInventoryRepository
                    .findByStoreIdAndProductId(order.getPickupStore().getId(), detail.getProduct().getId())
                    .orElse(null);
            if (inventory != null) {
                inventory.setQuantity(inventory.getQuantity() + detail.getQuantity());
                storeInventoryRepository.save(inventory);
            }
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
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