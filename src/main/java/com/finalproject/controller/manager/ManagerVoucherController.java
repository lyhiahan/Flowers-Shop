package com.finalproject.controller.manager;

import com.finalproject.entity.Store;
import com.finalproject.entity.User;
import com.finalproject.entity.Voucher;
import com.finalproject.repository.StoreRepository;
import com.finalproject.repository.UserRepository;
import com.finalproject.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping({"/manager/vouchers", "/vendor/vouchers"})
@RequiredArgsConstructor
public class ManagerVoucherController {

    private final VoucherService voucherService;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;

    @GetMapping
    public String listVouchers(Principal principal, Model model) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        Long storeId = user.getStore() != null ? user.getStore().getId() : 1L;

        List<Voucher> vouchers = voucherService.getVouchersByStore(storeId);
        model.addAttribute("vouchers", vouchers);
        model.addAttribute("store", user.getStore());
        return "manager/vouchers";
    }

    @PostMapping("/add")
    public String createVoucher(Principal principal,
                                @RequestParam("code") String code,
                                @RequestParam("name") String name,
                                @RequestParam("discountType") String discountType,
                                @RequestParam("discountValue") BigDecimal discountValue,
                                @RequestParam(value = "minOrderValue", defaultValue = "0") BigDecimal minOrderValue,
                                @RequestParam(value = "quantity", defaultValue = "100") int quantity,
                                @RequestParam("startDate") String startDate,
                                @RequestParam("endDate") String endDate,
                                RedirectAttributes ra) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        Store store = user.getStore() != null ? user.getStore() : storeRepository.findById(1L).orElse(null);

        Voucher voucher = Voucher.builder()
                .code(code.trim().toUpperCase())
                .name(name.trim())
                .discountType(discountType)
                .discountValue(discountValue)
                .minOrderValue(minOrderValue)
                .store(store)
                .quantity(quantity)
                .usedCount(0)
                .startDate(LocalDate.parse(startDate))
                .endDate(LocalDate.parse(endDate))
                .isActive(true)
                .build();

        voucherService.createVoucher(voucher);
        ra.addFlashAttribute("successMessage", "Tạo chương trình khuyến mãi [" + code + "] thành công!");
        return "redirect:/manager/vouchers";
    }

    @PostMapping("/delete/{id}")
    public String deleteVoucher(@PathVariable("id") Long id, RedirectAttributes ra) {
        voucherService.deleteVoucher(id);
        ra.addFlashAttribute("successMessage", "Đã xóa mã khuyến mãi!");
        return "redirect:/manager/vouchers";
    }
}
