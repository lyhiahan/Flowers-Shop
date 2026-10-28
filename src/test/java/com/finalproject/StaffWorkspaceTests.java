package com.finalproject;

import com.finalproject.entity.*;
import com.finalproject.enums.*;
import com.finalproject.repository.*;
import com.finalproject.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class StaffWorkspaceTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired StoreRepository stores;
    @Autowired OrderRepository orders;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired OrderDetailRepository details;
    @Autowired PaymentRepository payments;
    @Autowired TransactionRepository transactions;
    @Autowired DeliveryRepository deliveries;
    @Autowired ChatService chat;
    User staff, customer;
    Store own, foreign;
    Order order, other;

    User userAccount(String name, Role role) {
        return users.save(User.builder().username(name).fullName(name).email(name+"@workspace.test").password("unused")
                .role(role).isActive(true).build());
    }
    Order newOrder(Store store, OrderStatus status, String shipping) {
        return orders.save(Order.builder().user(customer).pickupStore(store).orderType(OrderType.BUY).status(status)
                .recipientName("Khách đặt hoa").recipientPhone("0901234567").recipientAddress("123 Đường Hoa, Quận 7")
                .deliveryDate(LocalDate.of(2026,10,25)).deliveryTime(LocalTime.of(14,30)).greetingMessage("Chúc mừng sinh nhật")
                .totalAmount(new BigDecimal("215000")).shippingFee(new BigDecimal("20000")).discountAmount(new BigDecimal("5000"))
                .paymentMethod("COD").paymentStatus("UNPAID").shippingMethod(shipping).createdAt(LocalDateTime.now()).build());
    }
    @BeforeEach void setup() {
        staff=userAccount("workspace_staff",Role.STAFF); customer=userAccount("workspace_customer",Role.USER);
        User manager=userAccount("workspace_manager",Role.MANAGER);
        own=stores.save(Store.builder().name("Chi nhánh Hoa Q7").address("12 Đường Tiệm Hoa").owner(manager).status(true).isOpen(true).build());
        foreign=stores.save(Store.builder().name("Chi nhánh bí mật").address("99 Đường Khác").status(true).isOpen(true).build());
        staff.setStore(own); users.save(staff); manager.setStore(own); users.save(manager);
        order=newOrder(own,OrderStatus.PENDING,"DELIVERY"); other=newOrder(foreign,OrderStatus.PENDING,"PICKUP");
        other.setRecipientName("Khách ở chi nhánh khác"); orders.save(other);
        var category=categories.save(Category.builder().name("Hoa kiểm thử nhân viên").build());
        var flower=products.save(Product.builder().name("Bó hoa cho nhân viên chuẩn bị").category(category).store(own)
                .priceBuy(new BigDecimal("999999")).importPrice(BigDecimal.ZERO).isSelling(false).build());
        var line=details.save(OrderDetail.builder().order(order).product(flower).orderType(OrderType.BUY)
                .quantity(2).unitPrice(new BigDecimal("100000")).build());
        order.setOrderDetails(new java.util.ArrayList<>(List.of(line))); orders.save(order);
    }

    void change(OrderStatus next, boolean cod, boolean success) throws Exception {
        mvc.perform(post("/staff/orders/status/"+order.getId()).with(user(staff.getUsername()).roles("STAFF")).with(csrf())
                .param("status",next.name()).param("codCollected",Boolean.toString(cod)))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists(success?"successMessage":"errorMessage"));
    }

    @Test void allStaffPagesRenderSharedNavigationAndBranchScope() throws Exception {
        for(String path:List.of("/staff/orders","/staff/orders/preparation","/staff/orders/delivery","/staff/chat")) {
            String html=mvc.perform(get(path).with(user(staff.getUsername()).roles("STAFF")))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertThat(html).contains("staffNavOrders","staffNavPreparation","staffNavDelivery","staffNavChat","Chi nhánh Hoa Q7")
                    .doesNotContain("Khách ở chi nhánh khác");
            assertThat(html.split("aria-current=\"page\"",-1)).hasSize(2);
        }
        mvc.perform(get("/staff").with(user(staff.getUsername()).roles("STAFF"))).andExpect(redirectedUrl("/staff/orders"));
        mvc.perform(get("/staff/chat").with(user(customer.getUsername()).roles("USER"))).andExpect(status().isForbidden());
    }

    @Test void detailIncludesDeliveryScheduleGreetingSnapshotPriceAndPausedProduct() throws Exception {
        var unit=deliveries.save(Delivery.builder().name("Giao hoa hỏa tốc").price(BigDecimal.TEN).build());
        order.setDeliveryUnitId(unit.getId()); orders.save(order);
        String html=mvc.perform(get("/staff/orders/"+order.getId()).with(user(staff.getUsername()).roles("STAFF")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("123 Đường Hoa, Quận 7","25/10/2026","14:30","Chúc mừng sinh nhật",
                "Bó hoa cho nhân viên chuẩn bị","100,000","200,000","215,000","Giao hoa hỏa tốc")
                .doesNotContain("999,999","Khách ở chi nhánh khác");
    }

    @Test void directForeignAndMissingOrderLinksCannotExposeInformation() throws Exception {
        mvc.perform(get("/staff/orders/"+other.getId()).with(user(staff.getUsername()).roles("STAFF"))).andExpect(status().isForbidden());
        mvc.perform(get("/staff/orders/99999999").with(user(staff.getUsername()).roles("STAFF"))).andExpect(status().isNotFound());
    }

    @Test void filteringAndPagingNeverIncludeOtherBranchOrders() throws Exception {
        newOrder(own,OrderStatus.CONFIRMED,"PICKUP"); newOrder(own,OrderStatus.PREPARING,"STORE_PICKUP");
        var result=mvc.perform(get("/staff/orders").with(user(staff.getUsername()).roles("STAFF")).param("size","1"))
                .andExpect(model().attribute("totalElements",3)).andReturn();
        assertThat((List<?>)result.getModelAndView().getModel().get("orders")).hasSize(1);
        mvc.perform(get("/staff/orders").with(user(staff.getUsername()).roles("STAFF")).param("keyword","#HD"+order.getId()))
                .andExpect(model().attribute("totalElements",1));
        mvc.perform(get("/staff/orders").with(user(staff.getUsername()).roles("STAFF")).param("keyword","Khách ở chi nhánh khác"))
                .andExpect(model().attribute("totalElements",0));
        mvc.perform(get("/staff/orders/preparation").with(user(staff.getUsername()).roles("STAFF")))
                .andExpect(model().attribute("totalElements",2));
        mvc.perform(get("/staff/orders/delivery").with(user(staff.getUsername()).roles("STAFF")).param("fulfillment","PICKUP"))
                .andExpect(model().attribute("totalElements",1));
        mvc.perform(get("/staff/orders").with(user(staff.getUsername()).roles("STAFF")).param("status","PENDING").param("fulfillment","PICKUP"))
                .andExpect(model().attribute("totalElements",0));
    }

    @Test void deliveryCodRequiresCorrectSequenceAndCollectionThenSettlesOnlyOnce() throws Exception {
        change(OrderStatus.COMPLETED,true,false); assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        change(OrderStatus.CONFIRMED,false,true); change(OrderStatus.PREPARING,false,true);
        change(OrderStatus.DELIVERED,true,false); change(OrderStatus.DELIVERING,false,true);
        change(OrderStatus.DELIVERED,false,false); assertThat(order.getPaymentStatus()).isEqualTo("UNPAID");
        assertThat(payments.findByOrderId(order.getId())).isEmpty();
        change(OrderStatus.DELIVERED,true,true);
        assertThat(order.getPaymentStatus()).isEqualTo("PAID");
        change(OrderStatus.COMPLETED,false,true); change(OrderStatus.COMPLETED,false,true);
        assertThat(payments.findByOrderId(order.getId())).hasSize(1);
        assertThat(transactions.findAllByOrderIdAndType(order.getId(),"ORDER_PAYMENT")).hasSize(1);
        assertThat(order.getAmountToCollect()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test void pickupAndLegacyPickupCannotEnterDeliveryAndNeedCodConfirmationOnLegacyEndpoint() throws Exception {
        order.setShippingMethod("STORE_PICKUP"); order.setStatus(OrderStatus.PREPARING); orders.saveAndFlush(order);
        change(OrderStatus.DELIVERING,true,false);
        String html=mvc.perform(get("/staff/orders/"+order.getId()).with(user(staff.getUsername()).roles("STAFF")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Tiền cần thu:","name=\"codCollected\"","required","Nhận tại quầy")
                .doesNotContain("staffAction-DELIVERING");
        mvc.perform(post("/staff/orders/deliver/"+order.getId()).with(user(staff.getUsername()).roles("STAFF")).with(csrf()))
                .andExpect(flash().attributeExists("errorMessage"));
        assertThat(order.getPaymentStatus()).isEqualTo("UNPAID");
        mvc.perform(post("/staff/orders/deliver/"+order.getId()).with(user(staff.getUsername()).roles("STAFF")).with(csrf()).param("codCollected","true"))
                .andExpect(flash().attributeExists("successMessage"));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test void prepaidOrdersNeedNoCodAndUnpaidOnlineOrdersCannotBeDelivered() throws Exception {
        order.setPaymentMethod("BANK_TRANSFER"); order.setStatus(OrderStatus.PREPARING); orders.saveAndFlush(order);
        String html=mvc.perform(get("/staff/orders/"+order.getId()).with(user(staff.getUsername()).roles("STAFF")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Chuyển yêu cầu xác nhận chuyển khoản cho quản lý").doesNotContain("staffAction-DELIVERING");
        change(OrderStatus.DELIVERING,false,false);
        order.setPaymentStatus("PAID"); orders.saveAndFlush(order);
        change(OrderStatus.DELIVERING,false,true); change(OrderStatus.DELIVERED,false,true); change(OrderStatus.COMPLETED,false,true);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test void staffChatIsAnEmbeddedWorkbenchAndCannotReadForeignConversation() throws Exception {
        String html=mvc.perform(get("/staff/chat").with(user(staff.getUsername()).roles("STAFF")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("chat-workbench");
        assertThat(html.split("id=\"flowerChat\"",-1)).hasSize(2);
        Long conversation=chat.open(customer.getUsername(),"STORE",own.getId()).id();
        mvc.perform(get("/api/chat/conversations").with(user(staff.getUsername()).roles("STAFF")).param("storeId",own.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.conversations[0].id").value(conversation));
        mvc.perform(get("/api/chat/conversations").with(user(staff.getUsername()).roles("STAFF")).param("storeId",foreign.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test void removedAssignmentShowsHelpWithoutChatOrOrders() throws Exception {
        staff.setStore(null); users.saveAndFlush(staff);
        for(String path:List.of("/staff/orders","/staff/orders/preparation","/staff/orders/delivery","/staff/chat"))
            mvc.perform(get(path).with(user(staff.getUsername()).roles("STAFF"))).andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Bạn chưa được phân công")))
                    .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("id=\"flowerChat\""))));
    }

    @Test void malformedFiltersCsrfAndForgedFinancialStatusAreRejected() throws Exception {
        for(String[] parameter:List.of(new String[]{"size","101"},new String[]{"page","-1"},new String[]{"status","UNKNOWN"},new String[]{"keyword","x".repeat(101)}))
            mvc.perform(get("/staff/orders").with(user(staff.getUsername()).roles("STAFF")).param(parameter[0],parameter[1])).andExpect(status().isBadRequest());
        mvc.perform(post("/staff/orders/status/"+order.getId()).with(user(staff.getUsername()).roles("STAFF")).param("status","CONFIRMED"))
                .andExpect(status().isForbidden());
        change(OrderStatus.REFUNDED,true,false); change(OrderStatus.CANCELLED,true,false);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }
}
