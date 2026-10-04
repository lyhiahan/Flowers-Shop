package com.finalproject.controller.customer;

import com.finalproject.dto.request.AddToCartRequest;
import com.finalproject.entity.Cart;
import com.finalproject.enums.PaymentMethod;
import com.finalproject.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final com.finalproject.repository.StoreInventoryRepository storeInventoryRepository;

    @GetMapping
    public String showCart(Principal principal, Model model) {
        Cart cart = cartService.getCartByUsername(principal.getName());
        model.addAttribute("cart", cart);

        // Nhóm các sản phẩm theo chi nhánh/cửa hàng (UC_012 Step 8) và tính tồn kho thực tế
        java.util.Map<com.finalproject.entity.Store, java.util.List<com.finalproject.entity.CartItem>> itemsByStore = new java.util.LinkedHashMap<>();
        java.util.Map<Long, Integer> itemStockMap = new java.util.HashMap<>();

        int totalQuantity = 0;
        if (cart != null && cart.getItems() != null) {
            for (com.finalproject.entity.CartItem item : cart.getItems()) {
                totalQuantity += (item.getQuantity() != null ? item.getQuantity() : 0);
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
        model.addAttribute("stockByItem", itemStockMap);
        model.addAttribute("totalQuantity", totalQuantity);
        model.addAttribute("paymentMethods", PaymentMethod.values());
        return "customer/cart";
    }

    /**
     * AJAX endpoint: Được gọi khi frontend gửi header X-Requested-With=XMLHttpRequest.
     * Trả về JSON {success, count} hoặc {success, error} — không redirect, không tải lại trang.
     */
    @PostMapping(value = "/add", headers = "X-Requested-With=XMLHttpRequest")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleAddToCartAjax(
            Principal principal,
            @ModelAttribute AddToCartRequest request) {
        Map<String, Object> body = new HashMap<>();
        try {
            // addToCart trả về tổng số lượng mới — không cần query thêm
            int count = cartService.addToCart(principal.getName(), request);
            body.put("success", true);
            body.put("count", count);
            return ResponseEntity.ok(body);
        } catch (Exception ex) {
            body.put("success", false);
            body.put("error", ex.getMessage());
            return ResponseEntity.badRequest().body(body);
        }
    }

    /**
     * Form-submit fallback (không có header X-Requested-With): Redirect như cũ.
     * Dùng cho trình duyệt tắt JS hoặc gửi form trực tiếp.
     */
    @PostMapping("/add")
    public String handleAddToCart(Principal principal,
                                  @ModelAttribute AddToCartRequest request,
                                  jakarta.servlet.http.HttpServletRequest httpRequest,
                                  RedirectAttributes redirectAttributes) {
        String referer = httpRequest.getHeader("Referer");
        try {
            cartService.addToCart(principal.getName(), request);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            if (com.finalproject.utils.SafeRedirect.isWithin(referer, "/products", "/stores")) {
                return "redirect:" + referer;
            }
            return "redirect:/";
        }
        if (com.finalproject.utils.SafeRedirect.isWithin(referer, "/products", "/stores")) {
            return "redirect:" + referer;
        }
        return "redirect:/cart";
    }

    @PostMapping(value = "/update-quantity/{id}", headers = "X-Requested-With=XMLHttpRequest")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleUpdateQuantityAjax(Principal principal,
                                                                        @PathVariable("id") Long id,
                                                                        @RequestParam("quantity") Integer quantity) {
        try {
            if (quantity == null || quantity < 1) {
                throw new IllegalArgumentException("Số lượng tối thiểu là 1. Để bỏ sản phẩm, hãy dùng nút Xóa.");
            }
            cartService.updateQuantity(principal.getName(), id, quantity);
            return ResponseEntity.ok(buildCartState(principal.getName(), true));
        } catch (Exception ex) {
            Map<String, Object> body = new HashMap<>();
            body.put("success", false);
            body.put("error", ex.getMessage());
            return ResponseEntity.badRequest().body(body);
        }
    }

    /** Trả trạng thái giỏ đã lưu để đồng bộ lại khi không chắc yêu cầu trước đã lưu hay chưa. */
    @GetMapping(value = "/state", headers = "X-Requested-With=XMLHttpRequest")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cartState(Principal principal) {
        return ResponseEntity.ok(buildCartState(principal.getName(), false));
    }

    private Map<String, Object> buildCartState(String username, boolean success) {
        Cart cart = cartService.getCartByUsername(username);
        java.util.List<Map<String, Object>> items = new java.util.ArrayList<>();
        int totalQty = 0;
        if (cart != null && cart.getItems() != null) {
            for (com.finalproject.entity.CartItem it : cart.getItems()) {
                int q = it.getQuantity() != null ? it.getQuantity() : 0;
                totalQty += q;
                int stock = (it.getStore() != null && it.getProduct() != null)
                        ? storeInventoryRepository.findByStoreIdAndProductId(it.getStore().getId(), it.getProduct().getId())
                                .map(com.finalproject.entity.StoreInventory::getQuantity).orElse(0)
                        : 0;
                Map<String, Object> m = new HashMap<>();
                m.put("id", it.getId());
                m.put("quantity", q);
                m.put("stock", stock);
                m.put("subtotal", it.getOrderType() == com.finalproject.enums.OrderType.BUY ? it.getBuyAmount() : it.getRentAmount());
                m.put("deposit", it.getDepositAmount());
                m.put("total", it.getTotalAmount());
                items.add(m);
            }
        }
        Map<String, Object> body = new HashMap<>();
        body.put("success", success);
        body.put("items", items);
        body.put("rowCount", items.size());
        body.put("count", totalQty);
        return body;
    }

    @PostMapping("/update-quantity/{id}")
    public String handleUpdateQuantity(Principal principal,
                                       @PathVariable("id") Long id,
                                       @RequestParam("quantity") Integer quantity,
                                       RedirectAttributes redirectAttributes) {
        try {
            cartService.updateQuantity(principal.getName(), id, quantity);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/remove/{id}")
    public String handleRemoveCartItem(Principal principal,
                                       @PathVariable("id") Long id,
                                       RedirectAttributes redirectAttributes) {
        try {
            cartService.removeCartItem(principal.getName(), id);
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
            // Keep the legacy entry point, but collect and validate checkout details centrally.
            return "redirect:/checkout";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/cart";
        }
    }
}
