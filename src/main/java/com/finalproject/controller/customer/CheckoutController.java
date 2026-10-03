package com.finalproject.controller.customer;

import com.finalproject.entity.Cart;
import com.finalproject.entity.CartItem;
import com.finalproject.entity.User;
import com.finalproject.enums.PaymentMethod;
import com.finalproject.repository.UserRepository;
import com.finalproject.service.CartService;
import com.finalproject.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.format.annotation.DateTimeFormat;

@Controller
@RequestMapping("/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CartService cartService;
    private final OrderService orderService;
    private final UserRepository userRepository;
    private final com.finalproject.repository.StoreRepository storeRepository;
    private final com.finalproject.repository.DeliveryRepository deliveryRepository;
    private final com.finalproject.service.UserService userService;
    private final com.finalproject.service.VoucherService voucherService;
    private final com.finalproject.service.MomoService momoService;

    @GetMapping
    public String showCheckoutPage(
            Principal principal,
            @RequestParam(value = "selectedItemIds", required = false) List<Long> selectedItemIds,
            Model model,
            RedirectAttributes redirectAttributes) {

        Cart cart = cartService.getCartByUsername(principal.getName());
        List<CartItem> allItems = cart.getItems();

        if (allItems == null || allItems.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Giỏ hàng của bạn đang trống!");
            return "redirect:/cart";
        }

        List<CartItem> checkoutItems;
        if (selectedItemIds != null && !selectedItemIds.isEmpty()) {
            checkoutItems = allItems.stream()
                    .filter(item -> selectedItemIds.contains(item.getId()))
                    .collect(Collectors.toList());
            if (checkoutItems.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn ít nhất 1 sản phẩm để thanh toán!");
                return "redirect:/cart";
            }
        } else {
            checkoutItems = new ArrayList<>(allItems);
        }

        BigDecimal subtotalRentAndBuy = BigDecimal.ZERO;
        BigDecimal totalDeposit = BigDecimal.ZERO;

        for (CartItem item : checkoutItems) {
            if (item.getOrderType() == com.finalproject.enums.OrderType.BUY) {
                subtotalRentAndBuy = subtotalRentAndBuy.add(item.getBuyAmount());
            } else {
                subtotalRentAndBuy = subtotalRentAndBuy.add(item.getRentAmount());
                totalDeposit = totalDeposit.add(item.getDepositAmount());
            }
        }

        BigDecimal grandTotal = subtotalRentAndBuy.add(totalDeposit);
        User customer = userRepository.findByUsername(principal.getName()).orElse(null);

        model.addAttribute("customer", customer);
        model.addAttribute("addresses", customer != null ? userService.getUserAddresses(customer.getId()) : List.of());
        model.addAttribute("vouchers", voucherService.getAvailableVouchers(null));
        model.addAttribute("stores", storeRepository.findByStatusTrue());
        model.addAttribute("deliveries", deliveryRepository.findAll());
        model.addAttribute("checkoutItems", checkoutItems);
        model.addAttribute("subtotalRentAndBuy", subtotalRentAndBuy);
        model.addAttribute("totalDeposit", totalDeposit);
        model.addAttribute("grandTotal", grandTotal);
        model.addAttribute("paymentMethods", PaymentMethod.values());

        return "customer/checkout";
    }

    @PostMapping
    public String handleCheckout(
            Principal principal,
            @RequestParam(value = "selectedItemIds", required = false) List<Long> selectedItemIds,
            @RequestParam("paymentMethod") PaymentMethod paymentMethod,
            @RequestParam(value = "recipientName", required = false) String recipientName,
            @RequestParam(value = "recipientPhone", required = false) String recipientPhone,
            @RequestParam(value = "recipientAddress", required = false) String recipientAddress,
            @RequestParam(value = "greetingMessage", required = false) String greetingMessage,
            @RequestParam(value = "voucherCode", required = false) String voucherCode,
            @RequestParam(value = "deliveryDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate,
            @RequestParam(value = "shippingMethod", required = false, defaultValue = "DELIVERY") String shippingMethod,
            @RequestParam(value = "pickupStoreId", required = false) Long pickupStoreId,
            jakarta.servlet.http.HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        try {
            List<com.finalproject.entity.Order> createdOrders = orderService.checkout(principal.getName(), selectedItemIds, paymentMethod, recipientName, recipientPhone, recipientAddress, greetingMessage, voucherCode, deliveryDate, shippingMethod, pickupStoreId);
            if (paymentMethod == PaymentMethod.MOMO && createdOrders != null && !createdOrders.isEmpty()) {
                com.finalproject.entity.Order firstOrder = createdOrders.get(0);
                String momoUrl = momoService.createPaymentUrl(firstOrder, request);
                return "redirect:" + momoUrl;
            }
            redirectAttributes.addFlashAttribute("successMessage", "🌸 Đặt hoa thành công! Đơn hàng của bạn đã được chuyển đến chi nhánh cắm hoa để chuẩn bị.");
            return "redirect:/customer/orders";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/cart";
        }
    }
}
