package com.finalproject;

import com.finalproject.entity.*;
import com.finalproject.enums.Role;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import java.time.*;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class ChatSupportMonitorTests {
    @Autowired ChatService chat;
    @Autowired UserRepository users;
    @Autowired StoreRepository stores;
    @Autowired StoreMembershipRepository memberships;
    @Autowired ChatMessageRepository messages;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean ChatAccessAuditRepository audits;
    @Autowired MockMvc mvc;
    @Autowired Clock clock;
    @MockitoBean CloudinaryService cloud;
    User customer, outsider, manager, foreignManager, partner, admin;
    Store a,b;
    @BeforeEach void fixture() {
        customer=account("support_customer",Role.USER);outsider=account("support_outsider",Role.USER);
        manager=account("support_manager",Role.MANAGER);foreignManager=account("support_foreign",Role.MANAGER);
        partner=account("support_partner",Role.MANAGER);admin=account("support_admin",Role.ADMIN);
        a=stores.save(Store.builder().name("Support Quận 1").address("Quận 1").status(true).isOpen(true).owner(manager).build());
        b=stores.save(Store.builder().name("Support Quận 7").address("Quận 7").status(true).isOpen(true).owner(foreignManager).build());
        memberships.saveAndFlush(StoreMembership.builder().user(partner).store(a).active(true).accessRole("PARTNER").build());
    }
    User account(String name,Role role) {return users.save(User.builder().username(name).email(name+"@support.test").password("unused").fullName(name).role(role).isActive(true).build());}
    String key(){return UUID.randomUUID().toString();}
    Long branch(){return chat.open(customer.getUsername(),"STORE",a.getId()).id();}
    Long internal(){return chat.open(manager.getUsername(),"BRANCH_SUPPORT",a.getId()).id();}

    @Test void internalSupportIsSharedByAuthorizedBranchManagersAndAdminOnly() {
        Long id=internal();
        assertThat(chat.open(partner.getUsername(),"BRANCH_SUPPORT",a.getId()).id()).isEqualTo(id);
        chat.send(manager.getUsername(),id,"Chi nhánh cần hỗ trợ",key(),a.getId());
        chat.send(admin.getUsername(),id,"Admin đã tiếp nhận",key());
        assertThat(chat.history(partner.getUsername(),id,null,a.getId()).messages()).hasSize(2);
        assertThatThrownBy(()->chat.history(customer.getUsername(),id,null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(()->chat.history(foreignManager.getUsername(),id,null,b.getId())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(()->chat.open(customer.getUsername(),"BRANCH_SUPPORT",a.getId())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(()->chat.open(manager.getUsername(),"BRANCH_SUPPORT",b.getId())).isInstanceOf(AccessDeniedException.class);
        assertThat(audits.count()).isZero();
    }
    @Test void adminHasSeparateCustomerSupportInternalSupportAndMonitorInboxes() {
        Long shop=branch(),internal=internal(),support=chat.open(customer.getUsername(),"ADMIN",null).id();
        assertThat(chat.inbox(admin.getUsername(),0).conversations()).extracting(ChatService.ConversationView::id).containsExactly(support);
        assertThat(chat.inbox(admin.getUsername(),0,null,"BRANCH_SUPPORT",null,null,null).conversations()).extracting(ChatService.ConversationView::id).containsExactly(internal);
        assertThat(chat.inbox(admin.getUsername(),0,null,"STORE",null,null,null).conversations()).extracting(ChatService.ConversationView::id).containsExactly(shop);
        assertThat(chat.inbox(manager.getUsername(),0,a.getId()).conversations()).extracting(ChatService.ConversationView::id).containsExactly(shop);
        assertThat(chat.inbox(manager.getUsername(),0,a.getId(),"BRANCH_SUPPORT",null,null,null).conversations()).extracting(ChatService.ConversationView::id).containsExactly(internal);
        assertThatThrownBy(()->chat.inbox(manager.getUsername(),0,a.getId(),"ADMIN",null,null,null)).isInstanceOf(AccessDeniedException.class);
    }
    @Test void monitorListDoesNotExposeMessageBodyWithoutAnAuditedOpen() throws Exception {
        Long id=branch();chat.send(customer.getUsername(),id,"Nội dung riêng của khách",key());
        mvc.perform(get("/api/chat/conversations").param("kind","STORE").with(user(admin.getUsername()).roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.conversations[0].lastMessage").value(org.hamcrest.Matchers.containsString("ghi nhật ký")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Nội dung riêng của khách"))));
        assertThat(audits.count()).isZero();
    }
    @Test void monitoringFailsClosedWhenAuditCannotBeRecorded() throws Exception {
        Long id=branch();chat.send(customer.getUsername(),id,"Nội dung không được trả nếu thiếu nhật ký",key());
        doThrow(new org.springframework.dao.DataAccessResourceFailureException("Audit unavailable"))
                .when(audits).saveAndFlush(any(ChatAccessAudit.class));
        mvc.perform(get("/api/chat/conversations/"+id+"/messages").with(user(admin.getUsername()).roles("ADMIN")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("CHAT_STORAGE_UNAVAILABLE"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Nội dung không được trả"))));
    }
    @Test void everyMonitorHistoryRequestRecordsAuthenticatedAdminConversationTimeAndPage() throws Exception {
        Long id=branch();var message=chat.send(customer.getUsername(),id,"Tin cần giám sát",key());
        mvc.perform(get("/api/chat/conversations/"+id+"/messages").with(user(admin.getUsername()).roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.messages[0].content").value("Tin cần giám sát"));
        mvc.perform(get("/api/chat/conversations/"+id+"/messages").param("before",message.id().toString()).with(user(admin.getUsername()).roles("ADMIN")))
                .andExpect(status().isOk());
        assertThat(audits.findAll()).hasSize(2).allSatisfy(log->{
            assertThat(log.getAdministrator().getId()).isEqualTo(admin.getId());
            assertThat(log.getConversation().getId()).isEqualTo(id);
            assertThat(log.getAccessedAt()).isNotNull();
        });
        assertThat(audits.findAll()).extracting(ChatAccessAudit::getBeforeMessageId).contains(null,message.id());
        mvc.perform(get("/admin/chat/access-logs").with(user(admin.getUsername()).roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString(admin.getUsername())));
    }
    @Test void adminCannotSendTextImageOrImpersonateShopDuringMonitoring() {
        Long id=branch();
        assertThatThrownBy(()->chat.send(admin.getUsername(),id,"Giả danh cửa hàng",key())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(()->chat.sendImage(admin.getUsername(),id,"",key(),new MockMultipartFile("file","x.png","image/png",new byte[]{1})))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(cloud);assertThat(messages.count()).isZero();assertThat(audits.count()).isZero();
    }
    @Test void customerSupportStillAllowsRepliesButIsPrivateFromOtherCustomersAndManagers() {
        Long id=chat.open(customer.getUsername(),"ADMIN",null).id();chat.send(admin.getUsername(),id,"CSKH hỗ trợ bạn",key());
        assertThat(chat.history(customer.getUsername(),id,null).messages()).hasSize(1);
        for(User actor:java.util.List.of(outsider,manager)) assertThatThrownBy(()->chat.history(actor.getUsername(),id,null)).isInstanceOf(AccessDeniedException.class);
        assertThat(audits.count()).isZero();
    }
    @Test void monitoringFiltersUseBranchCustomerAndInclusiveDateRange() {
        Long own=branch();chat.open(outsider.getUsername(),"STORE",b.getId());
        var today=LocalDate.now(clock);
        assertThat(chat.inbox(admin.getUsername(),0,a.getId(),"STORE","support_customer",today,today).conversations())
                .extracting(ChatService.ConversationView::id).containsExactly(own);
        assertThat(chat.inbox(admin.getUsername(),0,a.getId(),"STORE","support_customer",today.minusDays(3),today.minusDays(1)).conversations()).isEmpty();
        assertThat(chat.inbox(admin.getUsername(),0,null,"STORE","%",null,null).conversations()).isEmpty();
        assertThatThrownBy(()->chat.inbox(admin.getUsername(),0,null,"STORE",null,today,today.minusDays(1))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void nonAdminCannotReadAuditLogsOrOpenAdminWorkbench() throws Exception {
        for(User actor:java.util.List.of(customer,manager)) {
            assertThatThrownBy(()->chat.accessLogs(actor.getUsername(),0)).isInstanceOf(AccessDeniedException.class);
            mvc.perform(get("/api/chat/access-logs").with(user(actor.getUsername()).roles(actor.getRole().name()))).andExpect(status().isForbidden());
            mvc.perform(get("/admin/chat").with(user(actor.getUsername()).roles(actor.getRole().name()))).andExpect(status().isForbidden());
        }
        mvc.perform(get("/admin/chat").with(user(admin.getUsername()).roles("ADMIN"))).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("chat-workbench")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("admin-chat-page")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"chatFilterToggle\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"chatText\" rows=\"1\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nhật ký truy cập")));
    }
    @Test void invalidReadDoesNotCreateAuditAndNonAdminReadsAreNotMarkedAsMonitoring() throws Exception {
        Long id=branch();chat.history(manager.getUsername(),id,null,a.getId());chat.history(customer.getUsername(),id,null);
        mvc.perform(get("/api/chat/conversations/"+id+"/messages").param("before","-1").with(user(admin.getUsername()).roles("ADMIN")))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/chat/conversations").param("from","bad-date").with(user(admin.getUsername()).roles("ADMIN")))
                .andExpect(status().isBadRequest());
        assertThat(audits.count()).isZero();
    }
    @Test void revokedOrDowngradedManagerCannotUseInternalSupportHistory() {
        Long id=internal();manager.setRole(Role.USER);users.saveAndFlush(manager);
        assertThatThrownBy(()->chat.history(manager.getUsername(),id,null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(()->chat.send(manager.getUsername(),id,"Không còn quyền",key())).isInstanceOf(AccessDeniedException.class);
        var membership=memberships.findByUserIdAndStoreId(partner.getId(),a.getId()).orElseThrow();membership.setActive(false);memberships.saveAndFlush(membership);
        assertThatThrownBy(()->chat.history(partner.getUsername(),id,null,a.getId())).isInstanceOf(AccessDeniedException.class);
    }
}
