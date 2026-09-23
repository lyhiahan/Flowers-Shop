package com.finalproject.controller.auth;

import com.finalproject.dto.request.RegisterRequest;
import com.finalproject.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/login")
    public String showLoginForm(@RequestParam(value = "error", required = false) String error,
                                @RequestParam(value = "logout", required = false) String logout,
                                @RequestParam(value = "activated", required = false) String activated,
                                Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Tên đăng nhập hoặc mật khẩu không chính xác, hoặc tài khoản chưa kích hoạt!");
        }
        if (logout != null) {
            model.addAttribute("successMessage", "Bạn đã đăng xuất thành công.");
        }
        if (activated != null) {
            model.addAttribute("successMessage", "Kích hoạt tài khoản thành công! Bạn có thể đăng nhập ngay.");
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new RegisterRequest());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String handleRegister(@Valid @ModelAttribute("registerRequest") RegisterRequest request,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            authService.registerCustomer(request);
            redirectAttributes.addFlashAttribute("successMessage", "Đăng ký thành công! Vui lòng nhập mã OTP đã được gửi đến email của bạn để kích hoạt tài khoản.");
            return "redirect:/verify-otp?email=" + request.getEmail();
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/register";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Đã xảy ra lỗi khi đăng ký: " + ex.getMessage());
            return "auth/register";
        }
    }

    @GetMapping("/verify-otp")
    public String showVerifyOtpForm(@RequestParam("email") String email, Model model) {
        model.addAttribute("email", email);
        return "auth/verify-otp";
    }

    @PostMapping("/verify-otp")
    public String handleVerifyOtp(@RequestParam("email") String email,
                                  @RequestParam("otpCode") String otpCode,
                                  RedirectAttributes redirectAttributes,
                                  Model model) {
        try {
            boolean verified = authService.verifyOtp(email, otpCode);
            if (verified) {
                redirectAttributes.addFlashAttribute("successMessage", "Kích hoạt tài khoản thành công! Bạn có thể đăng nhập.");
                return "redirect:/login?activated=true";
            } else {
                model.addAttribute("email", email);
                model.addAttribute("errorMessage", "Mã OTP không hợp lệ hoặc đã hết hạn!");
                return "auth/verify-otp";
            }
        } catch (Exception ex) {
            model.addAttribute("email", email);
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/verify-otp";
        }
    }

    @PostMapping("/resend-otp")
    public String handleResendOtp(@RequestParam("email") String email,
                                  RedirectAttributes redirectAttributes) {
        try {
            authService.resendOtp(email);
            redirectAttributes.addFlashAttribute("successMessage", "Mã OTP mới đã được gửi đến email của bạn!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/verify-otp?email=" + email;
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(@RequestParam("email") String email,
                                       RedirectAttributes redirectAttributes,
                                       Model model) {
        try {
            authService.forgotPassword(email);
            redirectAttributes.addFlashAttribute("successMessage", "Mã OTP đặt lại mật khẩu đã được gửi đến email của bạn!");
            return "redirect:/reset-password?email=" + email;
        } catch (Exception ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/forgot-password";
        }
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam("email") String email, Model model) {
        model.addAttribute("email", email);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String handleResetPassword(@RequestParam("email") String email,
                                      @RequestParam("otpCode") String otpCode,
                                      @RequestParam("newPassword") String newPassword,
                                      RedirectAttributes redirectAttributes,
                                      Model model) {
        try {
            authService.resetPassword(email, otpCode, newPassword);
            redirectAttributes.addFlashAttribute("successMessage", "Đặt lại mật khẩu thành công! Vui lòng đăng nhập với mật khẩu mới.");
            return "redirect:/login";
        } catch (Exception ex) {
            model.addAttribute("email", email);
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/reset-password";
        }
    }
}
