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
    public String listCustomerOrders(Principal principal, Model model) {
        model.addAttribute("orders", orderService.getOrdersByUsername(principal.getName()));
        return "customer/orders";
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