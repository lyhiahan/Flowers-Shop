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
    private final com.finalproject.repository.VoucherRepository voucherRepository;
    private final UserRepository userRepository;
    private final com.finalproject.service.StoreAccessService storeAccess;
    private final StoreRepository storeRepository;

    @GetMapping
    public String listVouchers(Principal principal, Model model) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        Long storeId = storeAccess.assignedStore().getId();

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
        Store store = storeAccess.assignedStore();

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
        storeAccess.requireManager(voucherRepository.findById(id).orElseThrow().getStore());
        voucherService.deleteVoucher(id);
        ra.addFlashAttribute("successMessage", "Đã xóa mã khuyến mãi!");
        return "redirect:/manager/vouchers";
    }
}
