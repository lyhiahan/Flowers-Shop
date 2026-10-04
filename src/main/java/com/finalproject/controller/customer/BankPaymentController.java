package com.finalproject.controller.customer;

import com.finalproject.config.BankTransferConfig;
import com.finalproject.entity.Order;
import com.finalproject.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.security.Principal;
import java.util.Map;

@Controller @RequestMapping("/checkout/bank") @RequiredArgsConstructor
public class BankPaymentController {
    private final OrderRepository orders;
    private final BankTransferConfig bank;

    private Order ownedOrder(Long id, Principal principal) {
        Order order = orders.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (principal == null || !principal.getName().equals(order.getUser().getUsername()))
            throw new org.springframework.security.access.AccessDeniedException("Không phải đơn của bạn.");
        if (!"BANK_TRANSFER".equals(order.getPaymentMethod())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return order;
    }

    @GetMapping("/{orderId}")
    public String page(@PathVariable Long orderId, Principal principal, Model model) {
        model.addAttribute("order", ownedOrder(orderId, principal));
        model.addAttribute("bank", bank);
        model.addAttribute("transferContent", "DH" + orderId);
        return "customer/bank-payment";
    }

    @GetMapping("/{orderId}/status") @ResponseBody
    public Map<String, String> status(@PathVariable Long orderId, Principal principal) {
        Order order = ownedOrder(orderId, principal);
        return Map.of("paymentStatus", order.getPaymentStatus(), "orderStatus", order.getStatus().name());
    }
}
