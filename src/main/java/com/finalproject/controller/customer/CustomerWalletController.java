package com.finalproject.controller.customer;

import com.finalproject.service.CustomerWalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.data.domain.Page;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.time.DateTimeException;
import java.time.LocalDate;

@Controller @RequiredArgsConstructor
@RequestMapping("/customer/wallet")
public class CustomerWalletController {
    private final CustomerWalletService wallets;

    @GetMapping
    public String wallet(Principal principal, @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "") String fromDate,
                         @RequestParam(defaultValue = "") String toDate, Model model) {
        Page<CustomerWalletService.Entry> history;
        try {
            history = wallets.history(principal.getName(), page, parseDate(fromDate), parseDate(toDate));
        } catch (DateTimeException ex) {
            model.addAttribute("filterError", "Ngày lọc không hợp lệ. Vui lòng chọn lại ngày.");
            history = Page.empty();
        } catch (IllegalArgumentException ex) {
            model.addAttribute("filterError", ex.getMessage());
            history = Page.empty();
        }
        model.addAttribute("walletBalance", wallets.balance(principal.getName()));
        model.addAttribute("history", history);
        model.addAttribute("walletEnabled", wallets.isEnabled());
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("dateFilterActive", !fromDate.isBlank() || !toDate.isBlank());
        return "customer/wallet";
    }

    private LocalDate parseDate(String value) {
        if (value.isBlank()) return null;
        if (!value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"))
            throw new DateTimeException("Invalid date filter");
        return LocalDate.parse(value);
    }
}
