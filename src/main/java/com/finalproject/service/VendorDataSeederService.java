package com.finalproject.service;

import com.finalproject.entity.*;
import com.finalproject.enums.OrderStatus;
import com.finalproject.enums.OrderType;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Service khởi tạo dữ liệu mẫu phong phú và toàn diện cho vai trò VENDOR (Người bán / Đối tác cửa hàng).
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
    private final ProductRepository productRepository;
    private final StoreInventoryRepository storeInventoryRepository;
    private final VoucherRepository voucherRepository;
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final TransactionRepository transactionRepository;
    private final DeliveryRepository deliveryRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void seedVendorDemoData() {
        log.info("--- Bắt đầu khởi tạo dữ liệu mẫu cho vai trò VENDOR ---");

        final Store storeB = resolveVendorStore();
        if (storeB == null) {
            log.warn("Chưa có cửa hàng nào trong hệ thống để gán dữ liệu Vendor.");
            return;
        }

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

        // 2. Tài khoản VENDOR chính: vendor_flower & tài khoản phụ vendor
        User vendorFlower = userRepository.findByUsername("vendor_flower").orElseGet(() -> userRepository.save(User.builder()
                .username("vendor_flower")
                .password(passwordEncoder.encode("123456"))
                .fullName("Trần Đối Tác Vườn Hoa Đà Lạt")
                .email("vendor@rentalshop.vn")
                .phone("0988776655")
                .role(Role.VENDOR)
                .store(storeB)
                .isActive(true)
                .isEmailActive(true)
                .createdDate(LocalDate.now().minusMonths(6))
                .build()));

        vendorFlower.setRole(Role.VENDOR);
        vendorFlower.addRole(Role.VENDOR);
        vendorFlower.setStore(storeB);
        vendorFlower.setIsActive(true);
        vendorFlower.setIsEmailActive(true);
        if (vendorFlower.getFullName() == null || vendorFlower.getFullName().isBlank()) {
            vendorFlower.setFullName("Trần Đối Tác Vườn Hoa Đà Lạt");
        }
        userRepository.save(vendorFlower);

        // Đảm bảo storeB có chủ sở hữu là vendor_flower
        storeB.setOwner(vendorFlower);
        storeRepository.save(storeB);

        // Tài khoản vendor alias (cho trường hợp người dùng gõ username: vendor)
        User vendorAlias = userRepository.findByUsername("vendor").orElseGet(() -> userRepository.save(User.builder()
                .username("vendor")
                .password(passwordEncoder.encode("123456"))
                .fullName("Trần Đối Tác (Vendor)")
                .email("vendor.demo@rentalshop.vn")
                .phone("0988776699")
                .role(Role.VENDOR)
                .store(storeB)
                .isActive(true)
                .isEmailActive(true)
                .createdDate(LocalDate.now().minusMonths(4))
                .build()));
        vendorAlias.setRole(Role.VENDOR);
        vendorAlias.addRole(Role.VENDOR);
        vendorAlias.setStore(storeB);
        userRepository.save(vendorAlias);

        // Thêm liên kết đối tác cho vendor_flower tại chi nhánh Quận 1 (để tab Danh Sách Cửa Hàng có 2 chi nhánh)
        Store storeA = storeRepository.findFirstByNameOrderByIdAsc("Chi Nhánh Bến Thành (Quận 1)").orElse(null);
        if (storeA != null && !storeA.getId().equals(storeB.getId())) {
            if (storeMembershipRepository.findByUserIdAndStoreId(vendorFlower.getId(), storeA.getId()).isEmpty()) {
                storeMembershipRepository.save(StoreMembership.builder()
                        .user(vendorFlower)
                        .store(storeA)
                        .accessRole("PARTNER")
                        .active(true)
                        .joinedAt(LocalDateTime.now().minusMonths(2))
                        .build());
            }
        }

        // 3. Khởi tạo đội ngũ nhân viên chi nhánh Phú Mỹ Hưng (Quản Lý Nhân Viên)
        initStoreStaff(storeB, vendorAlias);

        // 4. Khởi tạo mã khuyến mãi riêng cho chi nhánh Phú Mỹ Hưng (Chương Trình Khuyến Mãi)
        initStoreVouchers(storeB);

        // 5. Khởi tạo đơn hàng mẫu & lịch sử ví cửa hàng (Đơn Hàng, Báo Cáo Doanh Thu, Ví Cửa Hàng)
        initStoreOrdersAndTransactions(storeB, vendorFlower);

        log.info("--- Hoàn tất khởi tạo dữ liệu mẫu cho vai trò VENDOR ---");
    }

    private void initStoreStaff(Store storeB, User partnerVendor) {
        // Đối tác quản lý nhượng quyền (Partner)
        if (storeMembershipRepository.findByUserIdAndStoreId(partnerVendor.getId(), storeB.getId()).isEmpty()) {
            storeMembershipRepository.save(StoreMembership.builder()
                    .user(partnerVendor)
                    .store(storeB)
                    .accessRole("PARTNER")
                    .active(true)
                    .joinedAt(LocalDateTime.now().minusMonths(3))
                    .build());
        }

        // Nhân viên 1: Nghệ nhân cắm hoa chính
        User staff1 = userRepository.findByUsername("staff_q7_flt").orElseGet(() -> userRepository.save(User.builder()
                .username("staff_q7_flt")
                .password(passwordEncoder.encode("123456"))
                .fullName("Nguyễn Thị Mai Lan - Nghệ Nhân Cắm Hoa Chính")
                .email("mailan.florist@rentalshop.vn")
                .phone("0938112233")
                .role(Role.STAFF)
                .store(storeB)
                .isActive(true)
                .isEmailActive(true)
                .createdDate(LocalDate.now().minusMonths(5))
                .build()));
        staff1.setStore(storeB);
        staff1.setRole(Role.STAFF);
        staff1.addRole(Role.STAFF);
        userRepository.save(staff1);
        if (storeMembershipRepository.findByUserIdAndStoreId(staff1.getId(), storeB.getId()).isEmpty()) {
            storeMembershipRepository.save(StoreMembership.builder()
                    .user(staff1)
                    .store(storeB)
                    .accessRole("STAFF")
                    .active(true)
                    .joinedAt(LocalDateTime.now().minusMonths(5))
                    .build());
        }

        // Nhân viên 2: Tiếp nhận & Điều phối đơn hàng
        User staff2 = userRepository.findByUsername("staff_q7_order").orElseGet(() -> userRepository.save(User.builder()
                .username("staff_q7_order")
                .password(passwordEncoder.encode("123456"))
                .fullName("Trần Bảo Phúc - Tiếp Nhận & Điều Phối Đơn Hàng")
                .email("baophuc.orders@rentalshop.vn")
                .phone("0938223344")
                .role(Role.STAFF)
                .store(storeB)
                .isActive(true)
                .isEmailActive(true)
                .createdDate(LocalDate.now().minusMonths(3))
                .build()));
        staff2.setStore(storeB);
        staff2.setRole(Role.STAFF);
        staff2.addRole(Role.STAFF);
        userRepository.save(staff2);
        if (storeMembershipRepository.findByUserIdAndStoreId(staff2.getId(), storeB.getId()).isEmpty()) {
            storeMembershipRepository.save(StoreMembership.builder()
                    .user(staff2)
                    .store(storeB)
                    .accessRole("STAFF")
                    .active(true)
                    .joinedAt(LocalDateTime.now().minusMonths(3))
                    .build());
        }

        // Nhân viên 3: Thủ kho hoa tươi & nguyên phụ liệu
        User staff3 = userRepository.findByUsername("staff_q7_inv").orElseGet(() -> userRepository.save(User.builder()
                .username("staff_q7_inv")
                .password(passwordEncoder.encode("123456"))
                .fullName("Lê Hoàng Khôi - Thủ Kho & Kiểm Soát Hoa Tươi")
                .email("hoangkhoi.stock@rentalshop.vn")
                .phone("0938334455")
                .role(Role.STAFF)
                .store(storeB)
                .isActive(true)
                .isEmailActive(true)
                .createdDate(LocalDate.now().minusMonths(2))
                .build()));
        staff3.setStore(storeB);
        staff3.setRole(Role.STAFF);
        staff3.addRole(Role.STAFF);
        userRepository.save(staff3);
        if (storeMembershipRepository.findByUserIdAndStoreId(staff3.getId(), storeB.getId()).isEmpty()) {
            storeMembershipRepository.save(StoreMembership.builder()
                    .user(staff3)
                    .store(storeB)
                    .accessRole("STAFF")
                    .active(true)
                    .joinedAt(LocalDateTime.now().minusMonths(2))
                    .build());
        }
    }

    private void initStoreVouchers(Store storeB) {
        if (!voucherRepository.existsByCode("Q7PHUMYHUNG")) {
            voucherRepository.save(Voucher.builder()
                    .code("Q7PHUMYHUNG")
                    .name("Ưu Đãi 15% Hoa Tươi Khai Trương Phú Mỹ Hưng")
                    .discountType("PERCENT")
                    .discountValue(new BigDecimal("15"))
                    .minOrderValue(new BigDecimal("300000"))
                    .maxDiscountAmount(new BigDecimal("150000"))
                    .store(storeB)
                    .quantity(150)
                    .usedCount(24)
                    .startDate(LocalDate.now().minusDays(15))
                    .endDate(LocalDate.now().plusMonths(3))
                    .isActive(true)
                    .createdAt(LocalDateTime.now().minusDays(15))
                    .build());
        }

        if (!voucherRepository.existsByCode("Q7FREESHIP")) {
            voucherRepository.save(Voucher.builder()
                    .code("Q7FREESHIP")
                    .name("Freeship Đơn Hoa Tươi Nội Thành Quận 7")
                    .discountType("FREE_SHIP")
                    .discountValue(new BigDecimal("35000"))
                    .minOrderValue(new BigDecimal("250000"))
                    .store(storeB)
                    .quantity(100)
                    .usedCount(42)
                    .startDate(LocalDate.now().minusDays(10))
                    .endDate(LocalDate.now().plusMonths(2))
                    .isActive(true)
                    .createdAt(LocalDateTime.now().minusDays(10))
                    .build());
        }

        if (!voucherRepository.existsByCode("Q7TRIAN50K")) {
            voucherRepository.save(Voucher.builder()
                    .code("Q7TRIAN50K")
                    .name("Tri Ân Khách Hàng Thân Thiết Q7 - Giảm 50K")
                    .discountType("FIXED_AMOUNT")
                    .discountValue(new BigDecimal("50000"))
                    .minOrderValue(new BigDecimal("500000"))
                    .store(storeB)
                    .quantity(80)
                    .usedCount(15)
                    .startDate(LocalDate.now().minusDays(5))
                    .endDate(LocalDate.now().plusMonths(2))
                    .isActive(true)
                    .createdAt(LocalDateTime.now().minusDays(5))
                    .build());
        }

        if (!voucherRepository.existsByCode("Q7VIP100K")) {
            voucherRepository.save(Voucher.builder()
                    .code("Q7VIP100K")
                    .name("Đặc Quyền Khách Hàng VIP Quận 7 - Giảm 100K")
                    .discountType("FIXED_AMOUNT")
                    .discountValue(new BigDecimal("100000"))
                    .minOrderValue(new BigDecimal("1000000"))
                    .store(storeB)
                    .quantity(50)
                    .usedCount(8)
                    .startDate(LocalDate.now().minusDays(2))
                    .endDate(LocalDate.now().plusMonths(3))
                    .isActive(true)
                    .createdAt(LocalDateTime.now().minusDays(2))
                    .build());
        }
    }

    private void initStoreOrdersAndTransactions(Store storeB, User vendorFlower) {
        // Lấy hoặc tạo tài khoản khách hàng để đặt đơn
        User customer1 = userRepository.findByUsername("khachhang").orElseGet(() -> userRepository.save(User.builder()
                .username("khachhang")
                .password(passwordEncoder.encode("123456"))
                .fullName("Lê Khách Hàng")
                .email("khachhang@gmail.com")
                .phone("0909999888")
                .role(Role.CUSTOMER)
                .isActive(true)
                .isEmailActive(true)
                .createdDate(LocalDate.now().minusMonths(6))
                .build()));

        User customer2 = userRepository.findByUsername("khachhang_q7").orElseGet(() -> userRepository.save(User.builder()
                .username("khachhang_q7")
                .password(passwordEncoder.encode("123456"))
                .fullName("Nguyễn Ngọc Bích Trâm")
                .email("bichtram.flower@gmail.com")
                .phone("0908776655")
                .address("Khu biệt thự Chateau, Phú Mỹ Hưng, Quận 7, TP.HCM")
                .role(Role.CUSTOMER)
                .isActive(true)
                .isEmailActive(true)
                .createdDate(LocalDate.now().minusMonths(3))
                .build()));

        User customer3 = userRepository.findByUsername("khachhang_dn").orElseGet(() -> userRepository.save(User.builder()
                .username("khachhang_dn")
                .password(passwordEncoder.encode("123456"))
                .fullName("Công Ty Cổ Phần Công Nghệ Nam Sài Gòn")
                .email("contact@namsaigontech.vn")
                .phone("02873009988")
                .address("Tòa nhà MapleTree, 1060 Nguyễn Văn Linh, Tân Phong, Quận 7, TP.HCM")
                .role(Role.CUSTOMER)
                .isActive(true)
                .isEmailActive(true)
                .createdDate(LocalDate.now().minusMonths(4))
                .build()));

        List<Order> createdPaidOrders = new ArrayList<>();

        if (orderRepository.findByPickupStoreIdOrderByCreatedAtDesc(storeB.getId()).isEmpty()) {
            List<Product> storeProducts = productRepository.findByStoreIdOrderByCreatedAtDesc(storeB.getId());
            if (storeProducts.isEmpty()) {
                storeProducts = productRepository.findAll();
            }

            Product prodPastel = findOrCreateProduct(storeProducts, storeB, "Hộp Hoa Sinh Nhật Tone Hồng Pastel 'Thiên Thần Ngọt Ngào'", new BigDecimal("650000"), "https://images.unsplash.com/photo-1561181286-d3fee7d55364?w=800&q=80");
            Product prodTulip = findOrCreateProduct(storeProducts, storeB, "Bó Hoa Tulip Hà Lan Tone Hồng Pastel Hàn Quốc", new BigDecimal("950000"), "https://images.unsplash.com/photo-1520763185298-1b434c919102?w=800&q=80");
            Product prodWedding = findOrCreateProduct(storeProducts, storeB, "Bó Hoa Cưới Cầm Tay Cô Dâu Tone Trắng Tinh Khôi", new BigDecimal("890000"), "https://images.unsplash.com/photo-1533616688419-b7a585564566?w=800&q=80");
            Product prodLanHoDiep = findOrCreateProduct(storeProducts, storeB, "Chậu Lan Hồ Điệp Vàng Hoàng Yến 5 Cành 'Kim Ngân Đắc Lộc'", new BigDecimal("2300000"), "https://images.unsplash.com/photo-1525310072745-f49212b5ac6d?w=800&q=80");
            Product prodKhaiTruong = findOrCreateProduct(storeProducts, storeB, "Kệ Hoa Khai Trương Hiện Đại Tone Đỏ Hồng 'Vạn Sự Hanh Thông'", new BigDecimal("1950000"), "https://images.unsplash.com/photo-1526047932273-341f2a7631f9?w=800&q=80");
            Product prodCamTuCau = findOrCreateProduct(storeProducts, storeB, "Bình Hoa Cẩm Tú Cầu Xanh & Hoa Baby Decor Phòng Khách", new BigDecimal("690000"), "https://images.unsplash.com/photo-1582794543139-8ac9cb0f7b11?w=800&q=80");
            Product prodGioSinhNhat = findOrCreateProduct(storeProducts, storeB, "Giỏ Hoa Sinh Nhật 'Thanh Xuân Rực Rỡ' Phối Hoa Baby", new BigDecimal("720000"), "https://images.unsplash.com/photo-1563245372-f21724e3856d?w=800&q=80");

            updateInventory(storeB, prodPastel, 45);
            updateInventory(storeB, prodTulip, 30);
            updateInventory(storeB, prodWedding, 25);
            updateInventory(storeB, prodLanHoDiep, 15);
            updateInventory(storeB, prodKhaiTruong, 20);
            updateInventory(storeB, prodCamTuCau, 35);
            updateInventory(storeB, prodGioSinhNhat, 40);

            Long deliveryUnitId = deliveryRepository.findAll().stream().map(Delivery::getId).findFirst().orElse(1L);
            LocalDateTime now = LocalDateTime.now();

            // 1. ĐƠN CHỜ TIẾP NHẬN (PENDING)
            createOrder(storeB, customer2, OrderStatus.PENDING, "UNPAID", "COD",
                    new BigDecimal("675000"), new BigDecimal("25000"), BigDecimal.ZERO, deliveryUnitId,
                    now.minusMinutes(18), "Chị Ngọc Mai", "0903112233",
                    "Chung cư Scenic Valley 1, Phú Mỹ Hưng, Quận 7, TP.HCM",
                    "Chúc chị yêu sinh nhật vui vẻ, luôn trẻ đẹp và thành công trong công việc!",
                    List.of(new ItemSpec(prodPastel, 1, prodPastel.getPriceBuy())));

            createOrder(storeB, customer1, OrderStatus.PENDING, "PAID", "VNPAY",
                    new BigDecimal("975000"), new BigDecimal("25000"), BigDecimal.ZERO, deliveryUnitId,
                    now.minusMinutes(42), "Cô giáo Thu Hà", "0912445566",
                    "Trường Quốc Tế Nam Sài Gòn (SSIS), Nguyễn Đức Cảnh, Tân Phong, Quận 7",
                    "Kính chúc cô giáo luôn dồi dào sức khỏe, hạnh phúc và tràn đầy nhiệt huyết!",
                    List.of(new ItemSpec(prodTulip, 1, prodTulip.getPriceBuy())));

            // 2. ĐƠN ĐÃ XÁC NHẬN (CONFIRMED)
            createOrder(storeB, customer3, OrderStatus.CONFIRMED, "PAID", "VNPAY",
                    new BigDecimal("765000"), new BigDecimal("45000"), BigDecimal.ZERO, deliveryUnitId,
                    now.minusHours(2).minusMinutes(10), "Anh Tuấn Giám Đốc", "0909887766",
                    "Tòa nhà Crescent Plaza, 105 Tôn Dật Tiên, Tân Phú, Quận 7",
                    "Công ty Nam Sài Gòn chúc mừng sinh nhật Sếp Tuấn, chúc Sếp vạn sự hanh thông!",
                    List.of(new ItemSpec(prodGioSinhNhat, 1, prodGioSinhNhat.getPriceBuy())));

            createOrder(storeB, customer2, OrderStatus.CONFIRMED, "PAID", "MOMO",
                    new BigDecimal("935000"), new BigDecimal("45000"), BigDecimal.ZERO, deliveryUnitId,
                    now.minusHours(3).minusMinutes(25), "Cô dâu Phương Uyên", "0934556677",
                    "Trung tâm hội nghị tiệc cưới Crystal Palace, Nguyễn Lương Bằng, Quận 7",
                    "Trăm năm hạnh phúc, mãi mãi ngọt ngào bên nhau nhé Uyên!",
                    List.of(new ItemSpec(prodWedding, 1, prodWedding.getPriceBuy())));

            // 3. ĐƠN ĐANG CHUẨN BỊ & ĐANG GIAO (PREPARING, DELIVERING)
            createOrder(storeB, customer3, OrderStatus.PREPARING, "PAID", "VNPAY",
                    new BigDecimal("2000000"), new BigDecimal("50000"), BigDecimal.ZERO, deliveryUnitId,
                    now.minusHours(4).minusMinutes(15), "Showroom Audi Phú Mỹ Hưng", "02854112233",
                    "Tân Trào, Tân Phú, Quận 7, TP.HCM",
                    "Chúc mừng khai trương đại hồng phát, tấn tài tấn lộc!",
                    List.of(new ItemSpec(prodKhaiTruong, 1, prodKhaiTruong.getPriceBuy())));

            createOrder(storeB, customer1, OrderStatus.DELIVERING, "PAID", "VNPAY",
                    new BigDecimal("715000"), new BigDecimal("25000"), BigDecimal.ZERO, deliveryUnitId,
                    now.minusHours(5).minusMinutes(30), "Mẹ Hoàng Oanh", "0902334455",
                    "128 Đường số 79, Phường Tân Quy, Quận 7, TP.HCM",
                    "Con chúc Mẹ yêu luôn mạnh khỏe, bình an và hạnh phúc bên con cháu!",
                    List.of(new ItemSpec(prodCamTuCau, 1, prodCamTuCau.getPriceBuy())));

            // 4. ĐƠN ĐÃ GIAO HÔM NAY (DELIVERED)
            Order oToday1 = createOrder(storeB, customer3, OrderStatus.DELIVERED, "PAID", "VNPAY",
                    new BigDecimal("2350000"), new BigDecimal("50000"), BigDecimal.ZERO, deliveryUnitId,
                    now.minusHours(7).minusMinutes(15), "Ngân Hàng Shinhan Bank - Chi Nhánh PMH", "02854138899",
                    "Nguyễn Khắc Viện, Tân Phú, Quận 7, TP.HCM",
                    "Kính chúc đối tác Shinhan Bank luôn thịnh vượng và ngày càng phát triển vượt bậc!",
                    List.of(new ItemSpec(prodLanHoDiep, 1, prodLanHoDiep.getPriceBuy())));
            createdPaidOrders.add(oToday1);

            Order oToday2 = createOrder(storeB, customer2, OrderStatus.DELIVERED, "PAID", "MOMO",
                    new BigDecimal("1625000"), new BigDecimal("25000"), BigDecimal.ZERO, deliveryUnitId,
                    now.minusHours(9).minusMinutes(30), "Em gái Trâm Anh", "0909112233",
                    "Riverside Residence, Nguyễn Lương Bằng, Phú Mỹ Hưng, Quận 7",
                    "Chúc mừng em gái hoàn thành xuất sắc đồ án tốt nghiệp cử nhân!",
                    List.of(new ItemSpec(prodPastel, 1, prodPastel.getPriceBuy()),
                            new ItemSpec(prodTulip, 1, prodTulip.getPriceBuy())));
            createdPaidOrders.add(oToday2);

            // 5. ĐƠN ĐÃ GIAO HÔM QUA (DELIVERED)
            Order oYest1 = createOrder(storeB, customer1, OrderStatus.DELIVERED, "PAID", "VNPAY",
                    new BigDecimal("745000"), new BigDecimal("25000"), BigDecimal.ZERO, deliveryUnitId,
                    now.minusDays(1).withHour(15).withMinute(30), "Bạn Thảo My", "0933445566",
                    "Chung cư M-One Nam Sài Gòn, Bế Văn Cấm, Tân Kiểng, Quận 7",
                    "Chúc mừng tân gia nhà mới ngập tràn niềm vui và may mắn!",
                    List.of(new ItemSpec(prodGioSinhNhat, 1, prodGioSinhNhat.getPriceBuy())));
            createdPaidOrders.add(oYest1);

            Order oYest2 = createOrder(storeB, customer3, OrderStatus.DELIVERED, "PAID", "VNPAY",
                    new BigDecimal("915000"), new BigDecimal("25000"), BigDecimal.ZERO, deliveryUnitId,
                    now.minusDays(1).withHour(10).withMinute(15), "Bác Sĩ Trưởng Khoa", "0908223344",
                    "Bệnh viện FV (Pháp Việt), 6 Nguyễn Lương Bằng, Tân Phú, Quận 7",
                    "Gia đình bệnh nhân xin chân thành cảm ơn y bác sĩ đã tận tình cứu chữa!",
                    List.of(new ItemSpec(prodWedding, 1, prodWedding.getPriceBuy())));
            createdPaidOrders.add(oYest2);

            // 6. ĐƠN HOÀN THÀNH CÁC NGÀY TRƯỚC (COMPLETED)
            Order oPast2 = createOrder(storeB, customer3, OrderStatus.COMPLETED, "PAID", "VNPAY",
                    new BigDecimal("2000000"), new BigDecimal("50000"), BigDecimal.ZERO, deliveryUnitId,
                    now.minusDays(2).withHour(14).withMinute(20), "Công Ty Bất Động Sản Hưng Phát", "02837755666",
                    "Huỳnh Tấn Phát, Phú Mỹ, Quận 7, TP.HCM",
                    "Chúc mừng khai trương trụ sở mới, làm ăn phát đạt, tiền vào như nước!",
                    List.of(new ItemSpec(prodKhaiTruong, 1, prodKhaiTruong.getPriceBuy())));
            createdPaidOrders.add(oPast2);

            Order oPast3 = createOrder(storeB, customer2, OrderStatus.COMPLETED, "PAID", "MOMO",
                    new BigDecimal("2325000"), new BigDecimal("25000"), BigDecimal.ZERO, deliveryUnitId,
                    now.minusDays(3).withHour(11).withMinute(45), "Bà Nội", "0903556677",
                    "Biệt thự Mỹ Gia, Phú Mỹ Hưng, Quận 7",
                    "Kính chúc Bà thượng thọ an khang, sống lâu trăm tuổi cùng con cháu!",
                    List.of(new ItemSpec(prodLanHoDiep, 1, prodLanHoDiep.getPriceBuy())));
            createdPaidOrders.add(oPast3);

            Order oPast4 = createOrder(storeB, customer1, OrderStatus.COMPLETED, "PAID", "VNPAY",
                    new BigDecimal("675000"), new BigDecimal("25000"), BigDecimal.ZERO, deliveryUnitId,
                    now.minusDays(4).withHour(16).withMinute(10), "Bạn gái Diễm Hương", "0908889900",
                    "Sky Garden 3, Phú Mỹ Hưng, Quận 7, TP.HCM",
                    "Kỷ niệm 2 năm bên nhau, cảm ơn em đã luôn là hậu phương ngọt ngào của anh!",
                    List.of(new ItemSpec(prodPastel, 1, prodPastel.getPriceBuy())));
            createdPaidOrders.add(oPast4);

            Order oPast5 = createOrder(storeB, customer2, OrderStatus.COMPLETED, "PAID", "VNPAY",
                    new BigDecimal("715000"), new BigDecimal("25000"), BigDecimal.ZERO, deliveryUnitId,
                    now.minusDays(5).withHour(9).withMinute(30), "Chị Thảo Nguyên", "0918223344",
                    "Star Hill, Tân Phú, Quận 7, TP.HCM",
                    "Chúc chị sinh nhật vui vẻ, gặt hái thêm nhiều thành công mới trong cuộc sống!",
                    List.of(new ItemSpec(prodCamTuCau, 1, prodCamTuCau.getPriceBuy())));
            createdPaidOrders.add(oPast5);

            Order oPast6 = createOrder(storeB, customer3, OrderStatus.COMPLETED, "PAID", "VNPAY",
                    new BigDecimal("915000"), new BigDecimal("25000"), BigDecimal.ZERO, deliveryUnitId,
                    now.minusDays(6).withHour(13).withMinute(15), "Khách Sạn MerPerle Crystal Palace", "02854118899",
                    "Nguyễn Lương Bằng, Quận 7, TP.HCM",
                    "Chúc mừng sự kiện Gala Dinner đối tác thành công mỹ mãn!",
                    List.of(new ItemSpec(prodWedding, 1, prodWedding.getPriceBuy())));
            createdPaidOrders.add(oPast6);

            // 7. ĐƠN ĐÃ HỦY (CANCELLED)
            createOrder(storeB, customer1, OrderStatus.CANCELLED, "UNPAID", "COD",
                    new BigDecimal("1975000"), new BigDecimal("25000"), BigDecimal.ZERO, deliveryUnitId,
                    now.minusDays(3).withHour(8).withMinute(10), "Anh Hoàng", "0909123456",
                    "Tân Mỹ, Tân Phú, Quận 7, TP.HCM",
                    "Chúc mừng khai trương hồng phát!",
                    List.of(new ItemSpec(prodKhaiTruong, 1, prodKhaiTruong.getPriceBuy())));
        } else {
            createdPaidOrders = orderRepository.findByPickupStoreIdOrderByCreatedAtDesc(storeB.getId()).stream()
                    .filter(o -> "PAID".equals(o.getPaymentStatus()))
                    .toList();
        }

        // 8. Khởi tạo lịch sử giao dịch ví cửa hàng (Ví Cửa Hàng)
        initStoreTransactions(storeB, vendorFlower, createdPaidOrders);
    }

    private void initStoreTransactions(Store storeB, User vendorFlower, List<Order> paidOrders) {
        if (transactionRepository.findByStoreIdOrderByCreatedAtDesc(storeB.getId()).size() >= 5) {
            return;
        }

        BigDecimal commRate = new BigDecimal("0.08"); // 8% hoa hồng sàn

        for (Order order : paidOrders) {
            if (transactionRepository.findAllByOrderIdAndType(order.getId(), "ORDER_PAYMENT").isEmpty()) {
                transactionRepository.save(Transaction.builder()
                        .store(storeB)
                        .user(order.getUser())
                        .order(order)
                        .isUp(true)
                        .amount(order.getTotalAmount())
                        .type("ORDER_PAYMENT")
                        .status("COMPLETED")
                        .description("Nhận thanh toán đơn hàng hoa tươi #" + order.getId() + " (" + order.getRecipientName() + ")")
                        .createdAt(order.getCreatedAt().plusMinutes(5))
                        .build());

                BigDecimal commFee = order.getTotalAmount().multiply(commRate).setScale(0, java.math.RoundingMode.HALF_UP);
                transactionRepository.save(Transaction.builder()
                        .store(storeB)
                        .user(vendorFlower)
                        .order(order)
                        .isUp(false)
                        .amount(commFee)
                        .type("COMMISSION_FEE")
                        .status("COMPLETED")
                        .description("Phí hoa hồng sàn 8% áp dụng cho đơn hàng #" + order.getId())
                        .createdAt(order.getCreatedAt().plusMinutes(6))
                        .build());
            }
        }

        transactionRepository.save(Transaction.builder()
                .store(storeB)
                .user(vendorFlower)
                .isUp(false)
                .amount(new BigDecimal("5000000"))
                .type("WITHDRAWAL")
                .status("COMPLETED")
                .description("Rút tiền doanh thu tuần về tài khoản Vietcombank - STK 0123456789 - Trần Đối Tác")
                .createdAt(LocalDateTime.now().minusDays(2).withHour(17).withMinute(0))
                .build());

        transactionRepository.save(Transaction.builder()
                .store(storeB)
                .user(vendorFlower)
                .isUp(false)
                .amount(new BigDecimal("10000000"))
                .type("WITHDRAWAL")
                .status("COMPLETED")
                .description("Rút tiền lợi nhuận bán hoa về tài khoản Techcombank - STK 1903456789 - Trần Đối Tác")
                .createdAt(LocalDateTime.now().minusDays(5).withHour(18).withMinute(30))
                .build());
    }

    private record ItemSpec(Product product, int quantity, BigDecimal unitPrice) {}

    private Order createOrder(Store store, User customer, OrderStatus status, String paymentStatus,
                              String paymentMethod, BigDecimal totalAmount, BigDecimal shippingFee,
                              BigDecimal discountAmount, Long deliveryUnitId, LocalDateTime createdAt,
                              String recipientName, String recipientPhone, String recipientAddress,
                              String greetingMessage, List<ItemSpec> items) {
        Order order = Order.builder()
                .pickupStore(store)
                .user(customer)
                .orderType(OrderType.BUY)
                .status(status)
                .paymentStatus(paymentStatus)
                .paymentMethod(paymentMethod)
                .shippingMethod("DELIVERY")
                .deliveryUnitId(deliveryUnitId)
                .shippingFee(shippingFee)
                .discountAmount(discountAmount)
                .totalAmount(totalAmount)
                .recipientName(recipientName)
                .recipientPhone(recipientPhone)
                .recipientAddress(recipientAddress)
                .greetingMessage(greetingMessage)
                .deliveryDate(createdAt.toLocalDate())
                .createdAt(createdAt)
                .cancellationReason(status == OrderStatus.CANCELLED ? "Khách hàng thông báo hủy do hoãn sự kiện khai trương sang tuần sau." : null)
                .build();

        Order saved = orderRepository.save(order);

        List<OrderDetail> details = new ArrayList<>();
        for (ItemSpec item : items) {
            OrderDetail detail = OrderDetail.builder()
                    .order(saved)
                    .product(item.product())
                    .quantity(item.quantity())
                    .unitPrice(item.unitPrice())
                    .orderType(OrderType.BUY)
                    .build();
            details.add(orderDetailRepository.save(detail));
        }

        saved.setOrderDetails(details);
        return saved;
    }

    private Product findOrCreateProduct(List<Product> list, Store store, String name, BigDecimal price, String imageUrl) {
        return list.stream()
                .filter(p -> p.getName() != null && p.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(() -> {
                    Category cat = productRepository.findAll().stream()
                            .filter(p -> p.getCategory() != null)
                            .map(Product::getCategory)
                            .findFirst()
                            .orElse(null);

                    Product newP = Product.builder()
                            .name(name)
                            .store(store)
                            .category(cat)
                            .priceBuy(price)
                            .importPrice(price.multiply(new BigDecimal("0.6")).setScale(0, java.math.RoundingMode.HALF_UP))
                            .priceRentPerDay(price.multiply(new BigDecimal("0.15")).setScale(0, java.math.RoundingMode.HALF_UP))
                            .depositFee(price.multiply(new BigDecimal("0.5")).setScale(0, java.math.RoundingMode.HALF_UP))
                            .imageUrl(imageUrl)
                            .description(name + " tươi mới tuyển chọn từ các nhà vườn danh tiếng.")
                            .isActive(true)
                            .isSelling(true)
                            .sold(50)
                            .rating(4.9)
                            .build();
                    return productRepository.save(newP);
                });
    }

    private void updateInventory(Store store, Product product, int qty) {
        StoreInventory inv = storeInventoryRepository.findByStoreIdAndProductId(store.getId(), product.getId())
                .orElseGet(() -> StoreInventory.builder().store(store).product(product).build());
        inv.setQuantity(qty);
        storeInventoryRepository.save(inv);
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
