package com.finalproject;

import com.finalproject.controller.customer.CheckoutController;
import com.finalproject.enums.PaymentMethod;
import com.finalproject.service.CartService;
import com.finalproject.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.lang.reflect.Method;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CheckoutPhoneValidationTests {

    @Test
    @DisplayName("Kiểm tra logic làm sạch và regex số điện thoại trong OrderService")
    void testPhoneSanitizationAndValidationLogic() throws Exception {
        Class<?> orderServiceImplClass = Class.forName("com.finalproject.service.OrderService");
        Object instance = mock(com.finalproject.service.OrderService.class);

        Method cleanPhoneMethod = orderServiceImplClass.getDeclaredMethod("cleanPhone", String.class);
        cleanPhoneMethod.setAccessible(true);

        Method isValidPhoneMethod = orderServiceImplClass.getDeclaredMethod("isValidPhoneNumber", String.class);
        isValidPhoneMethod.setAccessible(true);

        // Case 1: Số chuẩn 10 chữ số
        String p1 = (String) cleanPhoneMethod.invoke(instance, "0933516123");
        assertEquals("0933516123", p1);
        assertTrue((Boolean) isValidPhoneMethod.invoke(instance, p1));

        // Case 2: Số bị lặp do form gửi trùng cả 2 section (#sectionDelivery và #sectionPickup)
        String p2 = (String) cleanPhoneMethod.invoke(instance, "0933516123,0933516123");
        assertEquals("0933516123", p2);
        assertTrue((Boolean) isValidPhoneMethod.invoke(instance, p2));

        // Case 3: Số có dấu phẩy rỗng đằng sau
        String p3 = (String) cleanPhoneMethod.invoke(instance, "0933516123,");
        assertEquals("0933516123", p3);
        assertTrue((Boolean) isValidPhoneMethod.invoke(instance, p3));

        // Case 4: Số có khoảng trắng hoặc dấu gạch nối, dấu chấm
        String p4 = (String) cleanPhoneMethod.invoke(instance, "0933 516 123");
        assertEquals("0933516123", p4);
        assertTrue((Boolean) isValidPhoneMethod.invoke(instance, p4));

        String p5 = (String) cleanPhoneMethod.invoke(instance, "0933-516-123");
        assertEquals("0933516123", p5);
        assertTrue((Boolean) isValidPhoneMethod.invoke(instance, p5));

        // Case 5: Số có tiền tố quốc tế +84 hoặc 84
        String p6 = (String) cleanPhoneMethod.invoke(instance, "+84933516123");
        assertEquals("0933516123", p6);
        assertTrue((Boolean) isValidPhoneMethod.invoke(instance, p6));

        String p7 = (String) cleanPhoneMethod.invoke(instance, "84933516123");
        assertEquals("0933516123", p7);
        assertTrue((Boolean) isValidPhoneMethod.invoke(instance, p7));

        // Case 6: Số máy bàn 028...
        String p8 = (String) cleanPhoneMethod.invoke(instance, "02838291234");
        assertEquals("02838291234", p8);
        assertTrue((Boolean) isValidPhoneMethod.invoke(instance, p8));

        // Case 7: Số không hợp lệ (ít chữ số, chữ cái, v.v.)
        String pInvalid1 = (String) cleanPhoneMethod.invoke(instance, "12345");
        assertFalse((Boolean) isValidPhoneMethod.invoke(instance, pInvalid1));

        String pInvalid2 = (String) cleanPhoneMethod.invoke(instance, "0123456789");
        assertFalse((Boolean) isValidPhoneMethod.invoke(instance, pInvalid2));

        String pInvalid3 = (String) cleanPhoneMethod.invoke(instance, "abcxyz");
        assertFalse((Boolean) isValidPhoneMethod.invoke(instance, pInvalid3));
    }

    @Test
    @DisplayName("Khi xảy ra lỗi checkout, hệ thống phải redirect về /checkout thay vì /cart")
    void testCheckoutErrorRedirectsToCheckout() {
        OrderService orderService = mock(OrderService.class);
        when(orderService.checkout(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Số điện thoại người nhận không hợp lệ."));

        CheckoutController controller = new CheckoutController(
                mock(CartService.class),
                orderService,
                mock(com.finalproject.repository.UserRepository.class),
                mock(com.finalproject.repository.StoreRepository.class),
                mock(com.finalproject.repository.DeliveryRepository.class),
                mock(com.finalproject.repository.StoreInventoryRepository.class),
                mock(com.finalproject.service.UserService.class),
                mock(com.finalproject.service.VoucherService.class),
                mock(com.finalproject.config.MomoConfig.class),
                mock(com.finalproject.service.MomoService.class),
                mock(com.finalproject.service.DemoPaymentService.class),
                mock(com.finalproject.service.CustomerWalletService.class)
        );

        Principal principal = () -> "testuser";
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.handleCheckout(
                principal,
                List.of(10L, 20L),
                PaymentMethod.COD,
                "Nguyễn Văn A",
                "0933516123",
                "123 Lê Lợi, Q1",
                "Lời chúc",
                null,
                LocalDate.now(),
                java.time.LocalTime.NOON,
                null,
                "DELIVERY",
                null,
                1L,
                "NEAR",
                new MockHttpServletRequest(),
                redirectAttributes
        );

        assertEquals("redirect:/checkout?selectedItemIds=10&selectedItemIds=20", view);
        assertEquals("Số điện thoại người nhận không hợp lệ.", redirectAttributes.getFlashAttributes().get("errorMessage"));
        assertEquals("0933516123", redirectAttributes.getFlashAttributes().get("prevRecipientPhone"));
        assertEquals("Nguyễn Văn A", redirectAttributes.getFlashAttributes().get("prevRecipientName"));
    }
}
