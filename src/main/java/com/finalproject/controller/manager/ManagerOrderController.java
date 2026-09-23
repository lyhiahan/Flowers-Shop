package com.finalproject.controller.manager;

import com.finalproject.entity.Order;
import com.finalproject.entity.Store;
import com.finalproject.entity.User;
import com.finalproject.enums.OrderStatus;
import com.finalproject.enums.Role;
import com.finalproject.repository.StoreRepository;
import com.finalproject.repository.UserRepository;
import com.finalproject.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping({"/manager/orders", "/vendor/orders"})
@RequiredArgsConstructor
public class ManagerOrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;

    @GetMapping
    public String listOrders(Principal principal,
                             @RequestParam(value = "status", required = false) String statusStr,
                             Model model) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        Long storeId = user.getStore() != null ? user.getStore().getId() : 1L;

        List<Order> orders = orderService.getOrdersByStore(storeId);
        if (statusStr != null && !statusStr.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusStr)) {
            try {
                OrderStatus st = OrderStatus.valueOf(statusStr.toUpperCase());
                orders = orders.stream().filter(o -> o.getStatus() == st).toList();
            } catch (Exception ignored) {}
        }

        List<User> shippers = userRepository.findByRole(Role.SHIPPER);

        model.addAttribute("orders", orders);
        model.addAttribute("currentStatus", statusStr != null ? statusStr : "ALL");
        model.addAttribute("shippers", shippers);
        model.addAttribute("store", user.getStore());
        return "manager/orders";
    }

    @PostMapping("/update-status/{id}")
    public String updateStatus(@PathVariable("id") Long id,
                               @RequestParam("status") OrderStatus status,
                               RedirectAttributes ra) {
        try {
            orderService.updateOrderStatus(id, status);
            ra.addFlashAttribute("successMessage", "Cập nhật trạng thái đơn #" + id + " thành [" + status + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/manager/orders";
    }

    @PostMapping("/assign-shipper/{id}")
    public String assignShipper(@PathVariable("id") Long id,
                                @RequestParam("shipperId") Long shipperId,
                                RedirectAttributes ra) {
        try {
            orderService.assignShipper(id, shipperId);
            ra.addFlashAttribute("successMessage", "Đã phân công Shipper cho đơn #" + id + " thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/manager/orders";
    }
}
