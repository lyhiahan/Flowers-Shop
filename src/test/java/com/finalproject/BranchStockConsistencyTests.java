package com.finalproject;

import com.finalproject.dto.request.AddToCartRequest;
import com.finalproject.entity.*;
import com.finalproject.enums.*;
import com.finalproject.repository.*;
import com.finalproject.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BranchStockConsistencyTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired StoreRepository stores;
    @Autowired ProductRepository products;
    @Autowired CategoryRepository categories;
    @Autowired StoreInventoryRepository inventories;
    @Autowired CartRepository carts;
    @Autowired CartItemRepository items;
    @Autowired OrderRepository orders;
    @Autowired OrderService orderService;
    @Autowired CartService cartService;
    Store nearest, owner;
    Product product;
    CartItem item;
    User customer;

    @BeforeEach void fixture() {
        nearest = stores.save(Store.builder().name("Branch Quận 1").address("Quận 1").status(true).isOpen(true).build());
        owner = stores.save(Store.builder().name("Branch Quận 7").address("Quận 7").status(true).isOpen(true).build());
        customer = users.save(User.builder().username("branch_stock_customer").email("branch_stock@test.local")
                .password("unused").fullName("Branch customer").phone("0900000000").address("Quận 1")
                .role(Role.USER).isActive(true).build());
        var category = categories.save(Category.builder().name("Branch stock flowers").build());
        product = products.save(Product.builder().name("Branch orchid").category(category).store(owner)
                .priceBuy(new BigDecimal("2300000")).importPrice(new BigDecimal("1000000"))
                .priceRentPerDay(BigDecimal.ZERO).depositFee(BigDecimal.ZERO).build());
        inventories.save(StoreInventory.builder().store(nearest).product(product).quantity(25).build());
        inventories.save(StoreInventory.builder().store(owner).product(product).quantity(15).build());
        var cart = carts.save(Cart.builder().user(customer).items(new ArrayList<>()).build());
        item = items.saveAndFlush(CartItem.builder().cart(cart).store(owner).product(product).quantity(15).orderType(OrderType.BUY).build());
        cart.getItems().add(item);
    }

    int stock(Store branch) {
        return inventories.findByStoreIdAndProductId(branch.getId(), product.getId()).orElseThrow().getQuantity();
    }

    com.finalproject.entity.Order checkout(String shipping, Long branch) {
        return orderService.checkout(customer.getUsername(), List.of(item.getId()), PaymentMethod.COD,
                "Branch customer", "0900000000", "Quận 1", null, null, LocalDate.now().plusDays(1),
                shipping, branch, null).getFirst();
    }

    @Test void wrongPickupBranchIsNotOfferedAndCannotReserveAnotherWarehouse() throws Exception {
        mvc.perform(get("/checkout").with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk()).andExpect(model().attribute("stores", List.of(owner)));
        mvc.perform(get("/api/customer/pickup-check").with(user(customer.getUsername()).roles("USER"))
                        .param("storeId", nearest.getId().toString()).param("itemIds", item.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ok").value(false));
        assertThatThrownBy(() -> orderService.quote(customer.getUsername(), List.of(item.getId()), null, null,
                "PICKUP", nearest.getId(), null)).hasMessageContaining("chỉ bán tại chi nhánh");
        assertThatThrownBy(() -> checkout("PICKUP", nearest.getId())).hasMessageContaining("chỉ bán tại chi nhánh");
        assertThat(stock(owner)).isEqualTo(15); assertThat(stock(nearest)).isEqualTo(25);
        assertThat(orders.count()).isZero();
    }

    @Test void buyingAllFifteenFromTheDisplayedBranchShowsSoldOutAndCancellationRestoresOnce() throws Exception {
        var order = checkout("PICKUP", owner.getId());
        orders.flush();
        assertThat(stock(owner)).isZero(); assertThat(stock(nearest)).isEqualTo(25);
        mvc.perform(get("/").with(user(customer.getUsername()).roles("USER"))).andExpect(status().isOk())
                .andExpect(model().attribute("customerStockQuantity", 0L)).andExpect(content().string(containsString("Hết hàng")));
        mvc.perform(get("/api/public/catalog-stock").param("productIds", product.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.products['" + product.getId() + "'].quantity").value(0));
        // A historical physical row in another branch must not be advertised as a sale fallback.
        mvc.perform(get("/api/inventory/check").param("productId", product.getId().toString()).param("storeId", nearest.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.available").value(false)).andExpect(jsonPath("$.currentStock").value(0));
        for (int retry = 0; retry < 2; retry++) {
            mvc.perform(post("/customer/orders/cancel/" + order.getId()).with(user(customer.getUsername()).roles("USER")).with(csrf()).param("reasonCode", "NO_LONGER_NEEDED"))
                    .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("successMessage"));
            inventories.flush();
            assertThat(stock(owner)).isEqualTo(15); assertThat(stock(nearest)).isEqualTo(25);
        }
    }

    @Test void deliveryQuoteAndCheckoutUseOwnerNotNearestBranchWithForeignInventory() {
        var quote = orderService.quote(customer.getUsername(), List.of(item.getId()), "Quận 1", null, "DELIVERY", null, null);
        assertThat(quote.storeId()).isEqualTo(owner.getId());
        var order = checkout("DELIVERY", null);
        assertThat(order.getPickupStore().getId()).isEqualTo(owner.getId());
        assertThat(stock(owner)).isZero(); assertThat(stock(nearest)).isEqualTo(25);
    }

    @Test void exhaustedOwnerCannotFallBackToAnotherBranchForAnOwnedProduct() {
        var row = inventories.findByStoreIdAndProductId(owner.getId(), product.getId()).orElseThrow();
        row.setQuantity(0); inventories.saveAndFlush(row);
        assertThatThrownBy(() -> orderService.quote(customer.getUsername(), List.of(item.getId()), "Quận 1", null, "DELIVERY", null, null))
                .hasMessageContaining("tồn kho");
        assertThatThrownBy(() -> checkout("DELIVERY", null)).hasMessageContaining("tồn kho");
        assertThat(stock(nearest)).isEqualTo(25); assertThat(orders.count()).isZero();
    }

    @Test void cartCannotBeTamperedToSelectAnotherBranchForOwnedProduct() {
        var request = new AddToCartRequest(); request.setProductId(product.getId()); request.setStoreId(nearest.getId()); request.setQuantity(1);
        assertThatThrownBy(() -> cartService.addToCart(customer.getUsername(), request)).hasMessageContaining("chỉ bán tại chi nhánh");
        item.setStore(nearest); items.saveAndFlush(item);
        assertThatThrownBy(() -> cartService.updateQuantity(customer.getUsername(), item.getId(), 1)).hasMessageContaining("không bán mẫu hoa");
        cartService.updateQuantity(customer.getUsername(), item.getId(), 0);
        assertThat(items.findById(item.getId())).isEmpty();
    }

    @Test void chainWideProductsStillSupportCrossBranchFulfillment() {
        product.setStore(null); products.saveAndFlush(product);
        var quote = orderService.quote(customer.getUsername(), List.of(item.getId()), "Quận 1", null, "DELIVERY", null, null);
        assertThat(quote.storeId()).isEqualTo(nearest.getId());
        var order = checkout("DELIVERY", null);
        assertThat(order.getPickupStore().getId()).isEqualTo(nearest.getId());
        assertThat(stock(nearest)).isEqualTo(10); assertThat(stock(owner)).isEqualTo(15);
    }
}
