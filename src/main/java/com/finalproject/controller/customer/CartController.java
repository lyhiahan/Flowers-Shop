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
    private final com.finalproject.repository.StoreInventoryRepository storeInventoryRepository;

    @GetMapping
    public String showCart(Principal principal, Model model) {
        Cart cart = cartService.getCartByUsername(principal.getName());
        model.addAttribute("cart", cart);

        // Nhóm các sản phẩm theo chi nhánh/cửa hàng (UC_012 Step 8) và tính tồn kho thực tế
        java.util.Map<com.finalproject.entity.Store, java.util.List<com.finalproject.entity.CartItem>> itemsByStore = new java.util.LinkedHashMap<>();
        java.util.Map<Long, Integer> itemStockMap = new java.util.HashMap<>();

        if (cart != null && cart.getItems() != null) {
            for (com.finalproject.entity.CartItem item : cart.getItems()) {
                com.finalproject.entity.Store store = item.getStore();
                itemsByStore.computeIfAbsent(store, k -> new java.util.ArrayList<>()).add(item);

                int stock = 0;
                if (store != null && item.getProduct() != null) {
                    stock = storeInventoryRepository.findByStoreIdAndProductId(store.getId(), item.getProduct().getId())
                            .map(com.finalproject.entity.StoreInventory::getQuantity)
                            .orElse(0);
                }
                itemStockMap.put(item.getId(), stock);
            }
        }
        model.addAttribute("itemsByStore", itemsByStore);
        model.addAttribute("itemStockMap", itemStockMap);
        model.addAttribute("paymentMethods", PaymentMethod.values());
        return "customer/cart";
    }

    @PostMapping("/add")
    public String handleAddToCart(Principal principal,
                                  @ModelAttribute AddToCartRequest request,
                                  jakarta.servlet.http.HttpServletRequest httpRequest,
                                  RedirectAttributes redirectAttributes) {
        String referer = httpRequest.getHeader("Referer");
        try {
            cartService.addToCart(principal.getName(), request);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm sản phẩm vào giỏ hàng thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            if (referer != null && !referer.isEmpty() && !referer.contains("/cart")) {
                return "redirect:" + referer;
            }
            return "redirect:/";
        }
        if (referer != null && !referer.isEmpty() && !referer.contains("/cart")) {
            return "redirect:" + referer;
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

    @RequestMapping(value = "/remove/{id}", method = {RequestMethod.POST, RequestMethod.GET})
    public String handleRemoveCartItem(Principal principal,
                                       @PathVariable("id") Long id,
                                       RedirectAttributes redirectAttributes) {
        try {
            cartService.removeCartItem(principal.getName(), id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa sản phẩm khỏi giỏ hàng thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Xóa sản phẩm thất bại: " + ex.getMessage());
        }
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