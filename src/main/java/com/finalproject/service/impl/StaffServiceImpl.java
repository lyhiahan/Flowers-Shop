package com.finalproject.service.impl;

import com.finalproject.entity.*;
import com.finalproject.enums.*;
import com.finalproject.exception.ResourceNotFoundException;
import com.finalproject.repository.*;
import com.finalproject.service.StaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffServiceImpl implements StaffService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final StoreInventoryRepository storeInventoryRepository;
    private final PaymentRepository paymentRepository;
    private final ActivityLogRepository activityLogRepository;

    private User getStaffUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Nhân viên không tồn tại: " + username));
        if (user.getStore() == null) {
            throw new AccessDeniedException("Tài khoản chưa được phân bổ vào chi nhánh nào!");
        }
        return user;
    }

    @Override
    public List<Order> getOrdersByStaffStore(String staffUsername) {
        User staff = getStaffUser(staffUsername);
        return orderRepository.findByPickupStoreIdOrderByCreatedAtDesc(staff.getStore().getId());
    }

    @Override
    @Transactional
    public void approveOrder(String staffUsername, Long orderId) {
        User staff = getStaffUser(staffUsername);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng không tồn tại: " + orderId));

        // Chống IDOR: Chỉ nhân viên chi nhánh xuất hàng mới được duyệt
        if (!order.getPickupStore().getId().equals(staff.getStore().getId())) {
            throw new AccessDeniedException("Bạn không có quyền can thiệp vào đơn hàng của chi nhánh khác!");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Đơn hàng không ở trạng thái PENDING");
        }

        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);

        logAction(staff, "APPROVE_ORDER", "Order", orderId);
    }

    @Override
    @Transactional
    public void deliverOrder(String staffUsername, Long orderId) {
        User staff = getStaffUser(staffUsername);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng không tồn tại: " + orderId));

        if (!order.getPickupStore().getId().equals(staff.getStore().getId())) {
            throw new AccessDeniedException("Bạn không thể xử lý đơn của chi nhánh khác!");
        }

        if (order.getOrderType() == OrderType.BUY) {
            order.setStatus(OrderStatus.COMPLETED);
        } else {
            order.setStatus(OrderStatus.RENTING);
        }
        orderRepository.save(order);

        logAction(staff, "DELIVER_ORDER", "Order", orderId);
    }

    @Override
    @Transactional
    public void processReturn(String staffUsername, Long orderId, Long returnStoreId, boolean waiveFine, BigDecimal damageFee) {
        User staff = getStaffUser(staffUsername);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderId));

        if (order.getOrderType() != OrderType.RENT) {
            throw new IllegalStateException("Chỉ áp dụng quy trình trả đồ cho đơn hàng THUÊ!");
        }

        Store returnStore = storeRepository.findById(returnStoreId)
                .orElseThrow(() -> new ResourceNotFoundException("Chi nhánh nhận trả đồ không tồn tại"));

        // 1. Tính phạt trễ hạn (nếu có)
        LocalDate today = LocalDate.now();
        BigDecimal totalFine = BigDecimal.ZERO;

        for (OrderDetail detail : order.getOrderDetails()) {
            detail.setActualReturnDate(today);
            if (detail.getRentEndDate() != null && today.isAfter(detail.getRentEndDate()) && !waiveFine) {
                long overdueDays = ChronoUnit.DAYS.between(detail.getRentEndDate(), today);
                // Hệ số phạt gấp đôi giá thuê/ngày
                BigDecimal penaltyPerDay = detail.getUnitPrice().multiply(BigDecimal.valueOf(2.0));
                BigDecimal itemFine = penaltyPerDay.multiply(BigDecimal.valueOf(overdueDays)).multiply(BigDecimal.valueOf(detail.getQuantity()));
                totalFine = totalFine.add(itemFine);
            }
        }

        if (damageFee != null && damageFee.compareTo(BigDecimal.ZERO) > 0) {
            totalFine = totalFine.add(damageFee);
        }

        order.setFineAmount(totalFine);
        order.setReturnStore(returnStore); // Ghi nhận chi nhánh nhận trả (hỗ trợ trả chéo kho)
        order.setStatus(OrderStatus.RETURNED);
        orderRepository.save(order);

        // 2. Nghiệp vụ trả chéo kho: Cộng tồn kho vào chi nhánh NHẬN TRẢ ĐỒ
        for (OrderDetail detail : order.getOrderDetails()) {
            StoreInventory inventory = storeInventoryRepository
                    .findByStoreIdAndProductId(returnStore.getId(), detail.getProduct().getId())
                    .orElseGet(() -> StoreInventory.builder()
                            .store(returnStore)
                            .product(detail.getProduct())
                            .quantity(0)
                            .build());

            inventory.setQuantity(inventory.getQuantity() + detail.getQuantity());
            storeInventoryRepository.save(inventory);
        }

        // 3. Hoàn cọc (Refund): Tiền cọc - Tiền phạt (nếu phạt lớn hơn cọc thì trừ hết cọc)
        BigDecimal deposit = order.getTotalDeposit() != null ? order.getTotalDeposit() : BigDecimal.ZERO;
        BigDecimal refundAmount = deposit.subtract(totalFine);
        if (refundAmount.compareTo(BigDecimal.ZERO) < 0) {
            refundAmount = BigDecimal.ZERO;
        }

        Payment refundPayment = Payment.builder()
                .order(order)
                .method(PaymentMethod.CASH)
                .amount(refundAmount)
                .status(PaymentStatus.REFUNDED)
                .refundMethod("TIỀN MẶT / CHUYỂN KHOẢN TẠI QUẦY")
                .refundNote("Đã tất toán trả đồ tại " + returnStore.getName() + " (Khấu trừ phạt: " + totalFine + "đ)")
                .refundedAt(LocalDateTime.now())
                .build();
        paymentRepository.save(refundPayment);

        logAction(staff, "CONFIRM_RETURN_STORE_" + returnStore.getId(), "Order", orderId);
    }

    @Override
    @Transactional
    public void convertRentToBuy(String staffUsername, Long orderId) {
        User staff = getStaffUser(staffUsername);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng không tồn tại: " + orderId));

        if (order.getOrderType() != OrderType.RENT || order.getStatus() != OrderStatus.RENTING) {
            throw new IllegalStateException("Chỉ áp dụng chuyển đổi khi khách đang thuê đồ!");
        }

        // Tính số tiền cần thanh toán thêm: Giá mua đứt - Tiền cọc đã đóng
        BigDecimal totalBuyPrice = BigDecimal.ZERO;
        for (OrderDetail detail : order.getOrderDetails()) {
            BigDecimal buyPrice = detail.getProduct().getPriceBuy();
            totalBuyPrice = totalBuyPrice.add(buyPrice.multiply(BigDecimal.valueOf(detail.getQuantity())));
            detail.setOrderType(OrderType.BUY);
        }

        BigDecimal depositPaid = order.getTotalDeposit() != null ? order.getTotalDeposit() : BigDecimal.ZERO;
        BigDecimal additionalAmount = totalBuyPrice.subtract(depositPaid);
        if (additionalAmount.compareTo(BigDecimal.ZERO) < 0) {
            additionalAmount = BigDecimal.ZERO;
        }

        // Tạo bản ghi thanh toán phần tiền chênh lệch
        Payment additionalPayment = Payment.builder()
                .order(order)
                .method(PaymentMethod.CASH)
                .amount(additionalAmount)
                .status(PaymentStatus.SUCCESS)
                .paidAt(LocalDateTime.now())
                .refundNote("Thu thêm phần chênh lệch khi khách chuyển sang mua đứt")
                .build();
        paymentRepository.save(additionalPayment);

        order.setOrderType(OrderType.BUY);
        order.setStatus(OrderStatus.COMPLETED);
        order.setTotalAmount(order.getTotalAmount().add(additionalAmount));
        orderRepository.save(order);

        logAction(staff, "CONVERT_RENT_TO_BUY", "Order", orderId);
    }

    private void logAction(User user, String action, String targetType, Long targetId) {
        ActivityLog log = ActivityLog.builder()
                .user(user)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .timestamp(LocalDateTime.now())
                .build();
        activityLogRepository.save(log);
    }
}