package com.finalproject.controller.shipper;

import com.finalproject.entity.Order;
import com.finalproject.entity.User;
import com.finalproject.enums.OrderStatus;
import com.finalproject.repository.UserRepository;
import com.finalproject.service.ShipperService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/shipper")
@RequiredArgsConstructor
public class ShipperOrderController {

    private final ShipperService shipperService;
    private final UserRepository userRepository;

    @GetMapping("/orders")
    public String listAssignedOrders(@RequestParam(value = "status", required = false) String statusStr,
                                     Principal principal,
                                     Model model) {
        User shipper = userRepository.findByUsername(principal.getName()).orElseThrow();
        List<Order> orders;

        if (statusStr != null && !statusStr.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusStr)) {
            try {
                OrderStatus st = OrderStatus.valueOf(statusStr.toUpperCase());
                orders = shipperService.getAssignedOrdersByStatus(shipper.getId(), st);
            } catch (Exception ex) {
                orders = shipperService.getAssignedOrders(shipper.getId());
            }
        } else {
            orders = shipperService.getAssignedOrders(shipper.getId());
        }

        Map<String, Object> stats = shipperService.getShipperStats(shipper.getId());

        model.addAttribute("orders", orders);
        model.addAttribute("currentStatus", statusStr != null ? statusStr : "ALL");
        model.addAttribute("stats", stats);
        model.addAttribute("shipper", shipper);
        return "shipper/orders";
    }

    @PostMapping("/orders/update-status/{id}")
    public String updateDeliveryStatus(@PathVariable("id") Long id,
                                       @RequestParam("status") OrderStatus status,
                                       Principal principal,
                                       RedirectAttributes ra) {
        User shipper = userRepository.findByUsername(principal.getName()).orElseThrow();
        try {
            shipperService.updateDeliveryStatus(id, shipper.getId(), status);
            ra.addFlashAttribute("successMessage", "Cập nhật tiến độ giao đơn #" + id + " thành [" + status + "] thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/shipper/orders";
    }
}
