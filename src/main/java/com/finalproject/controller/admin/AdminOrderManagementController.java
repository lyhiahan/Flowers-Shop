package com.finalproject.controller.admin;

import com.finalproject.entity.Order;
import com.finalproject.enums.OrderStatus;
import com.finalproject.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/orders-manage")
@RequiredArgsConstructor
public class AdminOrderManagementController {

    private final OrderService orderService;
    private final com.finalproject.service.OrderWorkflowService workflow;

    @GetMapping
    public String listAllOrders(@RequestParam(value = "status", required = false) String statusStr,
                                Model model) {
        List<Order> orders = orderService.getAllOrders();

        if (statusStr != null && !statusStr.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusStr)) {
            try {
                OrderStatus st = OrderStatus.valueOf(statusStr.toUpperCase());
                orders = orders.stream().filter(o -> o.getStatus() == st).toList();
            } catch (Exception ignored) {}
        }

        model.addAttribute("orders", orders);
        model.addAttribute("currentStatus", statusStr != null ? statusStr : "ALL");
        model.addAttribute("orderStatuses", java.util.List.of(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PREPARING,
                OrderStatus.PICKED_UP, OrderStatus.DELIVERING, OrderStatus.DELIVERED, OrderStatus.COMPLETED,
                OrderStatus.CANCELLED, OrderStatus.REFUND_REQUESTED, OrderStatus.REFUNDED));
        return "admin/orders-manage";
    }

    @PostMapping("/update-status/{id}")
    public String updateOrderStatus(@PathVariable("id") Long id,
                                    @RequestParam("status") OrderStatus status,
                                    RedirectAttributes ra) {
        try {
            orderService.updateOrderStatus(id, status);
            ra.addFlashAttribute("successMessage", "Cập nhật trạng thái đơn #" + id + " thành [" + status + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/orders-manage";
    }

    @PostMapping("/confirm-payment/{id}")
    public String confirmPayment(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            workflow.confirmPayment(id);
            ra.addFlashAttribute("successMessage", "Đã xác nhận thanh toán đơn #" + id);
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/orders-manage";
    }
}
