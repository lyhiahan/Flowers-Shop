package com.finalproject;

import com.finalproject.config.AdminRoleSanitizer;
import com.finalproject.dto.request.AddToCartRequest;
import com.finalproject.entity.User;
import com.finalproject.enums.OrderType;
import com.finalproject.enums.PaymentMethod;
import com.finalproject.enums.Role;
import com.finalproject.repository.UserRepository;
import com.finalproject.service.CartService;
import com.finalproject.service.OrderService;
import com.finalproject.service.impl.CustomUserDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:role-separation-test;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
@Transactional
@SuppressWarnings("unused")
class AdminCustomerRoleSeparationTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private AdminRoleSanitizer adminRoleSanitizer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("1. Admin khi đăng nhập chỉ nhận ROLE_ADMIN, tuyệt đối KHÔNG có ROLE_CUSTOMER")
    void adminUser_receivesOnlyAdminAuthority_notCustomer() {
        User admin = userRepository.save(User.builder()
                .username("test_admin_sep")
                .password("$2a$10$abcdefghijklmnopqrstuv")
                .email("admin_sep@flower.vn")
                .fullName("Quản Trị Viên Tách Quyền")
                .role(Role.ADMIN)
                .isActive(true)
                .build());

        UserDetails details = customUserDetailsService.loadUserByUsername("test_admin_sep");
        Set<String> authNames = details.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        assertThat(authNames).contains("ROLE_ADMIN");
        assertThat(authNames).doesNotContain("ROLE_CUSTOMER");
    }

    @Test
    @DisplayName("2. Khách hàng, Quản lý, Người bán khi đăng nhập vẫn giữ ROLE_CUSTOMER bình thường")
    void normalUsers_retainCustomerAuthority() {
        User customer = userRepository.save(User.builder()
                .username("test_cust_sep")
                .password("$2a$10$abcdefghijklmnopqrstuv")
                .email("cust_sep@flower.vn")
                .fullName("Khách Hàng Mua Hoa")
                .role(Role.CUSTOMER)
                .isActive(true)
                .build());

        UserDetails custDetails = customUserDetailsService.loadUserByUsername("test_cust_sep");
        Set<String> custAuths = custDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        assertThat(custAuths).contains("ROLE_CUSTOMER");

        User vendor = userRepository.save(User.builder()
                .username("test_vendor_sep")
                .password("$2a$10$abcdefghijklmnopqrstuv")
                .email("vendor_sep@flower.vn")
                .fullName("Người Bán Hoa")
                .role(Role.VENDOR)
                .isActive(true)
                .build());

        UserDetails vendorDetails = customUserDetailsService.loadUserByUsername("test_vendor_sep");
        Set<String> vendorAuths = vendorDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        assertThat(vendorAuths).contains("ROLE_CUSTOMER", "ROLE_VENDOR");
    }

    @Test
    @DisplayName("3. Entity User loại bỏ vai trò CUSTOMER khi gán vai trò ADMIN")
    void userEntity_removesCustomerRole_whenAdminAssigned() {
        User user = User.builder()
                .username("test_promote_admin")
                .role(Role.CUSTOMER)
                .roles(new java.util.HashSet<>(java.util.List.of(Role.CUSTOMER)))
                .build();

        user.addRole(Role.ADMIN);
        user.setRole(Role.ADMIN);

        assertThat(user.getRoles()).doesNotContain(Role.CUSTOMER);
        assertThat(user.hasRole(Role.ADMIN)).isTrue();
        assertThat(user.hasRole(Role.CUSTOMER)).isFalse();
    }

    @Test
    @DisplayName("4. CartService chặn tài khoản Admin thêm hàng vào giỏ và xem giỏ")
    void cartService_blocksAdminFromAddingOrViewingCart() {
        User admin = userRepository.save(User.builder()
                .username("test_admin_cart_block")
                .password("$2a$10$abcdefghijklmnopqrstuv")
                .email("admin_cart@flower.vn")
                .fullName("Admin Thử Giỏ")
                .role(Role.ADMIN)
                .isActive(true)
                .build());

        AddToCartRequest req = new AddToCartRequest();
        req.setProductId(1L);
        req.setOrderType(OrderType.BUY);
        req.setQuantity(1);

        assertThatThrownBy(() -> cartService.addToCart(admin.getUsername(), req))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Quản trị viên");

        assertThatThrownBy(() -> cartService.getCartByUsername(admin.getUsername()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Quản trị viên");

        assertThat(cartService.getCartItemCount(admin.getUsername())).isEqualTo(0);
    }

    @Test
    @DisplayName("5. OrderService chặn tài khoản Admin thực hiện thanh toán/tạo đơn")
    void orderService_blocksAdminFromCheckout() {
        User admin = userRepository.save(User.builder()
                .username("test_admin_checkout_block")
                .password("$2a$10$abcdefghijklmnopqrstuv")
                .email("admin_checkout@flower.vn")
                .fullName("Admin Thử Đặt Hàng")
                .role(Role.ADMIN)
                .isActive(true)
                .build());

        assertThatThrownBy(() -> orderService.checkout(admin.getUsername(), java.util.List.of(), PaymentMethod.COD))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Quản trị viên");
    }

    @Test
    @DisplayName("6. AdminRoleSanitizer tự động rà soát và xóa role CUSTOMER cũ khỏi tài khoản Admin trong DB")
    void adminRoleSanitizer_cleansCustomerRoleFromDatabase() {
        User legacyAdmin = userRepository.save(User.builder()
                .username("legacy_admin")
                .password("$2a$10$abcdefghijklmnopqrstuv")
                .email("legacy_admin@flower.vn")
                .fullName("Admin Cũ Có Cả Khách")
                .role(Role.ADMIN)
                .isActive(true)
                .build());

        // Cố tình ghi thêm vai trò CUSTOMER vào bảng user_roles cho legacyAdmin
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role) VALUES (?, ?)", legacyAdmin.getId(), "CUSTOMER");

        Integer countBefore = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_roles WHERE user_id = ? AND role = 'CUSTOMER'",
                Integer.class, legacyAdmin.getId()
        );
        assertThat(countBefore).isEqualTo(1);

        // Chạy rà soát
        adminRoleSanitizer.run(new DefaultApplicationArguments(new String[0]));

        Integer countAfter = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_roles WHERE user_id = ? AND role = 'CUSTOMER'",
                Integer.class, legacyAdmin.getId()
        );
        assertThat(countAfter).isEqualTo(0);
    }
}
