package com.finalproject;

import com.finalproject.entity.*;
import com.finalproject.enums.Role;
import com.finalproject.repository.*;
import com.finalproject.service.AccountProvisioningService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class FixedAccountRoleTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired UserAddressRepository addresses;
    @Autowired StoreRepository stores;
    @Autowired PasswordEncoder passwords;
    @Autowired AccountProvisioningService provisioning;
    User admin, customer, manager;
    @BeforeEach void setup() {
        admin = account("fixed_admin", Role.ADMIN);
        customer = account("fixed_customer", Role.USER);
        manager = account("fixed_manager", Role.MANAGER);
    }
    User account(String name, Role role) {
        return users.saveAndFlush(User.builder().username(name).fullName(name).email(name + "@fixed.test")
                .password(passwords.encode("fixed-password")).role(role).isActive(true).build());
    }
    @Test void oldRoleEndpointsCannotChangeExistingAccounts() throws Exception {
        for (String path : List.of("/admin/users/assign-role/", "/admin/users/revoke-role/", "/admin/users/role-request/approve/", "/admin/users/role-request/reject/")) {
            mvc.perform(post(path + customer.getId()).with(user(admin.getUsername()).roles("ADMIN")).with(csrf())
                    .param("role", "MANAGER")).andExpect(status().isNotFound());
        }
        mvc.perform(post("/customer/profile/role-request").with(user(customer.getUsername()).roles("USER")).with(csrf())
                .param("targetRole", "MANAGER")).andExpect(status().isNotFound());
        assertThat(users.findById(customer.getId()).orElseThrow().getRole()).isEqualTo(Role.USER);
        String profile = mvc.perform(get("/customer/profile?tab=roles").with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(profile).doesNotContain("registerRoleModal", "id=\"roles-tab\"", "role-request");
        String html = mvc.perform(get("/admin/users?tab=requests").with(user(admin.getUsername()).roles("ADMIN")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(html).contains("/admin/users/create", "usersTable").doesNotContain("assign-role", "revoke-role", "role-request");
    }
    @Test void adminCreatesSeparateAccountAndUserCannotProvision() throws Exception {
        mvc.perform(post("/admin/users/create").with(user(admin.getUsername()).roles("ADMIN")).with(csrf())
                .param("username", "new_manager").param("email", "new_manager@fixed.test").param("fullName", "Quản lý mới")
                .param("password", "new-password").param("role", "MANAGER"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("successMessage"));
        User created = users.findByUsername("new_manager").orElseThrow();
        assertThat(created.getRole()).isEqualTo(Role.MANAGER);
        assertThat(passwords.matches("new-password", created.getPassword())).isTrue();
        mvc.perform(post("/admin/users/create").with(user(customer.getUsername()).roles("USER")).with(csrf())).andExpect(status().isForbidden());
        assertThatThrownBy(() -> provisioning.create(customer.getUsername(), "forged_manager", "forged@fixed.test", "Name", "", "password123", "MANAGER"))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }
    @Test void emptyInvalidAndDuplicateAccountInputDoesNotWrite() throws Exception {
        long before = users.count();
        mvc.perform(post("/admin/users/create").with(user(admin.getUsername()).roles("ADMIN")).with(csrf()))
                .andExpect(flash().attributeExists("errorMessage"));
        for (String role : List.of("", "ADMIN", "SHIPPER", "unknown")) {
            assertThatThrownBy(() -> provisioning.create(admin.getUsername(), "bad_role", "bad@fixed.test", "Name", "", "password123", role)).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> provisioning.create(admin.getUsername(), customer.getUsername(), "other@fixed.test", "Name", "", "password123", "USER")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> provisioning.create(admin.getUsername(), "other_user", customer.getEmail(), "Name", "", "password123", "USER")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> provisioning.create(admin.getUsername(), "other_user", "other@fixed.test", "   ", "", "password123", "USER")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> provisioning.create(admin.getUsername(), "other_user", "other@fixed.test", "Name", "letters", "password123", "USER")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> provisioning.create(admin.getUsername(), "other_user", "other@fixed.test", "Name", "", "🌷".repeat(20), "USER")).isInstanceOf(IllegalArgumentException.class);
        assertThat(users.count()).isEqualTo(before);
    }
    @Test void assigningExistingUserToBranchDoesNotPromoteRole() throws Exception {
        Store store = stores.saveAndFlush(Store.builder().name("Fixed branch").address("Branch address").owner(manager).status(true).build());
        manager.setStore(store); users.saveAndFlush(manager);
        mvc.perform(post("/manager/staff/add").with(user(manager.getUsername()).roles("MANAGER")).with(csrf())
                .param("selectedUserIds", customer.getId().toString()).param("role", "MANAGER"))
                .andExpect(flash().attributeExists("errorMessage"));
        assertThat(customer.getRole()).isEqualTo(Role.USER);
        assertThat(customer.getStore()).isNull();
    }
    @Test void renderedEditAddressFormSubmitsItsActualCsrfTokenAndRejectsForeignAddress() throws Exception {
        UserAddress address = addresses.saveAndFlush(UserAddress.builder().user(customer).recipientName("Khách hàng")
                .phone("0900000000").addressDetail("123 Đường hoa").isDefault(true).build());
        var page = mvc.perform(get("/customer/profile").with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk()).andReturn();
        String html = page.getResponse().getContentAsString(StandardCharsets.UTF_8);
        var form = Pattern.compile("<form[^>]*id=\"formEditAddress\"[^>]*>(.*?)</form>", Pattern.DOTALL).matcher(html);
        assertThat(form.find()).isTrue();
        var token = Pattern.compile("<input(?=[^>]*name=\"_csrf\")(?=[^>]*value=\"([^\"]+)\")[^>]*>").matcher(form.group(1));
        assertThat(token.find()).as("dynamic POST form includes CSRF token").isTrue();
        String route = "/customer/profile/address/update/" + address.getId();
        mvc.perform(post(route).with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isForbidden());
        mvc.perform(post(route).with(user(customer.getUsername()).roles("USER")).session((MockHttpSession) page.getRequest().getSession())
                .param("_csrf", token.group(1)).param("recipientName", "Tên mới").param("phone", "0912345678").param("addressDetail", "456 Đường hoa"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("successMessage"));
        assertThat(addresses.findById(address.getId()).orElseThrow().getRecipientName()).isEqualTo("Tên mới");
        mvc.perform(post(route).with(user(manager.getUsername()).roles("USER", "MANAGER")).with(csrf())
                .param("recipientName", "Forbidden").param("phone", "0912345678").param("addressDetail", "Other address"))
                .andExpect(flash().attributeExists("errorMessage"));
        mvc.perform(post(route).with(user(customer.getUsername()).roles("USER")).with(csrf())
                .param("recipientName", " ").param("phone", "").param("addressDetail", " "))
                .andExpect(flash().attributeExists("errorMessage"));
        assertThat(addresses.findById(address.getId()).orElseThrow().getRecipientName()).isEqualTo("Tên mới");
    }

    @Test void invalidAddressInputSurvivesRedirectForAddAndEdit() throws Exception {
        var add = mvc.perform(post("/customer/profile/address/add")
                        .with(user(customer.getUsername()).roles("USER")).with(csrf())
                        .param("recipientName", "Người nhận mới").param("phone", "123")
                        .param("addressDetail", "24 Đường Hoa").param("isDefault", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("errorMessage", "addressAddDraft"))
                .andReturn();
        String addPage = mvc.perform(get("/customer/profile?tab=address")
                        .with(user(customer.getUsername()).roles("USER")).flashAttrs(add.getFlashMap()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(addPage).contains("value=\"Người nhận mới\"", "value=\"123\"", "24 Đường Hoa");
        assertThat(addPage).containsPattern("id=\"checkDefaultAddress\"[^>]*checked");

        UserAddress existing = addresses.saveAndFlush(UserAddress.builder().user(customer)
                .recipientName("Tên cũ").phone("0900000000").addressDetail("1 Đường Cũ").isDefault(true).build());
        var edit = mvc.perform(post("/customer/profile/address/update/" + existing.getId())
                        .with(user(customer.getUsername()).roles("USER")).with(csrf())
                        .param("recipientName", "Tên vừa sửa").param("phone", "abc")
                        .param("addressDetail", "9 Đường Mới").param("isDefault", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("errorMessage", "addressEditDraft", "addressEditId"))
                .andReturn();
        String editPage = mvc.perform(get("/customer/profile?tab=address")
                        .with(user(customer.getUsername()).roles("USER")).flashAttrs(edit.getFlashMap()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(editPage).contains("data-restore-address-id=\"" + existing.getId() + "\"",
                "value=\"Tên vừa sửa\"", "value=\"abc\"", "9 Đường Mới");
        assertThat(addresses.findById(existing.getId()).orElseThrow().getRecipientName()).isEqualTo("Tên cũ");
    }
}
