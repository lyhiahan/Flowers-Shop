package com.finalproject.exception;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    // 1. Xử lý xung đột khi 2 người cùng thao tác trên 1 bản ghi có @Version
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public String handleOptimisticLockingFailure(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", 
            "Sản phẩm hoặc đơn hàng vừa được cập nhật bởi một thao tác khác. Vui lòng thử lại!");
        return "redirect:/cart";
    }

    // 2. Xử lý lỗi không tìm thấy tài nguyên
    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleResourceNotFound(ResourceNotFoundException ex, Model model) {
        model.addAttribute("errorMessage", ex.getMessage());
        return "common/error";
    }

    // 3. Xử lý lỗi truy cập trái phép (IDOR / Sai Role)
    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(AccessDeniedException ex, Model model) {
        model.addAttribute("errorMessage", "Bạn không có quyền truy cập hoặc thao tác trên tài nguyên này!");
        return "common/error";
    }

    // 4. Bắt các lỗi Runtime chung
    @ExceptionHandler(Exception.class)
    public String handleGeneralException(Exception ex, Model model) {
        model.addAttribute("errorMessage", "Đã xảy ra lỗi: " + ex.getMessage());
        return "common/error";
    }
}