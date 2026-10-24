package com.finalproject;

import com.finalproject.entity.*;
import com.finalproject.enums.*;
import com.finalproject.repository.*;
import com.finalproject.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class ChatBranchFlowTests {
    @Autowired ChatService chat;
    @Autowired UserRepository users;
    @Autowired StoreRepository stores;
    @Autowired StoreMembershipRepository memberships;
    @Autowired ProductRepository products;
    @Autowired CategoryRepository categories;
    @Autowired StoreInventoryRepository inventory;
    @Autowired OrderRepository orders;
    @Autowired ChatMessageRepository messages;
    @Autowired ChatConversationRepository conversations;
    @Autowired MockMvc mvc;
    @jakarta.persistence.PersistenceContext jakarta.persistence.EntityManager entityManager;
    @MockitoBean CloudinaryService cloud;
    User customer, outsider, manager, otherManager;
    Store a,b;
    Product flower;
    @BeforeEach void setup() {
        customer=account("scope_customer",Role.USER); outsider=account("scope_outsider",Role.USER);
        manager=account("scope_manager",Role.MANAGER); otherManager=account("scope_other_manager",Role.MANAGER);
        a=stores.save(Store.builder().name("Scope Quận 1").address("Quận 1").owner(manager).status(true).isOpen(true).build());
        b=stores.save(Store.builder().name("Scope Quận 7").address("Quận 7").owner(otherManager).status(true).isOpen(true).build());
        manager.setStore(a);users.save(manager);
        var category=categories.save(Category.builder().name("Scope flowers").build());
        flower=products.save(Product.builder().name("Scope orchid").store(b).category(category)
                .priceBuy(BigDecimal.TEN).importPrice(BigDecimal.ONE).priceRentPerDay(BigDecimal.ZERO).depositFee(BigDecimal.ZERO).build());
        inventory.saveAndFlush(StoreInventory.builder().store(b).product(flower).quantity(5).build());
    }
    User account(String name,Role role) { return users.save(User.builder().username(name).email(name+"@scope.test")
            .password("unused").fullName(name).role(role).isActive(true).build()); }
    Long thread(Store branch) { return chat.open(customer.getUsername(),"STORE",branch.getId()).id(); }

    @Test void productContactResolvesSellingBranchAndReusesThreadWithoutSendingAutomatically() throws Exception {
        for(int attempt=0;attempt<2;attempt++) mvc.perform(post("/api/chat/conversations/contact")
                .with(user(customer.getUsername()).roles("USER")).with(csrf()).contentType("application/json")
                .content("{\"productId\":"+flower.getId()+"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.conversation.storeId").value(b.getId()))
                .andExpect(jsonPath("$.product.name").value("Scope orchid"))
                .andExpect(jsonPath("$.product.id").value(flower.getId()));
        assertThat(conversations.count()).isEqualTo(1); assertThat(messages.count()).isZero();
        mvc.perform(get("/products/"+flower.getId()).with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("data-chat-product-id=\""+flower.getId()+"\"")));
    }
    @Test void productContactCannotBeTamperedToAnotherBranchOrPrivateProduct() {
        assertThatThrownBy(()->chat.contact(customer.getUsername(),flower.getId(),null,a.getId())).hasMessageContaining("không bán");
        flower.setIsSelling(false); products.saveAndFlush(flower);
        assertThatThrownBy(()->chat.contact(customer.getUsername(),flower.getId(),null,b.getId())).hasMessageContaining("khả dụng");
        assertThat(conversations.count()).isZero();
    }
    @Test void chainWideProductRequiresAnActualSellingBranch() {
        flower.setStore(null);products.saveAndFlush(flower);
        assertThatThrownBy(()->chat.contact(customer.getUsername(),flower.getId(),null,null)).hasMessageContaining("chọn chi nhánh");
        assertThatThrownBy(()->chat.contact(customer.getUsername(),flower.getId(),null,a.getId())).hasMessageContaining("không bán");
        assertThat(chat.contact(customer.getUsername(),flower.getId(),null,b.getId()).conversation().storeId()).isEqualTo(b.getId());
    }
    @Test void orderContactUsesRecordedFulfillmentBranchAndChecksOwnership() throws Exception {
        var order=orders.saveAndFlush(Order.builder().user(customer).pickupStore(a).status(OrderStatus.PENDING)
                .orderType(OrderType.BUY).paymentMethod("COD").paymentStatus("UNPAID").totalAmount(BigDecimal.TEN).build());
        assertThat(chat.contact(customer.getUsername(),null,order.getId(),b.getId()).conversation().storeId()).isEqualTo(a.getId());
        assertThatThrownBy(()->chat.contact(outsider.getUsername(),null,order.getId(),null))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        mvc.perform(get("/customer/orders/"+order.getId()).with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("data-chat-order-id=\""+order.getId()+"\"")));
    }
    @Test void foreignBranchIsDeniedForInboxHistoryAndImagesBeforeUpload() throws Exception {
        Long own=thread(a), foreign=thread(b);
        assertThat(chat.inbox(manager.getUsername(),0).conversations()).extracting(ChatService.ConversationView::id).containsExactly(own);
        mvc.perform(get("/api/chat/conversations").param("storeId",b.getId().toString()).with(user(manager.getUsername()).roles("MANAGER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/chat/conversations/"+foreign+"/messages").param("storeId",a.getId().toString()).with(user(manager.getUsername()).roles("MANAGER")))
                .andExpect(status().isForbidden());
        assertThatThrownBy(()->chat.sendImage(manager.getUsername(),foreign,"",UUID.randomUUID().toString(),
                new MockMultipartFile("file","flower.png","image/png",new byte[]{1}),a.getId()))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        verifyNoInteractions(cloud);
    }
    @Test void legacySecondMembershipCannotGrantForeignInboxAccess() throws Exception {
        memberships.saveAndFlush(StoreMembership.builder().user(manager).store(b).active(true).accessRole("PARTNER").build());
        Long own=thread(a), foreign=thread(b);
        var session=new MockHttpSession();session.setAttribute("activeStoreId",b.getId());
        mvc.perform(get("/api/chat/context").session(session).with(user(manager.getUsername()).roles("MANAGER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/chat/conversations").param("storeId",b.getId().toString()).with(user(manager.getUsername()).roles("MANAGER")))
                .andExpect(status().isForbidden());
        assertThat(chat.inbox(manager.getUsername(),0).conversations()).extracting(ChatService.ConversationView::id).containsExactly(own);
        assertThatThrownBy(()->chat.send(manager.getUsername(),foreign,"wrong branch",UUID.randomUUID().toString(),b.getId()))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }
    @Test void managerCannotUseOldOpenApiToCreatePersonalBuyerChats() throws Exception {
        mvc.perform(post("/api/chat/conversations").with(user(manager.getUsername()).roles("MANAGER")).with(csrf())
                .contentType("application/json").content("{\"kind\":\"STORE\",\"storeId\":"+b.getId()+"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/manager/profile").with(user(manager.getUsername()).roles("MANAGER")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("id=\"chatDestination\""))));
        assertThat(conversations.count()).isZero();
    }
    @Test void productQuestionPersistsSnapshotAndIsVisibleToBranchHistory() {
        var contact=chat.contact(customer.getUsername(),flower.getId(),null,null);
        String key=UUID.randomUUID().toString();
        var sent=chat.send(customer.getUsername(),contact.conversation().id(),"Mẫu này còn không?",key,null,flower.getId());
        assertThat(sent.product().name()).isEqualTo("Scope orchid");
        flower.setName("Tên thay đổi sau khi hỏi");flower.setPriceBuy(new BigDecimal("999"));products.saveAndFlush(flower);
        entityManager.clear();
        var history=chat.history(otherManager.getUsername(),contact.conversation().id(),null,b.getId());
        assertThat(history.messages().getFirst().product().name()).isEqualTo("Scope orchid");
        assertThat(history.messages().getFirst().product().price()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(chat.send(customer.getUsername(),contact.conversation().id(),"Mẫu này còn không?",key,null,flower.getId()).id()).isEqualTo(sent.id());
        assertThat(messages.count()).isEqualTo(1);
        assertThatThrownBy(()->chat.send(customer.getUsername(),contact.conversation().id(),"Mẫu này còn không?",key,null,null)).hasMessageContaining("nội dung khác");
    }
    @Test void productAttachmentCannotTargetWrongBranchOrBeForgedByManager() throws Exception {
        assertThatThrownBy(()->chat.send(customer.getUsername(),thread(a),"Sai chi nhánh",UUID.randomUUID().toString(),null,flower.getId()))
                .hasMessageContaining("không bán");
        assertThatThrownBy(()->chat.send(otherManager.getUsername(),thread(b),"Giả sản phẩm khách hỏi",UUID.randomUUID().toString(),b.getId(),flower.getId()))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(()->chat.sendImage(customer.getUsername(),thread(a),"",UUID.randomUUID().toString(),
                new MockMultipartFile("file","flower.png","image/png",new byte[]{1}),null,flower.getId())).hasMessageContaining("không bán");
        verifyNoInteractions(cloud);assertThat(messages.count()).isZero();
    }
    @Test void accountMenuContainsSystemChatButCommonHistoryHasNoSupportButton() throws Exception {
        mvc.perform(get("/").with(user(customer.getUsername()).roles("USER"))).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-chat-system")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Trò chuyện với hệ thống")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("id=\"chatSupportButton\""))));
        mvc.perform(get("/customer/profile").with(user(customer.getUsername()).roles("USER"))).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-chat-system")));
        mvc.perform(get("/").with(user(manager.getUsername()).roles("MANAGER"))).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("data-chat-system"))));
    }
}
