package com.finalproject.controller.staff;

import com.finalproject.enums.OrderStatus;
import com.finalproject.service.StaffService;
import com.finalproject.service.StoreAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/staff/orders")
@RequiredArgsConstructor
public class StaffOrderController {
    private final StaffService staffService;
    private final StoreAccessService access;
    private final com.finalproject.repository.DeliveryRepository deliveries;

    @GetMapping({"", "/preparation", "/delivery"})
    public String list(@RequestParam(defaultValue="") String keyword,
                       @RequestParam(required=false) OrderStatus status,
                       @RequestParam(defaultValue="ALL") String fulfillment,
                       @RequestParam(defaultValue="0") int page,
                       @RequestParam(defaultValue="12") int size,
                       HttpServletRequest request, Principal principal, Model model) {
        String section = request.getRequestURI().endsWith("/preparation") ? "preparation"
                : request.getRequestURI().endsWith("/delivery") ? "delivery" : "orders";
        var store = access.selectedStore();
        var result = store == null ? org.springframework.data.domain.Page.<com.finalproject.entity.Order>empty()
                : staffService.searchOrders(principal.getName(), section, keyword, status, fulfillment, page, size);
        model.addAttribute("orders", result.getContent());
        com.finalproject.utils.StorePages.model(model, result);
        model.addAttribute("staffActive", section);
        model.addAttribute("pageTitle", switch(section) {
            case "preparation" -> "Chuẩn bị hoa";
            case "delivery" -> "Giao & nhận hoa";
            default -> "Đơn hàng của chi nhánh";
        });
        model.addAttribute("listPath", "/staff/orders" + ("orders".equals(section) ? "" : "/" + section));
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("fulfillment", fulfillment);
        model.addAttribute("statusOptions", switch(section) {
            case "preparation" -> List.of(OrderStatus.CONFIRMED, OrderStatus.PREPARING);
            case "delivery" -> List.of(OrderStatus.PREPARING, OrderStatus.PICKED_UP, OrderStatus.DELIVERING, OrderStatus.DELIVERED);
            default -> List.of(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PREPARING, OrderStatus.DELIVERING,
                    OrderStatus.DELIVERED, OrderStatus.COMPLETED, OrderStatus.CANCELLED, OrderStatus.REFUND_REQUESTED, OrderStatus.REFUNDED);
        });
        return "staff/order-manage";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Principal principal, Model model) {
        var order = staffService.getOrder(principal.getName(), id);
        model.addAttribute("order", order);
        model.addAttribute("deliveryUnit", order.getDeliveryUnitId() == null ? null : deliveries.findById(order.getDeliveryUnitId()).orElse(null));
        model.addAttribute("orderActions", staffService.actions(order));
        model.addAttribute("staffActive", "orders");
        var steps = order.isPickup()
                ? List.of(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PREPARING, OrderStatus.COMPLETED)
                : List.of(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PREPARING, OrderStatus.DELIVERING, OrderStatus.DELIVERED, OrderStatus.COMPLETED);
        model.addAttribute("steps", steps);
        model.addAttribute("stepIndex", steps.indexOf(order.getStatus()));
        return "staff/order-detail";
    }

    @PostMapping("/status/{id}")
    public String updateStatus(@PathVariable Long id, @RequestParam OrderStatus status,
                               @RequestParam(defaultValue="false") boolean codCollected,
                               @RequestParam(defaultValue="detail") String returnTo,
                               Principal principal, RedirectAttributes ra) {
        return change(id, status, codCollected, returnTo, principal, ra);
    }

    @PostMapping("/approve/{id}")
    public String approve(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        return change(id, OrderStatus.CONFIRMED, false, "orders", principal, ra);
    }

    @PostMapping("/prepare/{id}")
    public String prepare(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        return change(id, OrderStatus.PREPARING, false, "orders", principal, ra);
    }

    @PostMapping("/deliver/{id}")
    public String deliver(@PathVariable Long id, @RequestParam(defaultValue="false") boolean codCollected,
                          Principal principal, RedirectAttributes ra) {
        return change(id, OrderStatus.COMPLETED, codCollected, "orders", principal, ra);
    }

    private String change(Long id, OrderStatus next, boolean codCollected, String returnTo, Principal principal, RedirectAttributes ra) {
        try {
            staffService.updateStatus(principal.getName(), id, next, codCollected);
            ra.addFlashAttribute("successMessage", "Đã cập nhật đơn #HD" + id + ": " + next.getDisplayName());
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:" + switch(returnTo) {
            case "orders" -> "/staff/orders";
            case "preparation" -> "/staff/orders/preparation";
            case "delivery" -> "/staff/orders/delivery";
            default -> "/staff/orders/" + id;
        };
    }
}
