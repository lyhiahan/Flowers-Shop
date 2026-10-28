package com.finalproject;

import com.finalproject.dto.request.AddToCartRequest;
import com.finalproject.entity.*;
import com.finalproject.enums.*;
import com.finalproject.repository.*;
import com.finalproject.service.CartService;
import com.finalproject.service.OrderService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class CartLineMergingTests {
    @Autowired UserRepository users;
    @Autowired StoreRepository stores;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired StoreInventoryRepository inventories;
    @Autowired CartRepository carts;
    @Autowired CartItemRepository items;
    @Autowired CartService cartService;
    @Autowired OrderService orderService;
    @Autowired EntityManager entityManager;
    @Autowired MockMvc mvc;
    User customer;
    Store store;
    Product product;

    @BeforeEach void setup() {
        customer = users.save(User.builder().username("cart-merge").email("cart-merge@test.local")
                .fullName("Cart customer").password("unused").role(Role.USER).isActive(true).build());
        store = stores.save(Store.builder().name("Merge branch").address("123 Test street").status(true).isOpen(true).build());
        var category = categories.save(Category.builder().name("Merge flowers").build());
        product = products.save(Product.builder().name("Rose bouquet").category(category).store(store)
                .priceBuy(new BigDecimal("100000")).importPrice(BigDecimal.TEN)
                .build());
        inventories.saveAndFlush(StoreInventory.builder().store(store).product(product).quantity(10).build());
    }

    private AddToCartRequest request(int quantity, LocalDate date) {
        var request = new AddToCartRequest();
        request.setProductId(product.getId()); request.setStoreId(store.getId());
        request.setQuantity(quantity); request.setDeliveryDate(date);
        return request;
    }

    private List<CartItem> savedLines() {
        entityManager.flush(); entityManager.clear();
        return items.findByCartId(carts.findByUserId(customer.getId()).orElseThrow().getId());
    }

    @Test void sameProductAndBranchAccumulateEvenWhenDatesDiffer() {
        assertThat(cartService.addToCart(customer.getUsername(), request(1, LocalDate.now().plusDays(1)))).isEqualTo(1);
        Long firstId = savedLines().getFirst().getId();
        assertThat(cartService.addToCart(customer.getUsername(), request(2, LocalDate.now().plusDays(4)))).isEqualTo(3);
        assertThat(cartService.addToCart(customer.getUsername(), request(1, null))).isEqualTo(4);
        assertThat(savedLines()).singleElement().satisfies(line -> {
            assertThat(line.getId()).isEqualTo(firstId);
            assertThat(line.getQuantity()).isEqualTo(4);
            assertThat(line.getBuyAmount()).isEqualByComparingTo("400000");
        });
    }

    @Test void legacyDateDuplicatesAreCombinedWithoutLosingUnitsOrLatestGreeting() {
        Cart cart = cartService.getCartByUsername(customer.getUsername());
        var first = items.saveAndFlush(CartItem.builder().cart(cart).product(product).store(store).orderType(OrderType.BUY)
                .quantity(2).deliveryDate(LocalDate.now().minusDays(6)).greetingMessage("Old greeting").build());
        items.saveAndFlush(CartItem.builder().cart(cart).product(product).store(store).orderType(OrderType.BUY)
                .quantity(3).deliveryDate(LocalDate.now()).greetingMessage("Latest greeting").build());
        assertThat(cartService.addToCart(customer.getUsername(), request(1, null))).isEqualTo(6);
        assertThat(savedLines()).singleElement().satisfies(line -> {
            assertThat(line.getId()).isEqualTo(first.getId());
            assertThat(line.getQuantity()).isEqualTo(6);
            assertThat(line.getGreetingMessage()).isEqualTo("Latest greeting");
        });
    }

    @Test void explicitGreetingReplacesPreviousGreetingOnTheMergedRow() {
        var initial = request(1, null); initial.setGreetingMessage("First");
        cartService.addToCart(customer.getUsername(), initial);
        var next = request(1, LocalDate.now().plusDays(1)); next.setGreetingMessage("Updated");
        cartService.addToCart(customer.getUsername(), next);
        assertThat(savedLines()).singleElement().satisfies(line -> assertThat(line.getGreetingMessage()).isEqualTo("Updated"));
    }

    @Test void differentBranchesStaySeparate() {
        product.setStore(null); products.saveAndFlush(product);
        var other = stores.save(Store.builder().name("Second branch").address("Other street").status(true).isOpen(true).build());
        inventories.saveAndFlush(StoreInventory.builder().store(other).product(product).quantity(10).build());
        cartService.addToCart(customer.getUsername(), request(1, null));
        var request = request(2, null); request.setStoreId(other.getId());
        assertThat(cartService.addToCart(customer.getUsername(), request)).isEqualTo(3);
        assertThat(savedLines()).hasSize(2).extracting(line -> line.getStore().getId()).containsExactlyInAnyOrder(store.getId(), other.getId());
    }

    @Test void differentProductsStaySeparate() {
        var other = products.save(Product.builder().name("Other bouquet").category(product.getCategory()).store(store)
                .priceBuy(new BigDecimal("200000")).importPrice(BigDecimal.TEN)
                .build());
        inventories.saveAndFlush(StoreInventory.builder().store(store).product(other).quantity(10).build());
        cartService.addToCart(customer.getUsername(), request(1, null));
        var request = request(2, null); request.setProductId(other.getId());
        cartService.addToCart(customer.getUsername(), request);
        assertThat(savedLines()).hasSize(2).extracting(line -> line.getProduct().getId()).containsExactlyInAnyOrder(product.getId(), other.getId());
    }

    @Test void otherCustomersCartIsNotMerged() {
        var other = users.save(User.builder().username("other-merge").email("other-merge@test.local")
                .fullName("Other").password("unused").role(Role.USER).isActive(true).build());
        cartService.addToCart(other.getUsername(), request(3, null));
        cartService.addToCart(customer.getUsername(), request(1, null));
        cartService.addToCart(customer.getUsername(), request(1, LocalDate.now().plusDays(1)));
        assertThat(savedLines()).singleElement().satisfies(line -> assertThat(line.getQuantity()).isEqualTo(2));
        assertThat(items.findByCartId(carts.findByUserId(other.getId()).orElseThrow().getId()))
                .singleElement().satisfies(line -> assertThat(line.getQuantity()).isEqualTo(3));
    }

    @Test void changedDateCannotBypassStockLimitAndDoesNotChangeExistingQuantity() {
        cartService.addToCart(customer.getUsername(), request(9, null));
        assertThatThrownBy(() -> cartService.addToCart(customer.getUsername(), request(2, LocalDate.now().plusDays(1))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("tồn kho");
        assertThat(savedLines()).singleElement().satisfies(line -> assertThat(line.getQuantity()).isEqualTo(9));
    }

    @Test void ajaxAndFormAddsKeepOneRowAndReturnTheTotalUnitCount() throws Exception {
        mvc.perform(post("/cart/add").with(user(customer.getUsername()).roles("USER")).with(csrf())
                        .header("X-Requested-With", "XMLHttpRequest").param("productId", product.getId().toString())
                        .param("storeId", store.getId().toString()).param("quantity", "1")
                        .param("deliveryDate", LocalDate.now().plusDays(1).toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(1));
        mvc.perform(post("/cart/add").with(user(customer.getUsername()).roles("USER")).with(csrf())
                        .param("productId", product.getId().toString()).param("storeId", store.getId().toString())
                        .param("quantity", "1").param("deliveryDate", LocalDate.now().plusDays(2).toString()))
                .andExpect(redirectedUrl("/cart"));
        savedLines();
        mvc.perform(get("/cart/state").with(user(customer.getUsername()).roles("USER"))
                        .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rowCount").value(1))
                .andExpect(jsonPath("$.count").value(2)).andExpect(jsonPath("$.items[0].quantity").value(2));
        mvc.perform(get("/cart").with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Giao ngày:"))));
    }

    @Test void buyNowSelectsOnlyRequestedProductWithoutIncludingOtherCartLines() throws Exception {
        var other = products.save(Product.builder().name("Other bouquet").category(product.getCategory()).store(store)
                .priceBuy(new BigDecimal("200000")).importPrice(BigDecimal.TEN)
                .build());
        inventories.saveAndFlush(StoreInventory.builder().store(store).product(other).quantity(10).build());
        var otherRequest = request(2, null); otherRequest.setProductId(other.getId());
        cartService.addToCart(customer.getUsername(), otherRequest);

        var result = mvc.perform(post("/cart/buy-now").with(user(customer.getUsername()).roles("USER")).with(csrf())
                        .param("productId", product.getId().toString()).param("storeId", store.getId().toString())
                        .param("orderType", "BUY").param("quantity", "3"))
                .andExpect(status().is3xxRedirection()).andReturn();
        var lines = savedLines();
        assertThat(lines).hasSize(2);
        var chosen = lines.stream().filter(line -> line.getProduct().getId().equals(product.getId())).findFirst().orElseThrow();
        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/checkout?selectedItemIds=" + chosen.getId());
        assertThat(chosen.getQuantity()).isEqualTo(3);
        assertThat(lines.stream().filter(line -> line.getProduct().getId().equals(other.getId())).findFirst().orElseThrow().getQuantity()).isEqualTo(2);
        assertThat(inventories.findByStoreIdAndProductId(store.getId(), product.getId()).orElseThrow().getQuantity()).isEqualTo(10);
    }

    @Test void productDetailRendersBuyNowAsSeparatePostForm() throws Exception {
        mvc.perform(get("/product/" + product.getId()).with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("form=\"detailBuyNowForm\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"detailBuyNowForm\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/cart/buy-now\" method=\"post\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"buyNowQuantity\" name=\"quantity\" value=\"1\"")));
    }

    @Test void buyNowUsesChosenQuantityInsteadOfAccumulatingExistingCartQuantity() {
        cartService.addToCart(customer.getUsername(), request(8, null));
        Long existingId = savedLines().getFirst().getId();
        Long selectedId = cartService.prepareBuyNow(customer.getUsername(), request(2, null));
        assertThat(selectedId).isEqualTo(existingId);
        assertThat(savedLines()).singleElement().satisfies(line -> assertThat(line.getQuantity()).isEqualTo(2));
    }

    @Test void buyNowRejectsInsufficientStockWithoutChangingTheCart() throws Exception {
        cartService.addToCart(customer.getUsername(), request(2, null));
        mvc.perform(post("/cart/buy-now").with(user(customer.getUsername()).roles("USER")).with(csrf())
                        .param("productId", product.getId().toString()).param("storeId", store.getId().toString())
                        .param("orderType", "BUY").param("quantity", "11"))
                .andExpect(redirectedUrl("/product/" + product.getId()))
                .andExpect(flash().attributeExists("errorMessage"));
        assertThat(savedLines()).singleElement().satisfies(line -> assertThat(line.getQuantity()).isEqualTo(2));
    }

    @Test void checkoutUsesTheChosenDateAndAllMergedUnits() {
        cartService.addToCart(customer.getUsername(), request(1, LocalDate.now()));
        cartService.addToCart(customer.getUsername(), request(2, LocalDate.now().plusDays(1)));
        Long id = savedLines().getFirst().getId();
        var chosenDate = LocalDate.now().plusDays(5);
        var orders = orderService.checkout(customer.getUsername(), List.of(id), PaymentMethod.COD,
                "Customer", "0901234567", "Pickup", null, null, chosenDate, "PICKUP", store.getId());
        assertThat(orders).singleElement().satisfies(order -> {
            assertThat(order.getDeliveryDate()).isEqualTo(chosenDate);
            assertThat(order.getTotalAmount()).isEqualByComparingTo("300000");
            assertThat(order.getOrderDetails()).singleElement().satisfies(detail -> assertThat(detail.getQuantity()).isEqualTo(3));
        });
        assertThat(savedLines()).isEmpty();
        assertThat(inventories.findByStoreIdAndProductId(store.getId(), product.getId()).orElseThrow().getQuantity()).isEqualTo(7);
    }
}
