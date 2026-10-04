package com.finalproject.controller.customer;

import com.finalproject.entity.Order;
import com.finalproject.enums.OrderStatus;
import com.finalproject.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.security.Principal;
import java.util.Map;

/** Compatibility routes: customers may read payment status but cannot confirm receipt. */
@Controller
@RequestMapping("/customer/payment")
@RequiredArgsConstructor
public class CustomerPaymentController {
    private final OrderRepository orderRepository;

    private Order ownedBankOrder(Principal principal, Long orderId) {
        if (principal == null) throw new AccessDeniedException("Vui lòng đăng nhập.");
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!order.getUser().getUsername().equals(principal.getName()))
            throw new AccessDeniedException("Bạn không có quyền truy cập đơn hàng này.");
        if (!"BANK_TRANSFER".equals(order.getPaymentMethod()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn không sử dụng chuyển khoản ngân hàng.");
        return order;
    }

    @GetMapping("/{orderId}")
    public String viewPaymentPage(Principal principal, @PathVariable Long orderId) {
        ownedBankOrder(principal, orderId);
        return "redirect:/checkout/bank/" + orderId;
    }

    /** Read-only: only authorized staff can record receipt of a bank transfer. */
    @PostMapping("/{orderId}/process")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> processPayment(Principal principal, @PathVariable Long orderId) {
        Order order = ownedBankOrder(principal, orderId);
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.REFUND_REQUESTED
                || order.getStatus() == OrderStatus.REFUNDED || "REFUNDED".equals(order.getPaymentStatus()))
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Đơn đã hủy hoặc đang hoàn tiền."));
        boolean paid = "PAID".equals(order.getPaymentStatus());
        return ResponseEntity.ok(Map.of("success", paid, "alreadyPaid", paid, "pending", !paid,
                "orderId", order.getId(), "paymentStatus", order.getPaymentStatus(),
                "message", paid ? "Đã xác nhận nhận tiền." : "Chờ quản trị viên xác nhận tiền chuyển khoản đã nhận."));
    }
}