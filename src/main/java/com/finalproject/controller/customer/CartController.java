package com.finalproject.controller.customer;

import com.finalproject.dto.request.AddToCartRequest;
import com.finalproject.entity.Cart;
import com.finalproject.enums.PaymentMethod;
import com.finalproject.service.CartService;
import com.finalproject.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final OrderService orderService;

    @GetMapping
    public String showCart(Principal principal, Model model) {
        Cart cart = cartService.getCartByUsername(principal.getName());
        model.addAttribute("cart", cart);
        model.addAttribute("paymentMethods", PaymentMethod.values());
        return "customer/cart";
    }

    @PostMapping("/add")
    public String handleAddToCart(Principal principal,
                                  @ModelAttribute AddToCartRequest request,
                                  RedirectAttributes redirectAttributes) {
        try {
            cartService.addToCart(principal.getName(), request);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm sản phẩm vào giỏ hàng thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/";
        }
        return "redirect:/cart";
    }

    @PostMapping("/update-quantity/{id}")
    public String handleUpdateQuantity(Principal principal,
                                       @PathVariable("id") Long id,
                                       @RequestParam("quantity") Integer quantity,
                                       RedirectAttributes redirectAttributes) {
        try {
            cartService.updateQuantity(principal.getName(), id, quantity);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật số lượng thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/remove/{id}")
    public String handleRemoveCartItem(Principal principal, @PathVariable("id") Long id) {
        cartService.removeCartItem(principal.getName(), id);
        return "redirect:/cart";
    }

    @PostMapping("/checkout")
    public String handleCheckout(Principal principal,
                                 @RequestParam("paymentMethod") PaymentMethod paymentMethod,
                                 RedirectAttributes redirectAttributes) {
        try {
            orderService.checkout(principal.getName(), paymentMethod);
            redirectAttributes.addFlashAttribute("successMessage", "Đặt hàng thành công! Đơn hàng của bạn đang chờ xác nhận.");
            return "redirect:/customer/orders";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/cart";
        }
    }
}