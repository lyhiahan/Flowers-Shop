package com.finalproject.service;

import com.finalproject.enums.OrderStatus;
import com.finalproject.enums.Role;
import com.finalproject.repository.OrderRepository;
import com.finalproject.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerAccountClosureService {
    private final UserRepository users;
    private final OrderRepository orders;
    private final OrderWorkflowService workflow;
    private final NotificationService notifications;
    private final EntityManager entityManager;

    public record Result(int cancelled, int awaitingRefund) {
        public String message() {
            return "Đã hủy " + cancelled + " đơn chưa thanh toán; " + awaitingRefund
                    + " đơn đã thanh toán cần hoàn tiền lại cho khách hàng.";
        }
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Result disable(Long id, boolean deletionRequested) {
        var customer = users.findLockedById(id).orElseThrow();
        entityManager.refresh(customer, LockModeType.PESSIMISTIC_WRITE);
        if (customer.hasRole(Role.ADMIN)) {
            throw new IllegalArgumentException("Không thể khóa tài khoản Quản trị viên.");
        }
        customer.setIsActive(false);
        customer.setAccountDisabledByAdmin(true);
        users.saveAndFlush(customer);
        int cancelled = 0, refund = 0;
        // Lock orders in a stable order, sharing the same locks as payment and handover.
        var ids = orders.findByUserIdOrderByCreatedAtDesc(id).stream().map(o -> o.getId()).sorted().toList();
        for (Long orderId : ids) {
            var order = orders.findLockedById(orderId).orElseThrow();
            entityManager.refresh(order, LockModeType.PESSIMISTIC_WRITE);
            if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED
                    && order.getStatus() != OrderStatus.PREPARING) continue;
            workflow.cancelForUnavailableCustomer(orderId, deletionRequested ? "xóa" : "khóa");
            if (order.getStatus() == OrderStatus.REFUND_REQUESTED) refund++; else cancelled++;
            for (var admin : users.findByRole(Role.ADMIN)) {
                notifications.sendNotification(admin, "Khách hàng bị " + (deletionRequested ? "xóa" : "khóa"),
                        order.getCancellationReason(), "ORDER_UPDATE", orderId);
            }
        }
        return new Result(cancelled, refund);
    }
}
