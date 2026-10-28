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
    private final com.finalproject.repository.StoreInventoryRepository storeInventoryRepository;
    private final com.finalproject.service.UserService userService;
    private final com.finalproject.service.VoucherService voucherService;
    private final com.finalproject.config.MomoConfig momoConfig;
    private final com.finalproject.service.MomoService momoService;
    private final com.finalproject.service.DemoPaymentService demoPayments;
    private final com.finalproject.service.CustomerWalletService customerWallet;

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

        for (CartItem item : checkoutItems) {
            var product = item.getProduct();
            var store = item.getStore();
            if (item.getOrderType() != com.finalproject.enums.OrderType.BUY || store == null || !com.finalproject.service.ProductAvailability.supportsBranch(product, store)
                    || !com.finalproject.service.ProductAvailability.isPublic(product)
                    || !Boolean.TRUE.equals(store.getStatus()) || !Boolean.TRUE.equals(store.getIsOpen())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Sản phẩm " + product.getName()
                        + " đã ngừng bán. Vui lòng bỏ chọn sản phẩm này trong giỏ hàng.");
                return "redirect:/cart";
            }
            var stock = storeInventoryRepository.findByStoreIdAndProductId(store.getId(), product.getId()).orElse(null);
            if (stock == null || !stock.isAvailableForSale()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Sản phẩm " + product.getName()
                        + " đã ngừng bán tại chi nhánh. Vui lòng bỏ chọn sản phẩm này trong giỏ hàng.");
                return "redirect:/cart";
            }
            if (stock.getQuantity() == null || item.getQuantity() == null || stock.getQuantity() < item.getQuantity()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Sản phẩm " + product.getName()
                        + " không đủ tồn kho. Vui lòng điều chỉnh số lượng trong giỏ hàng.");
                return "redirect:/cart";
            }
        }

        // Kiểm tra tồn kho tại chi nhánh: Chặn thanh toán nếu có sản phẩm hết hàng
        // Verify stock for the selected fulfillment branch in quote and transactional checkout.
        BigDecimal subtotal = checkoutItems.stream().map(CartItem::getBuyAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal grandTotal = subtotal;
        User customer = userRepository.findByUsername(principal.getName()).orElse(null);

        model.addAttribute("customer", customer);
        model.addAttribute("addresses", customer != null ? userService.getUserAddresses(customer.getId()) : List.of());
        model.addAttribute("vouchers", voucherService.getAvailableVouchers(null));
        model.addAttribute("stores", storeRepository.findByStatusTrue().stream().filter(store -> Boolean.TRUE.equals(store.getIsOpen()))
                .filter(store -> checkoutItems.stream().allMatch(item -> com.finalproject.service.ProductAvailability.supportsBranch(item.getProduct(), store))).toList());
        model.addAttribute("deliveries", deliveryRepository.findAll().stream()
                .filter(d -> Boolean.TRUE.equals(d.getIsActive()) && !Boolean.TRUE.equals(d.getIsDeleted())).toList());
        model.addAttribute("checkoutItems", checkoutItems);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("grandTotal", grandTotal);
        model.addAttribute("paymentMethods", demoPayments.isEnabled()
                ? List.of(PaymentMethod.COD, PaymentMethod.MOMO_DEMO, PaymentMethod.WALLET_DEMO) : List.of(PaymentMethod.COD));
        model.addAttribute("customerWalletEnabled", customerWallet.isEnabled());
        model.addAttribute("walletBalance", customerWallet.balance(principal.getName()));
        model.addAttribute("momoMock", momoConfig.isMock());
        model.addAttribute("demoPaymentEnabled", demoPayments.isEnabled());

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
            @RequestParam(value = "deliveryZone", required = false) String deliveryZone,
            jakarta.servlet.http.HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        String finalRecipientName = extractFirstNonBlank(recipientName);
        String finalRecipientPhone = extractFirstNonBlank(recipientPhone);
        String finalRecipientAddress = extractFirstNonBlank(recipientAddress);

        try {
            if (paymentMethod == PaymentMethod.BANK_TRANSFER)
                throw new IllegalArgumentException("Thanh toán ngân hàng đã được gỡ. Vui lòng chọn COD hoặc MoMo.");
            finalRecipientName = resolveRecipientField(request, "recipientName", recipientName);
            finalRecipientPhone = resolveRecipientField(request, "recipientPhone", recipientPhone);
            finalRecipientAddress = resolveRecipientField(request, "recipientAddress", recipientAddress);
            if (selectedItemIds == null || selectedItemIds.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn sản phẩm để thanh toán.");
            if (finalRecipientName == null || finalRecipientName.isBlank())
                throw new IllegalArgumentException("Vui lòng nhập họ tên người nhận.");
            if (finalRecipientPhone == null || finalRecipientPhone.isBlank())
                throw new IllegalArgumentException("Vui lòng nhập số điện thoại người nhận.");
            if ("DELIVERY".equals(shippingMethod) && (finalRecipientAddress == null || finalRecipientAddress.isBlank()))
                throw new IllegalArgumentException("Vui lòng nhập địa chỉ giao hàng.");
            if (deliveryDate == null) throw new IllegalArgumentException("Vui lòng chọn ngày nhận hàng.");
            if (deliveryTime == null) {
                String slot = deliveryTimeSlot == null ? null : java.util.Arrays.stream(deliveryTimeSlot)
                        .filter(value -> value != null && !value.isBlank()).findFirst().orElse(null);
                if (slot == null || !java.util.Set.of("07:00-09:00", "09:00-11:00", "11:00-13:00", "13:00-15:00", "15:00-17:00", "17:00-20:00").contains(slot))
                    throw new IllegalArgumentException("Vui lòng chọn khung giờ nhận hoa hợp lệ.");
                deliveryTime = java.time.LocalTime.parse(slot.substring(0, 5));
            }
            validateDeliveryZone(shippingMethod, deliveryZone);
            if (paymentMethod == PaymentMethod.MOMO_DEMO && !demoPayments.isEnabled())
                throw new IllegalArgumentException("Thanh toán MoMo demo đang tắt.");
            List<com.finalproject.entity.Order> createdOrders = orderService.checkout(principal.getName(), selectedItemIds, paymentMethod, finalRecipientName, finalRecipientPhone, finalRecipientAddress, greetingMessage, voucherCode, deliveryDate, shippingMethod, pickupStoreId, null, deliveryTime, deliveryZone);
            if (paymentMethod == PaymentMethod.MOMO_DEMO) {
                try {
                    String token = demoPayments.create(createdOrders.stream().map(com.finalproject.entity.Order::getId).toList(), principal.getName());
                    return "redirect:/customer/demo-payment/" + token;
                } catch (IllegalArgumentException | IllegalStateException ex) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Đơn đã được lưu. Vui lòng mở lại thanh toán MoMo trong danh sách đơn hàng.");
                    return "redirect:/customer/orders";
                }
            }
            if (paymentMethod == PaymentMethod.MOMO && createdOrders != null && !createdOrders.isEmpty()) {
                com.finalproject.entity.Order firstOrder = createdOrders.get(0);
                try {
                    return "redirect:" + momoService.createPaymentUrl(firstOrder, request);
                } catch (IllegalArgumentException | IllegalStateException ex) {
                    redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage() + " Đơn đã được lưu để thanh toán lại.");
                    return "redirect:/customer/orders";
                }
            }
            redirectAttributes.addFlashAttribute("successMessage", "🌸 Đặt hoa thành công! Đơn hàng của bạn đã được chuyển đến chi nhánh cắm hoa để chuẩn bị.");
            return "redirect:/customer/orders";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            if (selectedItemIds != null && !selectedItemIds.isEmpty()) {
                redirectAttributes.addFlashAttribute("selectedItemIds", selectedItemIds);
                redirectAttributes.addFlashAttribute("prevShippingMethod", shippingMethod);
                redirectAttributes.addFlashAttribute("prevPaymentMethod", paymentMethod.name());
                redirectAttributes.addFlashAttribute("prevRecipientName", recipientName);
                redirectAttributes.addFlashAttribute("prevRecipientPhone", recipientPhone);
                redirectAttributes.addFlashAttribute("prevRecipientAddress", recipientAddress);
                redirectAttributes.addFlashAttribute("prevGreetingMessage", greetingMessage);
                redirectAttributes.addFlashAttribute("prevVoucherCode", voucherCode);
                redirectAttributes.addFlashAttribute("prevPickupStoreId", pickupStoreId);
                redirectAttributes.addFlashAttribute("prevDeliveryZone", deliveryZone);
                redirectAttributes.addFlashAttribute("prevDeliveryDate", deliveryDate);
                redirectAttributes.addFlashAttribute("prevDeliveryTimeSlot", deliveryTimeSlot == null ? null : java.util.Arrays.stream(deliveryTimeSlot)
                        .filter(value -> value != null && !value.isBlank()).findFirst().orElse(null));
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
            @RequestParam(value = "deliveryUnitId", required = false) Long deliveryUnitId,
            @RequestParam(value = "deliveryZone", required = false) String deliveryZone) {
        try {
            validateDeliveryZone(shippingMethod, deliveryZone);
            return org.springframework.http.ResponseEntity.ok(orderService.quote(principal.getName(), selectedItemIds,
                    recipientAddress, voucherCode, shippingMethod, pickupStoreId, null, deliveryZone));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("message", ex.getMessage()));
        }
    }

    private void validateDeliveryZone(String shippingMethod, String deliveryZone) {
        if ("DELIVERY".equals(shippingMethod)
                && !java.util.Set.of("NEAR", "FAR", "OUTSIDE").contains(deliveryZone == null ? "" : deliveryZone)) {
            throw new IllegalArgumentException("Vui lòng chọn vùng giao hàng demo.");
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
