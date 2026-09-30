package com.finalproject.controller.auth;

import com.finalproject.dto.request.RegisterRequest;
import com.finalproject.enums.OtpType;
import com.finalproject.repository.OtpVerificationRepository;
import com.finalproject.repository.UserRepository;
import com.finalproject.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final OtpVerificationRepository otpRepository;

    @GetMapping("/login")
    public String showLoginForm(@RequestParam(value = "error", required = false) String error,
                                @RequestParam(value = "logout", required = false) String logout,
                                @RequestParam(value = "activated", required = false) String activated,
                                Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Đăng nhập thất bại: Email, số điện thoại hoặc mật khẩu không chính xác!");
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
            model.addAttribute("errorMessage", "Thông báo đăng ký thất bại: Vui lòng kiểm tra và hoàn thiện các trường thông tin bị thiếu hoặc không đúng định dạng!");
            return "auth/register";
        }

        try {
            String otpCode = authService.registerCustomer(request);
            redirectAttributes.addFlashAttribute("successMessage", "Thông báo đăng ký thành công! Vui lòng nhập mã OTP đã gửi đến email của bạn để kích hoạt tài khoản.");
            redirectAttributes.addFlashAttribute("devOtpCode", otpCode);
            return "redirect:/verify-otp?email=" + request.getEmail();
        } catch (IllegalArgumentException ex) {
            String msg = ex.getMessage();
            model.addAttribute("errorMessage", "Thông báo đăng ký thất bại: " + msg);
            if (msg.contains("Tên đăng nhập") || msg.contains("tên đăng nhập")) {
                bindingResult.rejectValue("username", "duplicate", msg);
            } else if (msg.contains("Email") || msg.contains("email")) {
                bindingResult.rejectValue("email", "duplicate", msg);
            } else if (msg.contains("Số điện thoại") || msg.contains("số điện thoại")) {
                bindingResult.rejectValue("phone", "duplicate", msg);
            }
            return "auth/register";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Thông báo đăng ký thất bại: Đã xảy ra lỗi khi đăng ký (" + ex.getMessage() + ")");
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
            String newCode = authService.resendOtp(email);
            redirectAttributes.addFlashAttribute("successMessage", "Mã OTP mới đã được gửi đến email của bạn!");
            redirectAttributes.addFlashAttribute("devOtpCode", newCode);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/verify-otp?email=" + email;
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm(@RequestParam(value = "email", required = false) String email,
                                         @RequestParam(value = "sent", required = false) String sent,
                                         Model model) {
        if (email != null && !email.trim().isEmpty() && !model.containsAttribute("email")) {
            model.addAttribute("email", email.trim());
        }
        if (sent != null && !model.containsAttribute("emailSent")) {
            model.addAttribute("emailSent", true);
        }

        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(@RequestParam("email") String email,
                                       RedirectAttributes redirectAttributes,
                                       Model model) {
        try {
            authService.forgotPassword(email);
            redirectAttributes.addFlashAttribute("emailSent", true);
            redirectAttributes.addFlashAttribute("email", email.trim());
            redirectAttributes.addFlashAttribute("successMessage", "Hệ thống đã xác thực email thành công! Đã gửi email yêu cầu tạo mật khẩu mới về email người dùng.");
            return "redirect:/forgot-password?sent=true&email=" + java.net.URLEncoder.encode(email.trim(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception ex) {
            model.addAttribute("email", email);
            model.addAttribute("errorMessage", ex.getMessage() + " (Mật khẩu mới không được tạo)");
            return "auth/forgot-password";
        }
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam(value = "email", required = false) String email,
                                        @RequestParam(value = "otpCode", required = false) String otpCode,
                                        Model model) {
        model.addAttribute("email", email);
        if (otpCode != null && !otpCode.trim().isEmpty()) {
            model.addAttribute("otpCode", otpCode.trim());
        }
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String handleResetPassword(@RequestParam("email") String email,
                                      @RequestParam(value = "otpCode", required = false) String otpCode,
                                      @RequestParam("newPassword") String newPassword,
                                      @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
                                      Model model) {
        model.addAttribute("email", email);
        model.addAttribute("otpCode", otpCode);

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("errorMessage", "Mật khẩu xác nhận không khớp với mật khẩu mới!");
            return "auth/reset-password";
        }

        try {
            authService.resetPassword(email, otpCode, newPassword);
            model.addAttribute("isSuccess", true);
            model.addAttribute("successMessage", "Thay đổi thành công! Mật khẩu mới đã được cập nhật vào hệ thống.");
            return "auth/reset-password";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", ex.getMessage() + " (Mật khẩu mới không được tạo)");
            return "auth/reset-password";
        }
    }
}
