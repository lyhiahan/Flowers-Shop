package com.finalproject;

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
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class StaffRoleSeparationTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired StoreRepository stores;
    @Autowired StoreMembershipRepository memberships;
    @Autowired OrderRepository orders;
    @Autowired ChatService chat;
    @Autowired com.finalproject.service.impl.CustomUserDetailsService details;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder encoder;
    User manager, staff, customer, admin;
    Store own, foreign;
    Order order;

    User account(String name, Role role) {
        return users.save(User.builder().username(name).email(name + "@staff.test").fullName(name)
                .password(encoder.encode("testPassword123")).role(role).isActive(true).build());
    }

    @BeforeEach void setup() {
        manager=account("separate_manager",Role.MANAGER); staff=account("separate_staff",Role.STAFF);
        customer=account("separate_customer",Role.USER); admin=account("separate_admin",Role.ADMIN);
        own=stores.save(Store.builder().name("Chi nhánh nhân viên").address("123 Test street").owner(manager).isOpen(true).status(true).build());
        foreign=stores.save(Store.builder().name("Chi nhánh khác").address("456 Test street").isOpen(true).status(true).build());
        manager.setStore(own); users.save(manager); staff.setStore(own); users.save(staff);
        memberships.save(StoreMembership.builder().user(staff).store(own).accessRole("STAFF").active(true).build());
        order=orders.save(Order.builder().user(customer).pickupStore(own).orderType(OrderType.BUY).status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.TEN).paymentMethod("COD").paymentStatus("UNPAID").shippingMethod("PICKUP").build());
    }

    @Test void staffHasItsOwnAuthorityAndLoginDestination() throws Exception {
        assertThat(details.loadUserByUsername(staff.getUsername()).getAuthorities()).extracting("authority").containsExactly("ROLE_STAFF");
        mvc.perform(post("/perform_login").with(csrf()).param("username",staff.getUsername()).param("password","testPassword123"))
                .andExpect(redirectedUrl("/staff/orders"));
    }

    @Test void staffScreenRendersOwnOrdersAndNoManagementLinks() throws Exception {
        String html=mvc.perform(get("/staff/orders").with(user(staff.getUsername()).roles("STAFF")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Bàn làm việc nhân viên","Chi nhánh nhân viên","#HD"+order.getId(),"Nhân viên cửa hàng")
                .doesNotContain("href=\"/manager/wallet", "href=\"/manager/staff", "href=\"/manager/products");
        for(String path: new String[]{"/manager/dashboard","/manager/staff","/manager/products","/manager/analytics","/manager/wallet","/manager/stores","/manager/profile","/admin/users"})
            mvc.perform(get(path).with(user(staff.getUsername()).roles("MANAGER"))).andExpect(status().isForbidden());
    }

    @Test void publicHeaderIdentifiesStaffAndLinksToItsOwnWorkspace() throws Exception {
        mvc.perform(get("/").with(user(staff.getUsername()).roles("STAFF")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"navStaffOrdersDropdown\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("href=\"/customer/profile\""))));
    }

    @Test void staffCanConfirmPrepareAndHandOverPickupOrder() throws Exception {
        for(OrderStatus next: new OrderStatus[]{OrderStatus.CONFIRMED,OrderStatus.PREPARING,OrderStatus.COMPLETED})
            mvc.perform(post("/staff/orders/status/"+order.getId()).with(user(staff.getUsername()).roles("STAFF")).with(csrf())
                    .param("status",next.name()).param("codCollected",Boolean.toString(next == OrderStatus.COMPLETED))).andExpect(flash().attributeExists("successMessage"));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test void staffCannotMutateForeignOrdersOrPerformFinancialActions() throws Exception {
        Order other=orders.save(Order.builder().user(customer).pickupStore(foreign).orderType(OrderType.BUY).status(OrderStatus.PENDING).totalAmount(BigDecimal.TEN).build());
        mvc.perform(post("/staff/orders/approve/"+other.getId()).with(user(staff.getUsername()).roles("STAFF")).with(csrf()))
                .andExpect(flash().attributeExists("errorMessage"));
        assertThat(other.getStatus()).isEqualTo(OrderStatus.PENDING);
        for(String next: new String[]{"CANCELLED","REFUNDED"})
            mvc.perform(post("/staff/orders/status/"+order.getId()).with(user(staff.getUsername()).roles("STAFF")).with(csrf()).param("status",next))
                    .andExpect(flash().attributeExists("errorMessage"));
        mvc.perform(post("/manager/staff/add").with(user(staff.getUsername()).roles("MANAGER")).with(csrf()).param("username","forged"))
                .andExpect(status().isForbidden());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test void staffCanAssistOwnCustomersButCannotReadOtherBranchOrInternalSupport() throws Exception {
        Long ownChat=chat.open(customer.getUsername(),"STORE",own.getId()).id();
        assertThat(chat.context(staff.getUsername()).role()).isEqualTo("STAFF");
        chat.send(staff.getUsername(),ownChat,"Nhân viên hỗ trợ bạn",UUID.randomUUID().toString(),own.getId());
        assertThat(chat.history(customer.getUsername(),ownChat,null).messages()).hasSize(1);
        assertThatThrownBy(()->chat.inbox(staff.getUsername(),0,foreign.getId())).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        Long internal=chat.open(manager.getUsername(),"BRANCH_SUPPORT",own.getId()).id();
        assertThatThrownBy(()->chat.history(staff.getUsername(),internal,null,own.getId())).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(()->chat.open(staff.getUsername(),"ADMIN",null)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(()->chat.inbox(staff.getUsername(),0,own.getId(),"BRANCH_SUPPORT",null,null,null)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test void removingStaffRevokesOrderAndChatAccessInAnExistingSession() throws Exception {
        Long thread=chat.open(customer.getUsername(),"STORE",own.getId()).id();
        mvc.perform(post("/manager/staff/remove/"+staff.getId()).with(user(manager.getUsername()).roles("MANAGER")).with(csrf()))
                .andExpect(flash().attributeExists("successMessage"));
        mvc.perform(post("/staff/orders/approve/"+order.getId()).with(user(staff.getUsername()).roles("STAFF")).with(csrf()))
                .andExpect(flash().attributeExists("errorMessage"));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThatThrownBy(()->chat.history(staff.getUsername(),thread,null)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        mvc.perform(get("/staff/orders").with(user(staff.getUsername()).roles("STAFF"))).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Bạn chưa được phân công")));
    }

    @Test void managerCreatesOnlyStaffAccountsAndCannotAssignManagerAsStaff() throws Exception {
        mvc.perform(post("/manager/staff/add").with(user(manager.getUsername()).roles("MANAGER")).with(csrf())
                .param("username","new_real_staff").param("email","new_staff@staff.test").param("password","testPassword123").param("role","STAFF"))
                .andExpect(flash().attributeExists("successMessage"));
        User created=users.findByUsername("new_real_staff").orElseThrow();
        assertThat(created.getRole()).isEqualTo(Role.STAFF);
        assertThat(created.hasRole(Role.MANAGER)).isFalse();
        assertThat(memberships.findByUserIdAndStoreId(created.getId(),own.getId()).orElseThrow().getAccessRole()).isEqualTo("STAFF");
        for(String forged: new String[]{"MANAGER","PARTNER","ADMIN"})
            mvc.perform(post("/manager/staff/add").with(user(manager.getUsername()).roles("MANAGER")).with(csrf()).param("username","forged_staff").param("role",forged))
                    .andExpect(flash().attributeExists("errorMessage"));
        assertThat(users.findByUsername("forged_staff")).isEmpty();
    }
}
