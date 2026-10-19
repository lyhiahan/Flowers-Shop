package com.finalproject;

import com.finalproject.entity.*;
import com.finalproject.enums.*;
import com.finalproject.dto.request.AddToCartRequest;
import com.finalproject.repository.*;
import com.finalproject.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
@WithMockUser(username="branch_catalog_manager",roles="MANAGER")
class StoreProductCatalogTests {
    @Autowired MockMvc mvc;
    @Autowired StoreRepository stores;
    @Autowired ProductRepository products;
    @Autowired CategoryRepository categories;
    @Autowired StoreInventoryRepository stocks;
    @Autowired UserRepository users;
    @Autowired OrderRepository orders;
    @Autowired OrderDetailRepository details;
    @Autowired CartRepository carts;
    @Autowired CartItemRepository items;
    @Autowired CartService cartService;
    @Autowired OrderService orderService;
    @Autowired jakarta.persistence.EntityManager em;
    Store a,b; Product own,shared,foreign; User customer;
    Category category;

    @BeforeEach void setup() {
        a=stores.save(Store.builder().name("Branch catalog A").address("Quận 1").build());
        b=stores.save(Store.builder().name("Branch catalog B").address("Quận 7").build());
        users.save(User.builder().username("branch_catalog_manager").email("branch_catalog_manager@example.test")
                .password("test-only").fullName("Manager catalog A").role(Role.MANAGER).store(a).isActive(true).build());
        customer=users.save(User.builder().username("branch_catalog_customer").email("branch_catalog_customer@example.test")
                .password("test-only").fullName("Customer catalog").role(Role.USER).isActive(true).build());
        category=categories.save(Category.builder().name("Branch catalog flowers").build());
        own=flower("Owned legacy flower", a); own.setIsDeleted(null); products.save(own);
        shared=flower("Shared legacy flower", null); shared.setIsDeleted(null); products.save(shared);
        foreign=flower("Foreign legacy flower", b);
        stock(own,a,10); stock(shared,a,15); stock(shared,b,7); stock(foreign,a,99); stock(foreign,b,6);
        em.flush();
    }
    Product flower(String name,Store owner) {
        return products.save(Product.builder().name(name).category(category).store(owner)
                .priceBuy(new BigDecimal("120000")).importPrice(new BigDecimal("60000"))
                .priceRentPerDay(BigDecimal.ZERO).depositFee(BigDecimal.ZERO).imageUrl("https://res.cloudinary.com/test/history.png").build());
    }
    StoreInventory stock(Product product,Store store,int quantity) {
        return stocks.save(StoreInventory.builder().product(product).store(store).quantity(quantity).build());
    }

