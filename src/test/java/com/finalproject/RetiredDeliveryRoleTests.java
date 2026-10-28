package com.finalproject;

import com.finalproject.config.RetiredDeliveryRoleCleanup;
import com.finalproject.entity.*;
import com.finalproject.enums.*;
import com.finalproject.repository.*;
import com.finalproject.service.AuthService;
import com.finalproject.service.CustomUserDetailsService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:retired-role-tests;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
class RetiredDeliveryRoleTests {
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired UserRepository users;
    @Autowired StoreRepository stores;
    @Autowired OrderRepository orders;
    @Autowired RoleRequestRepository requests;
    @Autowired RoleAuditLogRepository audits;
    @Autowired RetiredDeliveryRoleCleanup cleanup;
    @Autowired CustomUserDetailsService userDetails;
    @Autowired AuthService auth;
    @Autowired OtpVerificationRepository otps;

    @BeforeAll
    void useExistingDatabaseRoleConstraints() {
        // Represent the old schema before inserting old data; fresh schemas reject retired roles.
        var checks = jdbc.queryForList("SELECT t.TABLE_NAME, t.CONSTRAINT_NAME, c.CHECK_CLAUSE "
                + "FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS t JOIN INFORMATION_SCHEMA.CHECK_CONSTRAINTS c "
                + "ON t.CONSTRAINT_NAME = c.CONSTRAINT_NAME AND t.CONSTRAINT_SCHEMA = c.CONSTRAINT_SCHEMA "
                + "WHERE t.TABLE_NAME IN ('USERS', 'USER_ROLES', 'ROLE_REQUESTS', 'ROLE_AUDIT_LOGS') "
                + "AND t.CONSTRAINT_TYPE = 'CHECK'");
        for (var check : checks) {
            String clause = check.get("CHECK_CLAUSE").toString();
            if (!clause.contains("'USER'")) continue;
            String table = check.get("TABLE_NAME").toString();
            String name = check.get("CONSTRAINT_NAME").toString();
            jdbc.execute("ALTER TABLE " + table + " DROP CONSTRAINT " + name);
            jdbc.execute("ALTER TABLE " + table + " ADD CONSTRAINT " + name + " CHECK ("
                    + clause.replace("'USER'", "'USER', 'SHIPPER'") + ")");
        }
    }

    @Test
    void disablesOldAccountAndKeepsOrdersRequestsAuditAndOtherRoles() {
        User retired = users.save(User.builder().username("retired-delivery").password("encoded")
                .fullName("Tài khoản cũ").email("retired@example.test").role(Role.USER).isActive(true).build());
        retired.getRoles().add(Role.USER);
        User manager = users.save(User.builder().username("manager-kept").password("encoded")
                .fullName("Quản lý").email("manager@example.test").role(Role.MANAGER).isActive(true).build());
        Store store = stores.save(Store.builder().name("Chi nhánh").address("Quận 1").build());
        Order order = orders.save(Order.builder().user(manager).orderType(OrderType.BUY).pickupStore(store)
                .status(OrderStatus.PICKED_UP).legacyDeliveryContact(retired).build());
        RoleRequest request = requests.save(RoleRequest.builder().user(retired).targetRole(Role.MANAGER).build());
        RoleAuditLog audit = audits.save(RoleAuditLog.builder().user(retired).role(Role.MANAGER).action("GRANT").performedBy(manager).build());
        em.flush();
        jdbc.update("UPDATE users SET role = 'SHIPPER' WHERE id = ?", retired.getId());
        jdbc.update("INSERT INTO user_roles(user_id, role) VALUES (?, 'SHIPPER')", retired.getId());
        jdbc.update("UPDATE role_requests SET target_role = 'SHIPPER' WHERE id = ?", request.getId());
        jdbc.update("UPDATE role_audit_logs SET role = 'SHIPPER' WHERE id = ?", audit.getId());
        cleanup.run(null);
        em.clear();
        assertThat(users.count()).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT is_active FROM users WHERE id = ?", Boolean.class, retired.getId())).isFalse();
        assertThat(users.findById(retired.getId()).orElseThrow().getIsActive()).isFalse();
        var details = userDetails.loadUserByUsername("retired-delivery");
        assertThat(details.isEnabled()).isFalse();
        assertThat(details.getAuthorities()).extracting("authority").doesNotContain("ROLE_SHIPPER", "ROLE_MANAGER");
        assertThat(users.findById(manager.getId()).orElseThrow().getIsActive()).isTrue();
        assertThat(users.findById(manager.getId()).orElseThrow().getRole()).isEqualTo(Role.MANAGER);
        Order saved = orders.findById(order.getId()).orElseThrow();
        assertThat(saved.getLegacyDeliveryContact().getId()).isEqualTo(retired.getId());
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.PICKED_UP);
        assertThat(orders.existsByLegacyDeliveryContactId(retired.getId())).isTrue();
        RoleRequest savedRequest = requests.findById(request.getId()).orElseThrow();
        assertThat(savedRequest.getStatus()).isEqualTo(RoleRequestStatus.REJECTED);
        assertThat(savedRequest.getTargetRoleDisplayName()).isEqualTo("Vai trò đã ngừng sử dụng");
        assertThat(audits.findById(audit.getId())).hasValueSatisfying(savedAudit -> {
            assertThat(savedAudit.getStoredRole()).isEqualTo("SHIPPER");
            assertThat(savedAudit.getRoleDisplayName()).isEqualTo("Vai trò đã ngừng sử dụng");
        });
        User savedUser = users.findById(retired.getId()).orElseThrow();
        for (OtpType type : new OtpType[] {OtpType.ACTIVATE_ACCOUNT, OtpType.RESET_PASSWORD}) {
            otps.save(OtpVerification.builder().user(savedUser).code("123456").type(type)
                    .expiredAt(java.time.LocalDateTime.now().plusMinutes(5)).build());
        }
        em.flush();
        assertThat(auth.verifyOtp(savedUser.getEmail(), "123456")).isFalse();
        assertThat(auth.verifyForgotPasswordOtp(savedUser.getEmail(), "123456")).isFalse();
        assertThatThrownBy(() -> auth.forgotPassword(savedUser.getEmail())).hasMessageContaining("vô hiệu hóa");
        assertThatThrownBy(() -> auth.resetPassword(savedUser.getEmail(), "123456", "new-password"))
                .hasMessageContaining("vô hiệu hóa");
        assertThat(jdbc.queryForObject("SELECT role FROM users WHERE id = ?", String.class, retired.getId())).isEqualTo("SHIPPER");
        // Saving an unrelated profile field rewrites the obsolete enum value, but must retain retirement.
        savedUser.setFullName("Hồ sơ đã cập nhật"); users.saveAndFlush(savedUser); em.clear();
        User reloaded = users.findById(retired.getId()).orElseThrow();
        assertThat(reloaded.isRetiredDeliveryAccount()).isTrue();
        assertThat(userDetails.loadUserByUsername(reloaded.getUsername()).isEnabled()).isFalse();
        assertThat(auth.verifyOtp(reloaded.getEmail(), "123456")).isFalse();
        assertThatThrownBy(() -> auth.resetPassword(reloaded.getEmail(), "123456", "new-password")).hasMessageContaining("vô hiệu hóa");
        cleanup.run(null);
        assertThat(users.count()).isEqualTo(2);
        assertThat(orders.count()).isEqualTo(1);
    }
}
