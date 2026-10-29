package com.finalproject.controller.common;

import com.finalproject.service.OrderAftercareService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.io.IOException;

@Controller @RequestMapping("/order-care/{id}") @RequiredArgsConstructor
public class OrderAftercareController {
    private final OrderAftercareService care;
    @FunctionalInterface private interface Action { void run() throws IOException; }
    private String execute(Long id, RedirectAttributes flash, Action action, String success) {
        String destination = care.detailPath(id);
        try { action.run(); flash.addFlashAttribute("successMessage", success); }
        catch (IllegalArgumentException | IllegalStateException ex) { flash.addFlashAttribute("errorMessage", ex.getMessage()); }
        catch (IOException ex) { flash.addFlashAttribute("errorMessage", "Không thể lưu ảnh. Vui lòng thử lại."); }
        return "redirect:" + destination;
    }
    @PostMapping("/flower-photo")
    public String photo(@PathVariable Long id, @RequestParam MultipartFile photo, @RequestParam(defaultValue="") String note, RedirectAttributes flash) {
        return execute(id, flash, () -> care.sendFlowerPhoto(id, photo, note), "Đã gửi ảnh thành phẩm để khách duyệt.");
    }
    @PostMapping("/photo-review")
    public String review(@PathVariable Long id, @RequestParam int revision, @RequestParam String decision, @RequestParam(defaultValue="") String comment, RedirectAttributes flash) {
        return execute(id, flash, () -> care.reviewFlowerPhoto(id, revision, decision, comment), "Đã gửi phản hồi ảnh thành phẩm.");
    }
    @PostMapping("/delivery-proof")
    public String delivery(@PathVariable Long id, @RequestParam MultipartFile photo, @RequestParam String note, RedirectAttributes flash) {
        return execute(id, flash, () -> care.recordDelivery(id, photo, note), "Đã lưu minh chứng bàn giao.");
    }
    @PostMapping("/received")
    public String received(@PathVariable Long id, RedirectAttributes flash) {
        return execute(id, flash, () -> care.confirmReceived(id), "Đã ghi nhận bạn nhận được hoa.");
    }
    @PostMapping("/support")
    public String support(@PathVariable Long id, @RequestParam String reason, @RequestParam String description, @RequestParam(required=false) MultipartFile photo, RedirectAttributes flash) {
        return execute(id, flash, () -> care.openSupport(id, reason, description, photo), "Đã gửi yêu cầu hỗ trợ. Cửa hàng sẽ phản hồi trong chi tiết đơn.");
    }
    @PostMapping("/support-response")
    public String response(@PathVariable Long id, @RequestParam String status, @RequestParam String response, RedirectAttributes flash) {
        return execute(id, flash, () -> care.respondSupport(id, status, response), "Đã cập nhật phản hồi hỗ trợ; không thay đổi thanh toán của đơn.");
    }
}
