package com.finalproject.controller.staff;

import com.finalproject.entity.Order;
import com.finalproject.entity.User;
import com.finalproject.repository.StoreRepository;
import com.finalproject.repository.UserRepository;
import com.finalproject.service.StaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/staff/orders")
@RequiredArgsConstructor
public class StaffOrderController {

    private final StaffService staffService;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;

    @GetMapping
    public String viewStaffOrders(Principal principal, Model model) {
        User staff = userRepository.findByUsername(principal.getName()).orElseThrow();
        List<Order> orders = staffService.getOrdersByStaffStore(principal.getName());

        model.addAttribute("orders", orders);
        model.addAttribute("staffStore", staff.getStore());
        model.addAttribute("allStores", storeRepository.findByStatusTrue());
        return "staff/order-manage";
    }

    @PostMapping("/approve/{id}")
    public String approveOrder(Principal principal, @PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            staffService.approveOrder(principal.getName(), id);
            ra.addFlashAttribute("successMessage", "Duyệt đơn #" + id + " thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/staff/orders";
    }

    @PostMapping("/deliver/{id}")
    public String deliverOrder(Principal principal, @PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            staffService.deliverOrder(principal.getName(), id);
            ra.addFlashAttribute("successMessage", "Đã cập nhật trạng thái xuất/giao hàng đơn #" + id);
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/staff/orders";
    }

    @PostMapping("/return/{id}")
    public String returnOrder(Principal principal,
                              @PathVariable("id") Long id,
                              @RequestParam("returnStoreId") Long returnStoreId,
                              @RequestParam(value = "waiveFine", defaultValue = "false") boolean waiveFine,
                              @RequestParam(value = "damageFee", defaultValue = "0") BigDecimal damageFee,
                              RedirectAttributes ra) {
        try {
            staffService.processReturn(principal.getName(), id, returnStoreId, waiveFine, damageFee);
            ra.addFlashAttribute("successMessage", "Nhận trả đồ và hoàn tất tất toán cho đơn #" + id + " thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/staff/orders";
    }

    @PostMapping("/convert-to-buy/{id}")
    public String convertToBuy(Principal principal, @PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            staffService.convertRentToBuy(principal.getName(), id);
            ra.addFlashAttribute("successMessage", "Khách đã chuyển sang Mua Đứt thành công cho đơn #" + id);
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/staff/orders";
    }
}