package com.finalproject.controller.customer;

import com.finalproject.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/customer/orders")
@RequiredArgsConstructor
public class CustomerOrderController {

    private final OrderService orderService;

    @GetMapping
    public String listCustomerOrders(Principal principal,
                                     @RequestParam(value = "status", required = false) String statusStr,
                                     Model model) {
        com.finalproject.enums.OrderStatus status = null;
        if (statusStr != null && !statusStr.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusStr)) {
            try {
                status = com.finalproject.enums.OrderStatus.valueOf(statusStr.toUpperCase());
            } catch (Exception ignored) {}
        }
        model.addAttribute("currentStatus", statusStr != null ? statusStr : "ALL");
        model.addAttribute("orders", orderService.getOrdersByUsernameAndStatus(principal.getName(), status));
        return "customer/orders";
    }

    @PostMapping("/refund/{id}")
    public String handleRefundOrder(Principal principal,
                                    @PathVariable("id") Long id,
                                    RedirectAttributes redirectAttributes) {
        try {
            orderService.requestRefund(principal.getName(), id);
            redirectAttributes.addFlashAttribute("successMessage", "Yêu cầu Trả hàng - Hoàn tiền cho đơn #" + id + " đã được gửi đến shop!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/customer/orders";
    }

    @PostMapping("/cancel/{id}")
    public String handleCancelOrder(Principal principal,
                                    @PathVariable("id") Long id,
                                    RedirectAttributes redirectAttributes) {
        try {
            orderService.cancelOrder(principal.getName(), id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy đơn hàng thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/customer/orders";
    }
}