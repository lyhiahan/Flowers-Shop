package com.finalproject.exception;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class,
            org.springframework.validation.BindException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class, IllegalArgumentException.class})
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
    public String handleInvalidInput(Exception ex, Model model) {
        model.addAttribute("errorMessage", ex instanceof IllegalArgumentException && !(ex instanceof org.springframework.web.method.annotation.MethodArgumentTypeMismatchException)
                ? ex.getMessage() : "Dữ liệu nhập không hợp lệ. Vui lòng kiểm tra giá, số lượng và các trường bắt buộc.");
        return "common/error";
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED)
    public String handleWrongMethod(Model model) {
        model.addAttribute("errorMessage", "Phương thức yêu cầu không được hỗ trợ.");
        return "common/error";
    }

    @ExceptionHandler({IllegalStateException.class, org.springframework.dao.PessimisticLockingFailureException.class})
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.CONFLICT)
    public String handleConflict(Exception ex, Model model) {
        model.addAttribute("errorMessage", ex instanceof IllegalStateException ? ex.getMessage() : "Dữ liệu đang được xử lý. Vui lòng thử lại.");
        return "common/error";
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public String handleStatus(org.springframework.web.server.ResponseStatusException ex, Model model, jakarta.servlet.http.HttpServletResponse response) {
        response.setStatus(ex.getStatusCode().value());
        model.addAttribute("errorMessage", "Không thể thực hiện yêu cầu này. Vui lòng kiểm tra lại đơn hàng.");
        return "common/error";
    }

    // 1. Xử lý xung đột khi 2 người cùng thao tác trên 1 bản ghi có @Version
    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, jakarta.persistence.OptimisticLockException.class})
    public String handleOptimisticLockingFailure(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", 
            "Sản phẩm hoặc đơn hàng vừa được cập nhật bởi một thao tác khác. Vui lòng thử lại!");
        return "redirect:/cart";
    }

    // 2. Xử lý lỗi không tìm thấy tài nguyên
    @ExceptionHandler(ResourceNotFoundException.class)
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.NOT_FOUND)
    public String handleResourceNotFound(ResourceNotFoundException ex, Model model) {
        model.addAttribute("errorMessage", ex.getMessage());
        return "common/error";
    }

    // 3. Xử lý lỗi truy cập trái phép (IDOR / Sai Role)
    @ExceptionHandler(AccessDeniedException.class)
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.FORBIDDEN)
    public String handleAccessDenied(AccessDeniedException ex, Model model) {
        model.addAttribute("errorMessage", "Bạn không có quyền truy cập hoặc thao tác trên tài nguyên này!");
        return "common/error";
    }

    // 4. Bắt các lỗi Runtime chung
    @ExceptionHandler(Exception.class)
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneralException(Exception ex, Model model) {
        org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class).error("Unexpected application error", ex);
        model.addAttribute("errorMessage", "Đã xảy ra lỗi xử lý. Vui lòng thử lại hoặc liên hệ cửa hàng.");
        return "common/error";
    }
}
