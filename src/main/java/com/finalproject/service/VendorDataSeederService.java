package com.finalproject.service;

import com.finalproject.entity.*;
import com.finalproject.enums.Role;
import com.finalproject.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service khởi tạo dữ liệu mẫu phong phú và toàn diện cho vai trò MANAGER (Người bán / Đối tác cửa hàng).
 * Đảm bảo mọi mục trong thanh điều hướng của Vendor/Manager đều có dữ liệu thực tế và sinh động:
 * 1. Tổng Quan Chi Nhánh & Báo Cáo Doanh Thu (Doanh thu, biểu đồ ngày/giờ, top sản phẩm bán chạy, đơn gần đây)
 * 2. Quản Lý Đơn Hàng (Đơn đang xử lý: PENDING, CONFIRMED, PREPARING, DELIVERING, DELIVERED; Đơn đã xử lý: COMPLETED, CANCELLED)
 * 3. Kho Mẫu Hoa & Tồn Kho Chi Nhánh
 * 4. Quản Lý Nhân Viên (Chủ shop, Đối tác nhượng quyền, Thợ cắm hoa, Nhân viên tiếp nhận đơn, Thủ kho)
 * 5. Ví Cửa Hàng (Số dư ví eWallet, lịch sử thanh toán đơn hàng, phí chiết khấu sàn, rút tiền về ngân hàng)
 * 6. Chương Trình Khuyến Mãi (Mã voucher riêng của chi nhánh: phần trăm, giảm cố định, freeship)
 * 7. Danh Sách Cửa Hàng (Cửa hàng sở hữu và Cửa hàng đối tác liên kết)
 * 8. Thông Tin Cửa Hàng (Ảnh đại diện, ảnh bìa, giới thiệu, hotline, địa chỉ, đánh giá)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VendorDataSeederService {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final StoreMembershipRepository storeMembershipRepository;
    private final StoreLevelRepository storeLevelRepository;
    private final CommissionRepository commissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final DemoStaffSeeder staffSeeder;
    private final DemoVoucherSeeder voucherSeeder;
    private final DemoOrderSeeder orderSeeder;

    @Transactional
    public void seedVendorDemoData() {
        if (userRepository.findByUsername("vendor_flower").isPresent()
                || userRepository.findByUsername("tranduclong").isPresent()
                || userRepository.findByEmail("vendor@rentalshop.vn").isPresent()) return;
        log.info("--- Bắt đầu khởi tạo dữ liệu mẫu cho vai trò MANAGER ---");

        final Store storeB = resolveVendorStore();
        if (storeB == null) {
            log.warn("Chưa có cửa hàng nào trong hệ thống để gán dữ liệu Vendor.");
            return;
        }

        if (storeB.getOwner() != null) return;
        // Cập nhật thông tin chi nhánh Phú Mỹ Hưng đầy đủ, sang trọng và chuẩn thương hiệu
        storeB.setName("Chi Nhánh Phú Mỹ Hưng (Quận 7)");
        storeB.setAddress("456 Nguyễn Thị Thập, Tân Hưng, Quận 7, TP.HCM");
        storeB.setPhone("0283333444");
        storeB.setBio("Chi nhánh hoa tươi cao cấp Phú Mỹ Hưng - Quận 7 trực thuộc chuỗi Bloom Flower. Chúng tôi chuyên cung cấp hoa nhập khẩu Hà Lan, Ecuador, hoa tươi Đà Lạt cắt cành trong ngày, lẵng hoa sinh nhật, kệ hoa khai trương sang trọng và dịch vụ điện hoa hỏa tốc trong 2 giờ.");
        storeB.setAvatar("https://images.unsplash.com/photo-1563245372-f21724e3856d?w=400&q=80");
        storeB.setCover("https://images.unsplash.com/photo-1526047932273-341f2a7631f9?w=1200&q=80");
        storeB.setStatus(true);
        storeB.setIsOpen(true);
        storeB.setRating(4.9);
        storeB.setPoint(1250);
        if (storeB.getEWallet() == null || storeB.getEWallet().compareTo(new BigDecimal("10000000")) < 0) {
            storeB.setEWallet(new BigDecimal("18650000"));
        }

        // Gán gói hoa hồng thân thiết (8%) và cấp bậc Cửa hàng xuất sắc
        commissionRepository.findAll().stream()
                .filter(c -> c.getName() != null && (c.getName().contains("8") || c.getName().contains("Thân Thiết")))
                .findFirst()
                .ifPresent(storeB::setCommission);

        storeLevelRepository.findAll().stream()
                .filter(sl -> sl.getName() != null && (sl.getName().contains("xuất sắc") || sl.getName().contains("uy tín")))
                .findFirst()
                .ifPresent(storeB::setStoreLevel);

        // 2. Tài khoản quản lý chính và quản lý hỗ trợ; chức vụ tách khỏi họ tên.
        User vendorFlower = userRepository.findByUsername("tranduclong").or(() -> userRepository.findByEmail("vendor@rentalshop.vn")).orElseGet(() -> userRepository.save(User.builder()
                .username("tranduclong")
                .password(passwordEncoder.encode("123456"))
                .fullName("Trần Đức Long")
                .email("vendor@rentalshop.vn")
                .phone("0988776655")
                .role(Role.MANAGER)
                .store(storeB)
                .isActive(true)
                .isEmailActive(true)
                .createdDate(LocalDate.now().minusMonths(6))
                .build()));

        vendorFlower.setRole(Role.MANAGER);
        vendorFlower.addRole(Role.MANAGER);
        vendorFlower.setStore(storeB);
        vendorFlower.setIsActive(true);
        vendorFlower.setIsEmailActive(true);
        if (vendorFlower.getFullName() == null || vendorFlower.getFullName().isBlank()) {
            vendorFlower.setFullName("Trần Đức Long");
        }
        userRepository.save(vendorFlower);

        // Đảm bảo storeB có chủ sở hữu là vendor_flower
        storeB.setOwner(vendorFlower);
        storeRepository.save(storeB);

        // Nhân viên hỗ trợ của chi nhánh, không cấp quyền quản lý.
        User vendorAlias = userRepository.findByUsername("tranminhtuan").or(() -> userRepository.findByEmail("vendor.demo@rentalshop.vn")).orElseGet(() -> userRepository.save(User.builder()
                .username("tranminhtuan")
                .password(passwordEncoder.encode("123456"))
                .fullName("Trần Minh Tuấn")
                .email("vendor.demo@rentalshop.vn")
                .phone("0988776699")
                .role(Role.STAFF)
                .store(storeB)
                .isActive(true)
                .isEmailActive(true)
                .createdDate(LocalDate.now().minusMonths(4))
                .build()));
        vendorAlias.setRole(Role.STAFF);
        vendorAlias.getRoles().remove(Role.MANAGER);
        vendorAlias.addRole(Role.STAFF);
        vendorAlias.setStore(storeB);
        userRepository.save(vendorAlias);


        // 3. Khởi tạo đội ngũ nhân viên chi nhánh Phú Mỹ Hưng (Quản Lý Nhân Viên)
        staffSeeder.seed(storeB, vendorAlias);

        // 4. Khởi tạo mã khuyến mãi riêng cho chi nhánh Phú Mỹ Hưng (Chương Trình Khuyến Mãi)
        voucherSeeder.seed(storeB);

        // 5. Khởi tạo đơn hàng mẫu & lịch sử ví cửa hàng (Đơn Hàng, Báo Cáo Doanh Thu, Ví Cửa Hàng)
        orderSeeder.seed(storeB, vendorFlower);

        log.info("--- Hoàn tất khởi tạo dữ liệu mẫu cho vai trò MANAGER ---");
    }

    private Store resolveVendorStore() {
        Store store = storeRepository.findFirstByNameOrderByIdAsc("Chi Nhánh Phú Mỹ Hưng (Quận 7)")
                .orElseGet(() -> storeRepository.findFirstByNameOrderByIdAsc("Bloom Flower - Chi Nhánh Phú Mỹ Hưng (Quận 7)")
                        .orElse(null));

        if (store == null) {
            List<Store> stores = storeRepository.findAll();
            if (stores.size() > 1) {
                store = stores.get(1);
            } else if (!stores.isEmpty()) {
                store = stores.get(0);
            }
        }
        return store;
    }
}
