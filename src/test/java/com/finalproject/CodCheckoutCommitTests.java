package com.finalproject;

import com.finalproject.entity.*;
import com.finalproject.enums.*;
import com.finalproject.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Separate HTTP requests/transactions: verify stock survives commit, not just an ORM cache. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:cod-stock-commit;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CodCheckoutCommitTests {
    @Autowired MockMvc mvc;
    @Autowired TransactionTemplate tx;
    @Autowired UserRepository users;
    @Autowired StoreRepository stores;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired StoreInventoryRepository inventories;
    @Autowired CartRepository carts;
    @Autowired CartItemRepository items;
    @Autowired OrderRepository orders;

    @Test void successfulCodCommitsDeductionAndCancellationCommitsOneRestock() throws Exception {
        long[] fixture = tx.execute(status -> {
            var customer = users.save(User.builder().username("commit_customer").email("commit@test.local")
                    .password("unused").fullName("Commit customer").phone("0900000000").address("Quận 1")
                    .role(Role.USER).isActive(true).build());
            var store = stores.save(Store.builder().name("Commit branch").address("Quận 1").isOpen(true).status(true).build());
            var category = categories.save(Category.builder().name("Commit flowers").build());
            var product = products.save(Product.builder().name("Commit bouquet").store(store).category(category)
                    .priceBuy(new BigDecimal("100000")).importPrice(new BigDecimal("50000"))
                    .build());
            inventories.save(StoreInventory.builder().store(store).product(product).quantity(10).build());
            var cart = carts.save(Cart.builder().user(customer).items(new ArrayList<>()).build());
            var item = items.save(CartItem.builder().cart(cart).store(store).product(product).orderType(OrderType.BUY).quantity(2).build());
            return new long[]{store.getId(), product.getId(), item.getId()};
        });
        mvc.perform(post("/checkout").with(user("commit_customer").roles("USER")).with(csrf())
                        .param("selectedItemIds", Long.toString(fixture[2])).param("paymentMethod", "COD")
                        .param("shippingMethod", "DELIVERY").param("deliveryZone", "NEAR").param("recipientName", "Commit customer")
                        .param("recipientPhone", "0900000000").param("recipientAddress", "Quận 1")
                        .param("deliveryDate", LocalDate.now().plusDays(1).toString()).param("deliveryTimeSlot", "09:00-11:00"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/customer/orders"))
                .andExpect(flash().attributeExists("successMessage"));
        assertThat(tx.<Integer>execute(status -> inventories.findByStoreIdAndProductId(fixture[0], fixture[1]).orElseThrow().getQuantity()))
                .isEqualTo(8);
        mvc.perform(get("/api/public/catalog-stock").param("productIds", Long.toString(fixture[1])))
                .andExpect(status().isOk()).andExpect(jsonPath("$.currentStockQuantity").value(8))
                .andExpect(jsonPath("$.products['" + fixture[1] + "'].quantity").value(8));
        long id = tx.execute(status -> orders.findAll().getFirst().getId());
        for (int retry = 0; retry < 2; retry++) {
            mvc.perform(post("/customer/orders/cancel/" + id).with(user("commit_customer").roles("USER")).with(csrf()).param("reasonCode", "NO_LONGER_NEEDED"))
                    .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("successMessage"));
            assertThat(tx.<Integer>execute(status -> inventories.findByStoreIdAndProductId(fixture[0], fixture[1]).orElseThrow().getQuantity()))
                    .isEqualTo(10);
            mvc.perform(get("/api/public/catalog-stock").param("productIds", Long.toString(fixture[1])))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.currentStockQuantity").value(10))
                    .andExpect(jsonPath("$.products['" + fixture[1] + "'].quantity").value(10));
        }
    }
}
