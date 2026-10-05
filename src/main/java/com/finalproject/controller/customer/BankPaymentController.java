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
    private final com.finalproject.service.BankSandboxService sandbox;
    private record Attempt(Long orderId, String username, java.math.BigDecimal amount, java.time.Instant expires, String token) implements java.io.Serializable {}

    private Order ownedOrder(Long id, Principal principal) {
        Order order = orders.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (principal == null || !principal.getName().equals(order.getUser().getUsername()))
            throw new org.springframework.security.access.AccessDeniedException("Không phải đơn của bạn.");
        if (!"BANK_TRANSFER".equals(order.getPaymentMethod())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return order;
    }

    @GetMapping("/{orderId}")
    public String page(@PathVariable Long orderId, Principal principal, Model model, jakarta.servlet.http.HttpSession session) {
        Order order = ownedOrder(orderId, principal);
        model.addAttribute("order", order);
        model.addAttribute("bank", bank);
        model.addAttribute("transferContent", "DH" + orderId);
        if (bank.isMock() && "UNPAID".equals(order.getPaymentStatus()) && order.getStatus() == com.finalproject.enums.OrderStatus.PENDING) {
            Object previous = session.getAttribute("bankAttempt:" + orderId);
            Attempt attempt = previous instanceof Attempt saved && saved.username().equals(principal.getName())
                    && saved.expires().isAfter(java.time.Instant.now()) && saved.amount().compareTo(order.getTotalAmount()) == 0 ? saved
                    : new Attempt(orderId, principal.getName(), order.getTotalAmount(), java.time.Instant.now().plusSeconds(900), java.util.UUID.randomUUID().toString());
            session.setAttribute("bankAttempt:" + orderId, attempt);
            model.addAttribute("bankToken", attempt.token());
            model.addAttribute("bankExpiresAt", attempt.expires().toEpochMilli());
        }
        return "customer/bank-payment";
    }

    @PostMapping("/{orderId}/simulate")
    public String simulate(@PathVariable Long orderId, @RequestParam String token,
            @RequestParam java.math.BigDecimal amount, Principal principal, jakarta.servlet.http.HttpSession session,
            org.springframework.web.servlet.mvc.support.RedirectAttributes flash) {
        ownedOrder(orderId, principal);
        Object stored = session.getAttribute("bankAttempt:" + orderId);
        if (!(stored instanceof Attempt attempt) || !attempt.token().equals(token) || !attempt.username().equals(principal.getName())
                || !attempt.expires().isAfter(java.time.Instant.now()) || amount.compareTo(attempt.amount()) != 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phiên thanh toán không hợp lệ hoặc đã hết hạn.");
        try {
            String receipt = sandbox.pay(orderId, principal.getName(), attempt.amount());
            flash.addFlashAttribute("bankReceipt", receipt);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            flash.addFlashAttribute("errorMessage", "Thanh toán chưa thành công. " + ex.getMessage());
        }
        return "redirect:/checkout/bank/" + orderId;
    }

    @GetMapping("/{orderId}/status") @ResponseBody
    public Map<String, String> status(@PathVariable Long orderId, Principal principal) {
        Order order = ownedOrder(orderId, principal);
        return Map.of("paymentStatus", order.getPaymentStatus(), "orderStatus", order.getStatus().name());
    }
}
