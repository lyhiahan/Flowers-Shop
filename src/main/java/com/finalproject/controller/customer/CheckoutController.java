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
    private final com.finalproject.config.MomoConfig momoConfig;
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

        List<Long> effectiveItemIds = selectedItemIds;
        if ((effectiveItemIds == null || effectiveItemIds.isEmpty()) && model.containsAttribute("selectedItemIds")) {
            Object obj = model.asMap().get("selectedItemIds");
            if (obj instanceof List<?>) {
                effectiveItemIds = ((List<?>) obj).stream()
                        .filter(Long.class::isInstance)
                        .map(Long.class::cast)
                        .toList();
            }
        }
        final List<Long> finalSelectedItemIds = effectiveItemIds;

        List<CartItem> checkoutItems;
        if (finalSelectedItemIds != null && !finalSelectedItemIds.isEmpty()) {
            checkoutItems = allItems.stream()
                    .filter(item -> finalSelectedItemIds.contains(item.getId()))
                    .collect(Collectors.toList());
            if (checkoutItems.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn ít nhất 1 sản phẩm để thanh toán!");
                return "redirect:/cart";
            }
        } else {
            checkoutItems = new ArrayList<>(allItems);
        }

        // Kiểm tra tồn kho tại chi nhánh: Chặn thanh toán nếu có sản phẩm hết hàng
        // Verify stock for the selected fulfillment branch in quote and transactional checkout.
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
        model.addAttribute("stores", storeRepository.findByStatusTrue().stream().filter(store -> Boolean.TRUE.equals(store.getIsOpen()))
                .filter(store -> checkoutItems.stream().allMatch(item -> com.finalproject.service.ProductAvailability.supportsBranch(item.getProduct(), store))).toList());
        model.addAttribute("deliveries", deliveryRepository.findAll().stream()
                .filter(d -> Boolean.TRUE.equals(d.getIsActive()) && !Boolean.TRUE.equals(d.getIsDeleted())).toList());
        model.addAttribute("checkoutItems", checkoutItems);
        model.addAttribute("subtotalRentAndBuy", subtotalRentAndBuy);
        model.addAttribute("totalDeposit", totalDeposit);
        model.addAttribute("grandTotal", grandTotal);
        model.addAttribute("paymentMethods", PaymentMethod.values());
        model.addAttribute("momoMock", momoConfig.isMock());

        return "customer/checkout";
    }

    @PostMapping
    public String handleCheckout(
            Principal principal,
            @RequestParam("selectedItemIds") List<Long> selectedItemIds,
            @RequestParam("paymentMethod") PaymentMethod paymentMethod,
            @RequestParam(value = "recipientName", required = false) String recipientName,
            @RequestParam(value = "recipientPhone", required = false) String recipientPhone,
            @RequestParam(value = "recipientAddress", required = false) String recipientAddress,
            @RequestParam(value = "greetingMessage", required = false) String greetingMessage,
            @RequestParam(value = "voucherCode", required = false) String voucherCode,
            @RequestParam(value = "deliveryDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate,
            @RequestParam(value = "deliveryTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) java.time.LocalTime deliveryTime,
            @RequestParam(value = "deliveryTimeSlot", required = false) String[] deliveryTimeSlot,
            @RequestParam(value = "shippingMethod", required = false, defaultValue = "DELIVERY") String shippingMethod,
            @RequestParam(value = "pickupStoreId", required = false) Long pickupStoreId,
            @RequestParam(value = "deliveryUnitId", required = false) Long deliveryUnitId,
            jakarta.servlet.http.HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        String finalRecipientName = extractFirstNonBlank(recipientName);
        String finalRecipientPhone = extractFirstNonBlank(recipientPhone);
        String finalRecipientAddress = extractFirstNonBlank(recipientAddress);

        try {
            finalRecipientName = resolveRecipientField(request, "recipientName", recipientName);
            finalRecipientPhone = resolveRecipientField(request, "recipientPhone", recipientPhone);
            finalRecipientAddress = resolveRecipientField(request, "recipientAddress", recipientAddress);
            if (selectedItemIds == null || selectedItemIds.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn sản phẩm để thanh toán.");
            if (deliveryDate == null) throw new IllegalArgumentException("Vui lòng chọn ngày nhận hàng.");
            if (deliveryTime == null) {
                String slot = deliveryTimeSlot == null ? null : java.util.Arrays.stream(deliveryTimeSlot)
                        .filter(value -> value != null && !value.isBlank()).findFirst().orElse(null);
                if (slot == null || !java.util.Set.of("07:00-09:00", "09:00-11:00", "11:00-13:00", "13:00-15:00", "15:00-17:00", "17:00-20:00").contains(slot))
                    throw new IllegalArgumentException("Vui lòng chọn khung giờ nhận hoa hợp lệ.");
                deliveryTime = java.time.LocalTime.parse(slot.substring(0, 5));
            }
            if (finalRecipientName == null || finalRecipientName.isBlank() || finalRecipientPhone == null || finalRecipientPhone.isBlank())
                throw new IllegalArgumentException("Vui lòng nhập tên và số điện thoại người nhận.");
            if ("DELIVERY".equals(shippingMethod) && (finalRecipientAddress == null || finalRecipientAddress.isBlank()))
                throw new IllegalArgumentException("Vui lòng nhập địa chỉ giao hàng.");
            validateDeliverySelection(shippingMethod, deliveryUnitId);
            List<com.finalproject.entity.Order> createdOrders = orderService.checkout(principal.getName(), selectedItemIds, paymentMethod, finalRecipientName, finalRecipientPhone, finalRecipientAddress, greetingMessage, voucherCode, deliveryDate, shippingMethod, pickupStoreId, deliveryUnitId, deliveryTime);
            if (paymentMethod == PaymentMethod.MOMO && createdOrders != null && !createdOrders.isEmpty()) {
                com.finalproject.entity.Order firstOrder = createdOrders.get(0);
                try {
                    return "redirect:" + momoService.createPaymentUrl(firstOrder, request);
                } catch (IllegalArgumentException | IllegalStateException ex) {
                    redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage() + " Đơn đã được lưu để thanh toán lại.");
                    return "redirect:/customer/orders";
                }
            }
            if (paymentMethod == PaymentMethod.BANK_TRANSFER && createdOrders != null && !createdOrders.isEmpty()) {
                return "redirect:/checkout/bank/" + createdOrders.get(0).getId();
            }
            redirectAttributes.addFlashAttribute("successMessage", "🌸 Đặt hoa thành công! Đơn hàng của bạn đã được chuyển đến chi nhánh cắm hoa để chuẩn bị.");
            return "redirect:/customer/orders";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            if (selectedItemIds != null && !selectedItemIds.isEmpty()) {
                redirectAttributes.addFlashAttribute("selectedItemIds", selectedItemIds);
                redirectAttributes.addFlashAttribute("prevShippingMethod", shippingMethod);
                redirectAttributes.addFlashAttribute("prevRecipientName", finalRecipientName);
                redirectAttributes.addFlashAttribute("prevRecipientPhone", finalRecipientPhone);
                redirectAttributes.addFlashAttribute("prevRecipientAddress", finalRecipientAddress);
                redirectAttributes.addFlashAttribute("prevGreetingMessage", greetingMessage);
                redirectAttributes.addFlashAttribute("prevVoucherCode", voucherCode);
                redirectAttributes.addFlashAttribute("prevPickupStoreId", pickupStoreId);
                redirectAttributes.addFlashAttribute("prevDeliveryUnitId", deliveryUnitId);
                String query = selectedItemIds.stream().map(id -> "selectedItemIds=" + id).collect(Collectors.joining("&"));
                return "redirect:/checkout?" + query;
            }
            return "redirect:/cart";
        }
    }

    @PostMapping("/quote")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> quote(Principal principal,
            @RequestParam(value = "selectedItemIds", required = false) List<Long> selectedItemIds,
            @RequestParam(value = "recipientAddress", required = false) String recipientAddress,
            @RequestParam(value = "voucherCode", required = false) String voucherCode,
            @RequestParam(value = "shippingMethod", defaultValue = "DELIVERY") String shippingMethod,
            @RequestParam(value = "pickupStoreId", required = false) Long pickupStoreId,
            @RequestParam(value = "deliveryUnitId", required = false) Long deliveryUnitId) {
        try {
            validateDeliverySelection(shippingMethod, deliveryUnitId);
            return org.springframework.http.ResponseEntity.ok(orderService.quote(principal.getName(), selectedItemIds,
                    recipientAddress, voucherCode, shippingMethod, pickupStoreId, deliveryUnitId));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("message", ex.getMessage()));
        }
    }

    private void validateDeliverySelection(String shippingMethod, Long deliveryUnitId) {
        if ("DELIVERY".equals(shippingMethod) && deliveryUnitId == null && deliveryRepository.findAll().stream()
                .anyMatch(d -> Boolean.TRUE.equals(d.getIsActive()) && !Boolean.TRUE.equals(d.getIsDeleted()))) {
            throw new IllegalArgumentException("Vui lòng chọn đơn vị giao hàng.");
        }
    }

    private String extractFirstNonBlank(String input) {
        return input == null ? null : input.trim();
    }

    private String resolveRecipientField(jakarta.servlet.http.HttpServletRequest request, String field, String fallback) {
        String[] submitted = request.getParameterValues(field);
        if (submitted == null) return extractFirstNonBlank(fallback);
        var values = java.util.Arrays.stream(submitted).filter(value -> value != null && !value.isBlank())
                .map(String::trim).distinct().toList();
        if (values.size() > 1) throw new IllegalArgumentException("Thông tin người nhận bị gửi nhiều giá trị khác nhau. Vui lòng kiểm tra lại.");
        return values.isEmpty() ? null : values.get(0);
    }
}
