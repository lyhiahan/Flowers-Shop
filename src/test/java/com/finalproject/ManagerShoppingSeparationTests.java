package com.finalproject;

import com.finalproject.entity.*;
import com.finalproject.enums.*;
import com.finalproject.repository.*;
import com.finalproject.service.*;
import com.finalproject.security.SessionUserDetails;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import java.util.*;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class ManagerShoppingSeparationTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired StoreRepository stores;
    @Autowired ProductRepository products;
    @Autowired CategoryRepository categories;
    @Autowired StoreInventoryRepository stocks;
    @Autowired CartRepository carts;
    @Autowired CartService cart;
    @Autowired OrderService orders;
    @Autowired CustomerWalletService wallet;
    @Autowired DemoPaymentService demo;
    @Autowired CustomUserDetailsService userDetails;
    @Autowired JwtTokenService tokens;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder encoder;
    User manager;
    Store store;
    Product product;

    @BeforeEach void setup() {
        manager=users.save(User.builder().username("manager-shopping-test").email("manager-shopping@test.local")
                .fullName("Manager shopping test").password(encoder.encode("test-password"))
                .role(Role.MANAGER).roles(new HashSet<>(Set.of(Role.MANAGER,Role.USER))).isActive(true).build());
        store=stores.save(Store.builder().name("Manager branch").address("Test address").status(true).isOpen(true).owner(manager).build());
        manager.setStore(store);users.saveAndFlush(manager);
        var category=categories.save(Category.builder().name("Manager shopping flowers").build());
        product=products.save(Product.builder().name("Manager preview flower").category(category).store(store)
                .priceBuy(new BigDecimal("100000")).importPrice(BigDecimal.ONE).build());
        stocks.saveAndFlush(StoreInventory.builder().product(product).store(store).quantity(10).build());
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void legacyCustomerAssignmentCannotGrantShoppingAuthority() {
        assertThat(manager.hasRole(Role.USER)).isTrue();
        assertThat(manager.isCustomerAccount()).isFalse();
        assertThat(userDetails.loadUserByUsername(manager.getUsername()).getAuthorities())
                .extracting("authority").containsExactly("ROLE_MANAGER");
        manager.addRole(Role.MANAGER);
        assertThat(manager.getRoles()).doesNotContain(Role.USER);
    }

    @Test void oldSessionLosesShoppingButRetainsBranchManagement() throws Exception {
        var context=SecurityContextHolder.createEmptyContext();
        var principal=new SessionUserDetails(manager.getUsername(),manager.getPassword(),true,
                List.of(new SimpleGrantedAuthority("ROLE_USER"),new SimpleGrantedAuthority("ROLE_MANAGER")));
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal,null,principal.getAuthorities()));
        var session=new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,context);
        for(String path:List.of("/cart","/checkout","/customer/orders","/customer/wallet","/customer/profile","/following"))
            mvc.perform(get(path).session(session)).andExpect(status().isForbidden());
        for(String path:List.of("/cart/add","/cart/buy-now","/checkout","/checkout/quote","/checkout/momo/1/simulate"))
            mvc.perform(post(path).session(session).with(csrf()).param("productId",product.getId().toString()).param("quantity","1"))
                    .andExpect(status().isForbidden());
        mvc.perform(get("/manager/dashboard").session(session)).andExpect(status().isOk());
        mvc.perform(get("/manager/orders").session(session)).andExpect(status().isOk());
        assertThat(carts.findByUserId(manager.getId())).isEmpty();
    }

    @Test void serviceCallsCannotCreateACartOrderOrDebitWalletForAManager() {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                manager.getUsername(),null,List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))));
        assertThatThrownBy(()->cart.getCartByUsername(manager.getUsername())).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(()->cart.addToCart(manager.getUsername(),null)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThat(cart.getCartItemCount(manager.getUsername())).isZero();
        assertThatThrownBy(()->orders.checkout(manager.getUsername(),List.of(1L),PaymentMethod.COD))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(()->wallet.balance(manager.getUsername())).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(()->demo.create(List.of(1L),manager.getUsername())).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThat(carts.findByUserId(manager.getId())).isEmpty();
    }

    @Test void existingJwtReadsManagerOnlyAuthoritiesAfterPromotion() throws Exception {
        manager.setRole(Role.USER);manager.getRoles().clear();manager.getRoles().add(Role.USER);users.saveAndFlush(manager);
        var pair=tokens.issue(UsernamePasswordAuthenticationToken.authenticated(manager.getUsername(),null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        manager.setRole(Role.MANAGER);manager.getRoles().add(Role.MANAGER);users.saveAndFlush(manager);
        mvc.perform(get("/api/auth/me").header("Authorization","Bearer "+pair.accessToken()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.roles.length()").value(1)).andExpect(jsonPath("$.roles[0]").value("MANAGER"));
        mvc.perform(get("/api/customer/unknown").header("Authorization","Bearer "+pair.accessToken()))
                .andExpect(status().isForbidden());
    }

    @Test void managerPublicPagesHaveNoShoppingLinksAndKeepWorkspaceNavigation() throws Exception {
        mvc.perform(get("/notifications").with(user(manager.getUsername()).roles("USER","MANAGER")))
                .andExpect(redirectedUrl("/manager/notifications"));
        for(String path:List.of("/","/products","/product/"+product.getId(),"/stores","/stores/"+store.getId(),"/manager/notifications")) {
            String html=mvc.perform(get(path).with(user(manager.getUsername()).roles("USER","MANAGER")))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertThat(html).as(path).doesNotContain("href=\"/cart\"","action=\"/cart/add\"","action=\"/cart/buy-now\"",
                    "href=\"/customer/orders\"","href=\"/customer/wallet\"","href=\"/customer/profile\"");
            assertThat(html).as(path).containsAnyOf("href=\"/manager/dashboard\"","href=\"/manager/products\"");
        }
    }
}
