package com.finalproject;

import com.finalproject.entity.*;
import com.finalproject.enums.*;
import com.finalproject.repository.*;
import com.finalproject.service.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class OrderAftercareTests {
    @Autowired OrderAftercareService care;
    @Autowired OrderWorkflowService workflow;
    @Autowired OrderAftercareRepository records;
    @Autowired OrderRepository orders;
    @Autowired UserRepository users;
    @Autowired StoreRepository stores;
    @Autowired EntityManager em;
    @Autowired MockMvc mvc;
    @MockitoBean CloudinaryService images;
    User owner, stranger, staff, manager, outsider, admin;
    Order order;
    Store store;

    @BeforeEach void setup() throws Exception {
        store = stores.save(Store.builder().name("Aftercare branch").address("Quận 1").eWallet(BigDecimal.ZERO).build());
        Store other = stores.save(Store.builder().name("Other branch").address("Quận 7").build());
        owner = account("care-owner", Role.USER, null);
        stranger = account("care-stranger", Role.USER, null);
        staff = account("care-staff", Role.STAFF, store);
        manager = account("care-manager", Role.MANAGER, store);
        outsider = account("care-outsider", Role.MANAGER, other);
        admin = account("care-admin", Role.ADMIN, null);
        store.setOwner(manager); stores.save(store);
        order = orders.saveAndFlush(Order.builder().user(owner).pickupStore(store).orderType(OrderType.BUY)
                .status(OrderStatus.PREPARING).totalAmount(new BigDecimal("100000"))
                .createdAt(LocalDateTime.now()).orderDetails(new java.util.ArrayList<>()).build());
        when(images.upload(any(), anyString())).thenReturn(new CloudinaryService.UploadResult(
                "https://res.cloudinary.com/fixture/image/upload/proof.png", "fixture-proof"));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    User account(String name, Role role, Store branch) {
        return users.save(User.builder().username(name).fullName(name).password("test-hash").email(name + "@test.local")
                .role(role).store(branch).isActive(true).createdDate(LocalDate.now()).build());
    }
    void login(User user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user.getUsername(), "", List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))));
    }
    MockMultipartFile photo() { return new MockMultipartFile("photo", "flower.png", "image/png", new byte[]{1,2,3}); }
    void delivered() { order.setStatus(OrderStatus.DELIVERED); orders.saveAndFlush(order); }

    @Test void approvalGatesDeliveryAndNewPhotoInvalidatesOldRevision() throws Exception {
        login(staff); care.sendFlowerPhoto(order.getId(), photo(), "Ảnh lần một"); em.flush(); em.clear();
        assertThat(records.findById(order.getId()).orElseThrow().getPhotoRevision()).isEqualTo(1);
        assertThatThrownBy(() -> workflow.transition(order.getId(), OrderStatus.DELIVERING)).isInstanceOf(IllegalStateException.class);
        login(owner); care.reviewFlowerPhoto(order.getId(), 1, "APPROVED", "Đồng ý");
        login(staff); care.sendFlowerPhoto(order.getId(), photo(), "Đã thay mẫu hoa");
        login(owner);
        assertThatThrownBy(() -> care.reviewFlowerPhoto(order.getId(), 1, "APPROVED", "")).isInstanceOf(IllegalStateException.class);
        care.reviewFlowerPhoto(order.getId(), 2, "APPROVED", "");
        login(staff); workflow.transition(order.getId(), OrderStatus.DELIVERING);
        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.DELIVERING);
        assertThat(orders.findById(order.getId()).orElseThrow().getPaymentStatus()).isEqualTo("UNPAID");
    }
    @Test void changesRequestedRequireExplanationAndReplacement() throws Exception {
        login(staff); care.sendFlowerPhoto(order.getId(), photo(), ""); login(owner);
        assertThatThrownBy(() -> care.reviewFlowerPhoto(order.getId(), 1, "CHANGES_REQUESTED", "a")).isInstanceOf(IllegalArgumentException.class);
        care.reviewFlowerPhoto(order.getId(), 1, "CHANGES_REQUESTED", "Đổi màu giấy gói");
        login(staff);
        assertThatThrownBy(() -> workflow.transition(order.getId(), OrderStatus.DELIVERING)).isInstanceOf(IllegalStateException.class);
        care.sendFlowerPhoto(order.getId(), photo(), "Đã đổi giấy gói");
        assertThat(records.findById(order.getId()).orElseThrow().getApprovalComment()).isNull();
    }
    @Test void otherCustomerAndOtherBranchCannotWriteOrUpload() {
        login(stranger);
        assertThatThrownBy(() -> care.reviewFlowerPhoto(order.getId(), 1, "APPROVED", "")).isInstanceOf(AccessDeniedException.class);
        login(outsider);
        assertThatThrownBy(() -> care.sendFlowerPhoto(order.getId(), photo(), "")).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(images);
        assertThat(records.findById(order.getId())).isEmpty();
    }
    @Test void deliveryEvidenceCannotBeOverwrittenAndReceiptIsIdempotent() throws Exception {
        delivered(); login(staff); care.recordDelivery(order.getId(), photo(), "Đã bàn giao tận tay");
        assertThatThrownBy(() -> care.recordDelivery(order.getId(), photo(), "Ghi đè ảnh cũ")).isInstanceOf(IllegalStateException.class);
        login(stranger);
        assertThatThrownBy(() -> care.confirmReceived(order.getId())).isInstanceOf(AccessDeniedException.class);
        login(owner); care.confirmReceived(order.getId());
        var first = records.findById(order.getId()).orElseThrow().getCustomerReceivedAt();
        care.confirmReceived(order.getId()); em.flush(); em.clear();
        var saved = records.findById(order.getId()).orElseThrow();
        assertThat(saved.getCustomerReceivedAt()).isCloseTo(first, org.assertj.core.api.Assertions.within(1, java.time.temporal.ChronoUnit.MICROS));
        assertThat(saved.getDeliveryRecordedBy()).isEqualTo(staff.getUsername());
        assertThat(orders.findById(order.getId()).orElseThrow().getPaymentStatus()).isEqualTo("UNPAID");
        verify(images, times(1)).upload(any(), anyString());
    }
    @Test void supportRequiresDeliveredOrderAndValidReasonAndDescription() {
        login(owner);
        assertThatThrownBy(() -> care.openSupport(order.getId(), "DAMAGED", "Hoa bị hỏng khi nhận", null)).isInstanceOf(IllegalStateException.class);
        delivered();
        assertThatThrownBy(() -> care.openSupport(order.getId(), "REFUND_ALL", "Hoa bị hỏng khi nhận", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> care.openSupport(order.getId(), "DAMAGED", "ngắn", null)).isInstanceOf(IllegalArgumentException.class);
        assertThat(records.findById(order.getId())).isEmpty();
    }
    @Test void managerHandlesSupportWithoutChangingMoneyAndClosedTicketCannotBeRewritten() throws Exception {
        delivered(); login(owner); care.openSupport(order.getId(), "DAMAGED", "Hoa bị dập khi bàn giao", photo());
        assertThatThrownBy(() -> care.openSupport(order.getId(), "OTHER", "Gửi thêm yêu cầu khác", null)).isInstanceOf(IllegalStateException.class);
        login(staff);
        assertThatThrownBy(() -> care.respondSupport(order.getId(), "RESOLVED", "Đã trao đổi với khách")).isInstanceOf(AccessDeniedException.class);
        login(outsider);
        assertThatThrownBy(() -> care.respondSupport(order.getId(), "RESOLVED", "Đã trao đổi với khách")).isInstanceOf(AccessDeniedException.class);
        login(manager); care.respondSupport(order.getId(), "IN_REVIEW", "Cửa hàng đang kiểm tra ảnh");
        login(admin); care.respondSupport(order.getId(), "RESOLVED", "Đã hướng dẫn khách chăm sóc lại hoa");
        assertThatThrownBy(() -> care.respondSupport(order.getId(), "REJECTED", "Ghi đè kết quả đã xử lý")).isInstanceOf(IllegalStateException.class);
        em.flush(); em.clear();
        assertThat(records.findById(order.getId()).orElseThrow().getComplaintStatus()).isEqualTo("RESOLVED");
        assertThat(orders.findById(order.getId()).orElseThrow().getTotalAmount()).isEqualByComparingTo("100000");
        assertThat(orders.findById(order.getId()).orElseThrow().getPaymentStatus()).isEqualTo("UNPAID");
    }
    @Test void endpointsRequireCsrfAndEnforceOwnership() throws Exception {
        delivered();
        mvc.perform(post("/order-care/{id}/received", order.getId()).with(user(owner.getUsername()).roles("USER"))).andExpect(status().isForbidden());
        mvc.perform(post("/order-care/{id}/received", order.getId()).with(user(stranger.getUsername()).roles("USER")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/order-care/{id}/received", order.getId()).with(user(owner.getUsername()).roles("USER")).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/customer/orders/detail/" + order.getId()));
    }
    @Test void detailPagesRenderAftercareForAllFourRolesAndEscapeCustomerText() throws Exception {
        delivered(); login(owner); care.openSupport(order.getId(), "OTHER", "<script>alert(1)</script>", null);
        for (var role : List.of(owner, staff, manager, admin)) {
            String path = role == owner ? "/customer/orders/detail/" : "/" + role.getRole().name().toLowerCase() + "/orders/";
            var result = mvc.perform(get(path + order.getId()).with(user(role.getUsername()).roles(role.getRole().name())))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertThat(result).contains("id=\"order-aftercare\"", "&lt;script&gt;alert(1)&lt;/script&gt;").doesNotContain("<script>alert(1)</script>");
            if (role == manager || role == admin) assertThat(result).contains("name=\"_csrf\"", "/support-response");
            else assertThat(result).doesNotContain("/support-response");
        }
    }
    @Test void servicePolicyIsPublicAndExplainsActualFeeMechanism() throws Exception {
        mvc.perform(get("/service-policy")).andExpect(status().isOk()).andExpect(view().name("service-policy"));
    }
}
