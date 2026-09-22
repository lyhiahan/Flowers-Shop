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
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public List<Order> checkout(String username, PaymentMethod paymentMethod) {
        Cart cart = cartService.getCartByUsername(username);
        List<CartItem> items = cart.getItems();

        if (items == null || items.isEmpty()) {
            throw new IllegalStateException("Giỏ hàng của bạn đang trống!");
        }

        User user = cart.getUser();
        List<Order> createdOrders = new ArrayList<>();

        // Tự động phân nhóm các sản phẩm theo chi nhánh xuất kho (pickup_store_id)
        Map<Store, List<CartItem>> itemsByStore = items.stream()
                .collect(Collectors.groupingBy(CartItem::getStore));

        for (Map.Entry<Store, List<CartItem>> storeEntry : itemsByStore.entrySet()) {
            Store store = storeEntry.getKey();
            List<CartItem> storeItems = storeEntry.getValue();

            // Tiếp tục tách đơn theo loại giao dịch: BUY hoặc RENT
            Map<OrderType, List<CartItem>> itemsByType = storeItems.stream()
                    .collect(Collectors.groupingBy(CartItem::getOrderType));

            for (Map.Entry<OrderType, List<CartItem>> typeEntry : itemsByType.entrySet()) {
                OrderType orderType = typeEntry.getKey();
                List<CartItem> subItems = typeEntry.getValue();

                BigDecimal totalAmount = BigDecimal.ZERO;
                BigDecimal totalDeposit = BigDecimal.ZERO;
                List<OrderDetail> orderDetails = new ArrayList<>();

                Order order = Order.builder()
                        .user(user)
                        .pickupStore(store)
                        .orderType(orderType)
                        .status(OrderStatus.PENDING)
                        .createdAt(LocalDateTime.now())
                        .build();

                for (CartItem item : subItems) {
                    Product product = item.getProduct();

                    // 1. Trừ kho lạc quan qua StoreInventory (@Version chống bán trùng)
                    StoreInventory inventory = storeInventoryRepository.findByStoreIdAndProductId(store.getId(), product.getId())
                            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dữ liệu kho cho sản phẩm: " + product.getName()));

                    if (inventory.getQuantity() < item.getQuantity()) {
                        throw new IllegalStateException("Sản phẩm [" + product.getName() + "] tại chi nhánh [" + store.getName() + "] không đủ số lượng tồn kho!");
                    }

                    inventory.setQuantity(inventory.getQuantity() - item.getQuantity());
                    storeInventoryRepository.save(inventory);

                    // 2. Tính tiền chi tiết từng sản phẩm
                    BigDecimal unitPrice;
                    BigDecimal itemDeposit = BigDecimal.ZERO;

                    if (orderType == OrderType.BUY) {
                        unitPrice = product.getPriceBuy();
                        BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
                        totalAmount = totalAmount.add(lineTotal);
                    } else {
                        // RENT: Đơn giá thuê theo ngày * số ngày thuê
                        long days = ChronoUnit.DAYS.between(item.getRentStartDate(), item.getRentEndDate());
                        if (days <= 0) days = 1;

                        unitPrice = product.getPriceRentPerDay();
                        BigDecimal rentCost = unitPrice.multiply(BigDecimal.valueOf(days)).multiply(BigDecimal.valueOf(item.getQuantity()));
                        itemDeposit = product.getDepositFee().multiply(BigDecimal.valueOf(item.getQuantity()));

                        totalAmount = totalAmount.add(rentCost);
                        totalDeposit = totalDeposit.add(itemDeposit);
                    }

                    OrderDetail detail = OrderDetail.builder()
                            .order(order)
                            .product(product)
                            .quantity(item.getQuantity())
                            .unitPrice(unitPrice)
                            .orderType(orderType)
                            .depositFee(itemDeposit)
                            .rentStartDate(item.getRentStartDate())
                            .rentEndDate(item.getRentEndDate())
                            .build();

                    orderDetails.add(detail);
                }

                order.setTotalAmount(totalAmount);
                order.setTotalDeposit(totalDeposit);
                order.setOrderDetails(orderDetails);
                Order savedOrder = orderRepository.save(order);

                // 3. Tạo bản ghi thanh toán (Payment)
                BigDecimal paymentTotal = totalAmount.add(totalDeposit);
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
                messagingTemplate.convertAndSend("/topic/store/" + store.getId() + "/orders",
                        "Có đơn hàng mới #" + savedOrder.getId() + " (" + orderType + ") vừa được tạo!");
            }
        }

        // Xóa sạch giỏ hàng sau khi đặt thành công
        cartService.clearCart(username);
        return createdOrders;
    }

    @Override
    public List<Order> getOrdersByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
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