    @Test void includesOwnAndSharedProductsWithoutExposingForeignStock() throws Exception {
        mvc.perform(get("/manager/products")).andExpect(status().isOk())
                .andExpect(model().attribute("totalElements",2))
                .andExpect(model().attribute("currentStockQuantity",25L))
                .andExpect(model().attributeDoesNotExist("legacyStockQuantity"))
                .andExpect(content().string(containsString("Shared legacy flower")))
                .andExpect(content().string(containsString("Owned legacy flower")))
                .andExpect(content().string(containsString("Xóa")))
                .andExpect(result -> assertEditLink(result.getResponse().getContentAsString(), own.getId()));
        for (String tab : List.of("selling", "stored", "all", "deleted")) {
            mvc.perform(get("/manager/products").param("tab",tab)).andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Foreign legacy flower"))))
                    .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Kho lệch chi nhánh"))));
        }
        mvc.perform(get("/manager/products").param("tab","legacy")).andExpect(status().isBadRequest());
        mvc.perform(get("/vendor/products").param("tab","legacy")).andExpect(status().isBadRequest());
        mvc.perform(get("/manager/dashboard")).andExpect(status().isOk())
                .andExpect(model().attribute("currentStockQuantity",25L))
                .andExpect(model().attributeDoesNotExist("legacyStockQuantity"))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("kho cũ lệch chi nhánh"))));
    }

    private void assertEditLink(String html, Long productId) throws Exception {
        Map<String, String> links = new HashMap<>();
        new javax.swing.text.html.parser.ParserDelegator().parse(new java.io.StringReader(html),
                new javax.swing.text.html.HTMLEditorKit.ParserCallback() {
                    String href;
                    final StringBuilder label = new StringBuilder();
                    @Override public void handleStartTag(javax.swing.text.html.HTML.Tag tag,
                            javax.swing.text.MutableAttributeSet attributes, int position) {
                        if (tag == javax.swing.text.html.HTML.Tag.A) {
                            href = (String) attributes.getAttribute(javax.swing.text.html.HTML.Attribute.HREF);
                            label.setLength(0);
                        }
                    }
                    @Override public void handleText(char[] text, int position) {
                        if (href != null) label.append(text);
                    }
                    @Override public void handleEndTag(javax.swing.text.html.HTML.Tag tag, int position) {
                        if (tag == javax.swing.text.html.HTML.Tag.A && href != null) {
                            links.put(href, label.toString().strip()); href = null;
                        }
                    }
                }, true);
        assertThat(links).containsEntry("/manager/products/edit/" + productId, "Sửa");
    }

    @Test void badgeUsesTotalCountNotJustFirstPage() throws Exception {
        for(int i=0;i<10;i++) stock(flower("Paged flower " + i,a),a,1);
        mvc.perform(get("/manager/products").param("size","8")).andExpect(status().isOk())
                .andExpect(model().attribute("totalElements",12)).andExpect(model().attribute("totalPages",2));
        mvc.perform(get("/manager/products").param("page","1").param("size","8"))
                .andExpect(status().isOk()).andExpect(model().attribute("currentPage",1));
    }

    @Test void sharedStopAndStockEditOnlyAffectTheSelectedBranch() throws Exception {
        mvc.perform(post("/manager/products/branch/"+shared.getId()).with(csrf())
                .param("quantity","12").param("selling","false")).andExpect(status().is3xxRedirection());
        assertThat(shared.getIsSelling()).isTrue();
        assertThat(stocks.findByStoreIdAndProductId(a.getId(),shared.getId()).orElseThrow().getQuantity()).isEqualTo(12);
        assertThat(stocks.findByStoreIdAndProductId(b.getId(),shared.getId()).orElseThrow().isAvailableForSale()).isTrue();
        mvc.perform(get("/manager/products").param("tab","stored")).andExpect(status().isOk()).andExpect(model().attribute("totalElements",1));
        assertThat(stocks.sumCatalogQuantity(a.getId())).isEqualTo(10);
        assertThat(stocks.sumRemainingQuantity(a.getId())).isEqualTo(22);
        mvc.perform(post("/manager/products/stock/"+shared.getId()).with(csrf()).param("quantity","11"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/manager/products/branch/"+shared.getId())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Lưu thay đổi")));
    }

    @Test void sharedDeletePreservesStockImagesAndOtherBranchAndCanBeRestored() throws Exception {
        mvc.perform(post("/manager/products/delete/"+shared.getId()).with(csrf())).andExpect(status().is3xxRedirection());
        assertThat(shared.getIsDeleted()).isFalse();
        var local=stocks.findByStoreIdAndProductId(a.getId(),shared.getId()).orElseThrow();
        assertThat(local.getIsDeleted()).isTrue(); assertThat(local.getQuantity()).isEqualTo(15);
        assertThat(stocks.findByStoreIdAndProductId(b.getId(),shared.getId()).orElseThrow().getQuantity()).isEqualTo(7);
        assertThat(stocks.sumRemainingQuantity(a.getId())).isEqualTo(10);
        assertThat(shared.getImageUrl()).isEqualTo("https://res.cloudinary.com/test/history.png");
        mvc.perform(get("/manager/products").param("tab","deleted")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Khôi phục"))).andExpect(model().attribute("totalElements",1));
        mvc.perform(post("/manager/products/restore/"+shared.getId()).with(csrf())).andExpect(status().is3xxRedirection());
        assertThat(local.getIsDeleted()).isFalse(); assertThat(local.getIsSelling()).isFalse();
        assertThat(stocks.sumRemainingQuantity(a.getId())).isEqualTo(25);
    }

    @Test void ownDeleteIsSoftAndPreservesCompletedOrderHistory() throws Exception {
        var order=orders.save(Order.builder().user(customer).pickupStore(a).orderType(OrderType.BUY).status(OrderStatus.COMPLETED).build());
        var detail=details.save(OrderDetail.builder().order(order).product(own).quantity(1).unitPrice(new BigDecimal("120000")).build());
        mvc.perform(post("/manager/products/delete/"+own.getId()).with(csrf())).andExpect(status().is3xxRedirection());
        assertThat(products.findById(own.getId()).orElseThrow().getIsDeleted()).isTrue();
        assertThat(details.findById(detail.getId())).isPresent(); assertThat(orders.findById(order.getId())).isPresent();
        assertThat(stocks.findByStoreIdAndProductId(a.getId(),own.getId()).orElseThrow().getQuantity()).isEqualTo(10);
        mvc.perform(post("/manager/products/restore/"+own.getId()).with(csrf())).andExpect(status().is3xxRedirection());
        assertThat(own.getIsDeleted()).isFalse(); assertThat(own.getIsSelling()).isFalse();
    }

    @Test void deleteWithOpenOrdersIsBlockedAndForeignMutationsAreForbidden() throws Exception {
        var order=orders.save(Order.builder().user(customer).pickupStore(a).orderType(OrderType.BUY).status(OrderStatus.PENDING).build());
        details.save(OrderDetail.builder().order(order).product(own).quantity(1).unitPrice(new BigDecimal("120000")).build());
        mvc.perform(post("/manager/products/delete/"+own.getId()).with(csrf())).andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("errorMessage"));
        assertThat(own.getIsDeleted()).isFalse();
        mvc.perform(post("/manager/products/delete/"+foreign.getId()).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/manager/products/stock/"+foreign.getId()).with(csrf()).param("quantity","1"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/manager/products/toggle-selling/"+foreign.getId()).with(csrf())).andExpect(status().isForbidden());
    }

    @Test void sharedMetadataCannotBeEditedAndDeleteRequiresCsrf() throws Exception {
        mvc.perform(post("/manager/products/update/"+shared.getId()).with(csrf()).param("name","Wrong rename")
                .param("categoryId",category.getId().toString()).param("priceBuy","100000")).andExpect(status().isForbidden());
        mvc.perform(post("/manager/products/delete/"+own.getId())).andExpect(status().isForbidden());
        assertThat(shared.getName()).isEqualTo("Shared legacy flower");
    }

    @Test void oldCartCannotCheckoutFromStoppedOrDeletedBranch() throws Exception {
        var cart=carts.save(Cart.builder().user(customer).build());
        var item=items.save(CartItem.builder().cart(cart).product(shared).store(a).quantity(1).orderType(OrderType.BUY).build());
        cart.getItems().add(item);
        mvc.perform(post("/manager/products/toggle-selling/"+shared.getId()).with(csrf()).param("selling","false"))
                .andExpect(status().is3xxRedirection());
        assertThatThrownBy(() -> orderService.quote(customer.getUsername(),List.of(item.getId()),"Quận 1",null,"PICKUP",a.getId(),null))
                .hasMessageContaining("ngừng bán");
        var request=new AddToCartRequest(); request.setProductId(shared.getId()); request.setStoreId(a.getId()); request.setOrderType(OrderType.BUY); request.setQuantity(1);
        assertThatThrownBy(() -> cartService.addToCart(customer.getUsername(),request)).isInstanceOf(IllegalStateException.class);
        assertThat(orders.count()).isZero(); assertThat(stocks.findByStoreIdAndProductId(a.getId(),shared.getId()).orElseThrow().getQuantity()).isEqualTo(15);
    }
}
