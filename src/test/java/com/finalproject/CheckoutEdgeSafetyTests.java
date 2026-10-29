package com.finalproject;

import com.finalproject.entity.*;
import com.finalproject.dto.request.AddToCartRequest;
import com.finalproject.enums.*;
import com.finalproject.repository.*;
import com.finalproject.service.*;
import com.finalproject.service.CustomUserDetailsService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.ArrayList;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class CheckoutEdgeSafetyTests {
    @Autowired UserRepository users;
    @Autowired StoreRepository stores;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired StoreInventoryRepository stocks;
    @Autowired CartRepository carts;
    @Autowired CartItemRepository items;
    @Autowired CartService cartService;
    @Autowired OrderService orderService;
    @Autowired ShippingTariffRepository tariffs;
    @Autowired DeliveryRepository deliveries;
    @Autowired CustomUserDetailsService details;
    @Autowired MockMvc mvc;
    User customer; Store store; Product product; Cart cart; CartItem first;
    @BeforeEach void setup() {
        users.save(User.builder().username("edge_admin").email("edge-admin@test.local").password("unused").fullName("Admin").role(Role.ADMIN).isActive(true).build());
        customer=users.save(User.builder().username("edge_customer").email("edge@test.local").password("unused")
                .fullName("Test").role(Role.USER).isActive(true).build());
        store=stores.save(Store.builder().name("Edge store").address("123 Test street").status(true).isOpen(true).build());
        var category=categories.save(Category.builder().name("Hoa").build());
        product=products.save(Product.builder().name("Hoa hồng").store(store).category(category)
                .importPrice(BigDecimal.TEN).priceBuy(new BigDecimal("100000"))
                .build());
        stocks.save(StoreInventory.builder().store(store).product(product).quantity(10).build());
        cart=carts.save(Cart.builder().user(customer).items(new ArrayList<>()).build());
        first=items.saveAndFlush(CartItem.builder().cart(cart).product(product).store(store).quantity(2)
                .orderType(OrderType.BUY).deliveryDate(LocalDate.now()).build());
        cart.getItems().add(first);
    }
    CartItem second(int quantity) {
        return items.saveAndFlush(CartItem.builder().cart(cart).store(store).product(product).orderType(OrderType.BUY)
                .quantity(quantity).deliveryDate(LocalDate.now().plusDays(1)).build());
    }
    @Test void openingDeliveryAdminMustNotCreateOrRecreateTariffs() throws Exception {
        long before = tariffs.count();
        mvc.perform(get("/admin/deliveries").with(user("edge_admin").roles("ADMIN")))
                .andExpect(status().isOk());
        assertThat(tariffs.count()).isEqualTo(before);
    }

    Delivery carrier(String name) {
        return deliveries.save(Delivery.builder().name(name).price(BigDecimal.ZERO).isActive(true).isDeleted(false).build());
    }

    @Test void unsupportedOrderTypeIsNotPartOfTheSalesModel() {
        assertThatThrownBy(() -> OrderType.valueOf("RENT")).isInstanceOf(IllegalArgumentException.class);
        assertThat(OrderType.values()).containsExactly(OrderType.BUY);
    }
    ShippingTariff tariff(Delivery carrier, String area, boolean active) {
        return tariffs.save(ShippingTariff.builder().store(store).delivery(carrier).destinationArea(area)
                .serviceName("Tiêu chuẩn").baseFee(new BigDecimal("25000")).bulkySurcharge(BigDecimal.ZERO)
                .isActive(active).isDeleted(false).build());
    }
    CheckoutQuote deliveryQuote(Delivery carrier) {
        return orderService.quote(customer.getUsername(), java.util.List.of(first.getId()),
                "123 Lê Lợi, Quận 1", null, "DELIVERY", null, carrier.getId());
    }
    @Test void shippingCannotUseAnotherCarriersTariff() {
        var chosen = carrier("Chosen carrier");
        tariff(carrier("Other carrier"), "Khu vực gần chi nhánh (< 5km)", true);
        assertThatThrownBy(() -> deliveryQuote(chosen)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void shippingCannotUseWrongZoneOrDisabledTariff() {
        var chosen = carrier("Chosen carrier");
        var rate = tariff(chosen, "Khu vực xa hơn (5 - 15km)", true);
        assertThatThrownBy(() -> deliveryQuote(chosen)).isInstanceOf(IllegalArgumentException.class);
        rate.setDestinationArea("Khu vực gần chi nhánh (< 5km)"); rate.setIsActive(false);
        assertThatThrownBy(() -> deliveryQuote(chosen)).isInstanceOf(IllegalArgumentException.class);
        rate.setIsActive(true);
        assertThat(deliveryQuote(chosen).shippingFee()).isEqualByComparingTo("25000");
    }
    @Test void branchTariffOverridesGlobalTariffForSameCarrierAndZone() {
        var chosen = carrier("Chosen carrier");
        var global = tariff(chosen, "Khu vực gần chi nhánh (< 5km)", true);
        global.setStore(null); global.setBaseFee(new BigDecimal("40000")); tariffs.saveAndFlush(global);
        assertThat(deliveryQuote(chosen).shippingFee()).isEqualByComparingTo("40000");
        tariff(chosen, "Khu vực gần chi nhánh (< 5km)", true);
        assertThat(deliveryQuote(chosen).shippingFee()).isEqualByComparingTo("25000");
    }
    @Test void checkoutAutomaticallyAssignsStandardCarrierForTheSelectedZone() throws Exception {
        var standard = carrier("Giao hàng tiêu chuẩn");
        tariff(standard, "Khu vực gần chi nhánh (< 5km)", true);
        var express = carrier("Giao hỏa tốc");
        var expressRate = tariff(express, "Khu vực gần chi nhánh (< 5km)", true);
        expressRate.setServiceName("Hỏa tốc");
        expressRate.setBaseFee(new BigDecimal("10000"));
        tariffs.saveAndFlush(expressRate);

        mvc.perform(post("/checkout/quote").with(user(customer.getUsername()).roles("USER")).with(csrf())
                .param("selectedItemIds", first.getId().toString())
                .param("shippingMethod", "DELIVERY").param("deliveryZone", "NEAR")
                .param("recipientAddress", "123 Lê Lợi, Quận 1")
                .param("deliveryUnitId", express.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryUnitId").value(standard.getId()))
                .andExpect(jsonPath("$.deliveryUnitName").value(standard.getName()))
                .andExpect(jsonPath("$.shippingFee").value(25000));

        var created = orderService.checkout(customer.getUsername(), java.util.List.of(first.getId()),
                PaymentMethod.COD, "Người nhận", "0901234567", "123 Lê Lợi, Quận 1",
                null, null, LocalDate.now().plusDays(2), "DELIVERY", null, null,
                java.time.LocalTime.of(9, 0), "NEAR");
        assertThat(created).isNotEmpty();
        assertThat(created.get(0).getDeliveryUnitId()).isEqualTo(standard.getId());
    }

    @Test void missingRecipientFieldsShowErrorsWithoutCreatingAnOrderOrReducingStock() throws Exception {
        var standard = carrier("Giao hàng tiêu chuẩn");
        tariff(standard, "Khu vực gần chi nhánh (< 5km)", true);
        String[] names = {"", "Người nhận", "Người nhận"};
        String[] phones = {"0901234567", "", "0901234567"};
        String[] addresses = {"123 Lê Lợi, Quận 1", "123 Lê Lợi, Quận 1", ""};
        String[] messages = {"Vui lòng nhập họ tên người nhận.", "Vui lòng nhập số điện thoại người nhận.",
                "Vui lòng nhập địa chỉ giao hàng."};
        for (int i = 0; i < messages.length; i++) {
            mvc.perform(post("/checkout").with(user(customer.getUsername()).roles("USER")).with(csrf())
                    .param("selectedItemIds", first.getId().toString()).param("paymentMethod", "COD")
                    .param("recipientName", names[i]).param("recipientPhone", phones[i])
                    .param("recipientAddress", addresses[i]).param("shippingMethod", "DELIVERY")
                    .param("deliveryZone", "NEAR")
                    .param("deliveryDate", LocalDate.now().plusDays(2).toString())
                    .param("deliveryTimeSlot", "09:00-11:00"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attribute("errorMessage", messages[i]));
        }
        assertThat(orderService.getOrdersByUsernameAndStatus(customer.getUsername(), null)).isEmpty();
        assertThat(stocks.findByStoreIdAndProductId(store.getId(), product.getId()).orElseThrow().getQuantity()).isEqualTo(10);
    }
    @Test void duplicateDateLineMustRespectCombinedStock() {
        second(7);
        var request=new AddToCartRequest(); request.setProductId(product.getId()); request.setStoreId(store.getId());
        request.setOrderType(OrderType.BUY); request.setQuantity(2); request.setDeliveryDate(LocalDate.now());
        assertThatThrownBy(()->cartService.addToCart(customer.getUsername(),request)).isInstanceOf(IllegalArgumentException.class);
        assertThat(first.getQuantity()).isEqualTo(2);
    }
    @Test void pickupMustAggregateAllLinesForSameProduct() throws Exception {
        var second=second(9);
        mvc.perform(get("/api/customer/pickup-check").with(user(customer.getUsername()).roles("USER"))
                .param("storeId",store.getId().toString()).param("itemIds",first.getId().toString(),second.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ok").value(false));
    }
    @Test void emptyOrMissingSelectionCannotReportAvailable() throws Exception {
        mvc.perform(get("/api/customer/pickup-check").with(user(customer.getUsername()).roles("USER"))
                .param("storeId",store.getId().toString())).andExpect(status().isBadRequest());
        mvc.perform(get("/api/customer/pickup-check").with(user(customer.getUsername()).roles("USER"))
                .param("storeId",store.getId().toString()).param("itemIds","999999999"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ok").value(false));
    }
    @Test void foreignCartSelectionIsForbidden() throws Exception {
        // Use a real active account so this checks authorization, not session revocation.
        users.saveAndFlush(User.builder().username("another_customer").email("another_customer@fixture.test")
                .password("unused-fixture-password").fullName("Access test").role(Role.USER).isActive(true).build());
        mvc.perform(get("/api/customer/pickup-check").with(user("another_customer").roles("USER"))
                .param("storeId",store.getId().toString()).param("itemIds",first.getId().toString())).andExpect(status().isForbidden());
    }
    @Test void tariffRejectsNegativeAndBlankInputs() throws Exception {
        long before=tariffs.count();
        mvc.perform(post("/admin/deliveries/tariffs/add").with(user("edge_admin").roles("ADMIN")).with(csrf())
                .param("storeId",store.getId().toString()).param("destinationArea","").param("serviceName","")
                .param("baseFee","-100").param("bulkySurcharge","-200")).andExpect(status().is3xxRedirection());
        assertThat(tariffs.count()).isEqualTo(before);
    }
    @Test void usernameAloneMustNotGrantAdmin() {
        var actor=users.save(User.builder().username("AdMiN").email("ordinary@test.local").password("unused")
                .fullName("Test").isActive(true).role(Role.USER).build());
        assertThat(details.loadUserByUsername(actor.getUsername()).getAuthorities()).noneMatch(a->a.getAuthority().equals("ROLE_ADMIN"));
    }

    @Test void quoteRejectsCombinedQuantityEvenWhenEachLineFitsIndividually() {
        var other=second(9);
        assertThatThrownBy(()->orderService.quote(customer.getUsername(), java.util.List.of(first.getId(),other.getId()),
                null,null,"PICKUP",store.getId(),null)).isInstanceOf(IllegalStateException.class);
    }

    @Test void missingOrPastPickupTimeCannotCreateOrder() {
        assertThatThrownBy(()->orderService.checkout(customer.getUsername(), java.util.List.of(first.getId()), PaymentMethod.BANK_TRANSFER,
                "Khách", "0901234567",null,null,null,LocalDate.now().plusDays(1),"PICKUP",store.getId(),null,null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->orderService.checkout(customer.getUsername(), java.util.List.of(first.getId()), PaymentMethod.BANK_TRANSFER,
                "Khách", "0901234567",null,null,null,LocalDate.now().minusDays(1),"PICKUP",store.getId(),null,java.time.LocalTime.NOON))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void demoCheckoutRedirectsToQrAndKeepsPaymentPending() throws Exception {
        var result = mvc.perform(post("/checkout").with(user(customer.getUsername()).roles("USER")).with(csrf())
                .param("selectedItemIds", first.getId().toString()).param("paymentMethod", "MOMO_DEMO")
                .param("recipientName", "Test customer").param("recipientPhone", "0901234567")
                .param("shippingMethod", "PICKUP").param("pickupStoreId", store.getId().toString())
                .param("deliveryDate", LocalDate.now().plusDays(2).toString()).param("deliveryTimeSlot", "09:00-11:00"))
                .andExpect(status().is3xxRedirection()).andReturn();
        assertThat(result.getResponse().getRedirectedUrl()).matches("/customer/demo-payment/[0-9a-f]{64}");
        var demoOrder = orderService.getOrdersByUsernameAndStatus(customer.getUsername(), null).get(0);
        assertThat(demoOrder.getPaymentMethod()).isEqualTo("MOMO_DEMO");
        assertThat(demoOrder.getPaymentStatus()).isEqualTo("UNPAID");
        assertThat(demoOrder.getPaymentExpiresAt()).isNotNull();
    }

    @Test void bankOptionIsRemovedAndForgedBankCheckoutCannotCreateOrder() throws Exception {
        var page = mvc.perform(get("/checkout").param("selectedItemIds", first.getId().toString())
                .with(user(customer.getUsername()).roles("USER"))).andExpect(status().isOk()).andReturn();
        assertThat(page.getResponse().getContentAsString()).contains("value=\"MOMO_DEMO\"", "value=\"COD\"")
                .doesNotContain("payOptionBank", "value=\"BANK_TRANSFER\"");
        mvc.perform(post("/checkout").with(user(customer.getUsername()).roles("USER")).with(csrf())
                .param("selectedItemIds", first.getId().toString()).param("paymentMethod", "BANK_TRANSFER"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("errorMessage"));
        assertThat(orderService.getOrdersByUsernameAndStatus(customer.getUsername(), null)).isEmpty();
        assertThat(stocks.findByStoreIdAndProductId(store.getId(), product.getId()).orElseThrow().getQuantity()).isEqualTo(10);
    }
}
