package com.finalproject;

import com.finalproject.entity.*;
import com.finalproject.dto.request.AddToCartRequest;
import com.finalproject.enums.*;
import com.finalproject.repository.*;
import com.finalproject.service.*;
import com.finalproject.service.impl.CustomUserDetailsService;
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
    @Autowired CustomUserDetailsService details;
    @Autowired MockMvc mvc;
    User customer; Store store; Product product; Cart cart; CartItem first;
    @BeforeEach void setup() {
        customer=users.save(User.builder().username("edge_customer").email("edge@test.local").password("unused")
                .fullName("Test").role(Role.USER).isActive(true).build());
        store=stores.save(Store.builder().name("Edge store").address("123 Test street").status(true).isOpen(true).build());
        var category=categories.save(Category.builder().name("Hoa").build());
        product=products.save(Product.builder().name("Hoa hồng").store(store).category(category)
                .importPrice(BigDecimal.TEN).priceBuy(new BigDecimal("100000"))
                .priceRentPerDay(BigDecimal.ZERO).depositFee(BigDecimal.ZERO).build());
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
}
