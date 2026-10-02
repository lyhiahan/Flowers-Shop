package com.finalproject.config;

import com.finalproject.entity.*;
import com.finalproject.enums.Role;
import com.finalproject.repository.*;
import com.finalproject.service.ProductSeederService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final StoreInventoryRepository storeInventoryRepository;
    private final VoucherRepository voucherRepository;
    private final DeliveryRepository deliveryRepository;
    private final UserAddressRepository userAddressRepository;
    private final UserLevelRepository userLevelRepository;
    private final StoreLevelRepository storeLevelRepository;
    private final CommissionRepository commissionRepository;
    private final StyleRepository styleRepository;
    private final StyleValueRepository styleValueRepository;
    private final ReviewRepository reviewRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProductSeederService productSeederService;

    @Override
    public void run(String... args) throws Exception {
        // 1. Tạo hoặc cập nhật 3 chi nhánh mẫu sang tiếng Việt thuần
        Store storeA = storeRepository.findByName("Chi Nhánh Bến Thành (Quận 1)")
                .orElseGet(() -> storeRepository.findByName("Bloom Flower - Chi Nhánh Bến Thành (Quận 1)")
                        .map(s -> {
                            s.setName("Chi Nhánh Bến Thành (Quận 1)");
                            return storeRepository.save(s);
                        })
                        .orElseGet(() -> storeRepository.save(Store.builder()
                                .name("Chi Nhánh Bến Thành (Quận 1)")
                                .address("123 Lê Lợi, Phường Bến Thành, Quận 1, TP.HCM")
                                .phone("0281111222")
                                .status(true)
                                .isOpen(true)
                                .rating(4.9)
                                .eWallet(new BigDecimal("15500000"))
                                .build())));

        Store storeB = storeRepository.findByName("Chi Nhánh Phú Mỹ Hưng (Quận 7)")
                .orElseGet(() -> storeRepository.findByName("Bloom Flower - Chi Nhánh Phú Mỹ Hưng (Quận 7)")
                        .map(s -> {
                            s.setName("Chi Nhánh Phú Mỹ Hưng (Quận 7)");
                            return storeRepository.save(s);
                        })
                        .orElseGet(() -> storeRepository.save(Store.builder()
                                .name("Chi Nhánh Phú Mỹ Hưng (Quận 7)")
                                .address("456 Nguyễn Thị Thập, Tân Hưng, Quận 7, TP.HCM")
                                .phone("0283333444")
                                .status(true)
                                .isOpen(true)
                                .rating(4.8)
                                .eWallet(new BigDecimal("8200000"))
                                .build())));

        Store storeC = storeRepository.findByName("Chi Nhánh Làng Đại Học (TP. Thủ Đức)")
                .orElseGet(() -> storeRepository.findByName("Bloom Flower - Chi Nhánh Làng Đại Học (TP. Thủ Đức)")
                        .map(s -> {
                            s.setName("Chi Nhánh Làng Đại Học (TP. Thủ Đức)");
                            return storeRepository.save(s);
                        })
                        .orElseGet(() -> storeRepository.save(Store.builder()
                                .name("Chi Nhánh Làng Đại Học (TP. Thủ Đức)")
                                .address("1 Võ Văn Ngân, Linh Chiểu, Thủ Đức, TP.HCM")
                                .phone("0285555666")
                                .status(true)
                                .isOpen(true)
                                .rating(4.7)
                                .eWallet(new BigDecimal("4300000"))
                                .build())));

        // 2. Tạo các tài khoản mẫu cho từng Role (mật khẩu mặc định: 123456)
        User admin = userRepository.findByUsername("admin").orElseGet(() -> userRepository.save(User.builder()
                .username("admin")
                .password(passwordEncoder.encode("123456"))
                .fullName("Quản Trị Viên Toàn Hệ Thống")
                .email("admin@rentalshop.vn")
                .role(Role.ADMIN)
                .isActive(true)
                .isEmailActive(true)
                .createdDate(LocalDate.now())
                .build()));

        User managerA = userRepository.findByUsername("manager_q1").orElseGet(() -> userRepository.save(User.builder()
                .username("manager_q1")
                .password(passwordEncoder.encode("123456"))
                .fullName("Nguyễn Quản Lý Q1")
                .email("manager.q1@rentalshop.vn")
                .phone("0912345678")
                .role(Role.MANAGER)
                .store(storeA)
                .isActive(true)
                .isEmailActive(true)
                .createdDate(LocalDate.now())
                .build()));

        User vendor = userRepository.findByUsername("vendor_flower").orElseGet(() -> userRepository.save(User.builder()
                .username("vendor_flower")
                .password(passwordEncoder.encode("123456"))
                .fullName("Trần Đối Tác Vườn Hoa Đà Lạt")
                .email("vendor@rentalshop.vn")
                .phone("0988776655")
                .role(Role.VENDOR)
                .store(storeB)
                .isActive(true)
                .isEmailActive(true)
                .createdDate(LocalDate.now())
                .build()));

        User shipper = userRepository.findByUsername("shipper_nhanh").orElseGet(() -> userRepository.save(User.builder()
                .username("shipper_nhanh")
                .password(passwordEncoder.encode("123456"))
                .fullName("Phạm Giao Hàng Hỏa Tốc")
                .email("shipper@rentalshop.vn")
                .phone("0933445566")
                .role(Role.SHIPPER)
                .isActive(true)
                .isEmailActive(true)
                .createdDate(LocalDate.now())
                .build()));

        User customer = userRepository.findByUsername("khachhang").orElseGet(() -> userRepository.save(User.builder()
                .username("khachhang")
                .password(passwordEncoder.encode("123456"))
                .fullName("Lê Khách Hàng")
                .email("khachhang@gmail.com")
                .phone("0909999888")
                .address("789 Hai Bà Trưng, Phường 8, Quận 3, TP.HCM")
                .role(Role.CUSTOMER)
                .isActive(true)
                .isEmailActive(true)
                .eWallet(new BigDecimal("2500000"))
                .createdDate(LocalDate.now())
                .build()));

        // Sổ địa chỉ cho khách hàng
        if (userAddressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(customer.getId()).isEmpty()) {
            userAddressRepository.save(UserAddress.builder()
                    .user(customer)
                    .recipientName("Lê Khách Hàng")
                    .phone("0909999888")
                    .addressDetail("789 Hai Bà Trưng, Phường 8, Quận 3, TP.HCM")
                    .isDefault(true)
                    .build());
            userAddressRepository.save(UserAddress.builder()
                    .user(customer)
                    .recipientName("Lê Khách Hàng (Văn phòng)")
                    .phone("0909999888")
                    .addressDetail("Toà nhà Bitexco, số 2 Hải Triều, Phường Bến Nghé, Quận 1, TP.HCM")
                    .isDefault(false)
                    .build());
        }

        // 3. Đơn vị vận chuyển (Deliveries)
        if (deliveryRepository.count() == 0) {
            deliveryRepository.save(Delivery.builder()
                    .name("Giao Hàng Tiêu Chuẩn Trong Ngày")
                    .price(new BigDecimal("25000"))
                    .description("Giao tận nơi trong vòng 4-6 giờ, đóng gói hoa cẩn thận có giữ ẩm.")
                    .isActive(true)
                    .build());
            deliveryRepository.save(Delivery.builder()
                    .name("GrabExpress Hỏa Tốc (2H)")
                    .price(new BigDecimal("45000"))
                    .description("Giao nhanh trong 2 giờ bảo quản hoa tươi trong lồng kính xe máy.")
                    .isActive(true)
                    .build());
            deliveryRepository.save(Delivery.builder()
                    .name("Ahamove Siêu Tốc Hoa Tươi")
                    .price(new BigDecimal("50000"))
                    .description("Tài xế chuyên giao hoa tươi và bánh kem, cam kết không dập nát.")
                    .isActive(true)
                    .build());
        }

        // 4. Mã giảm giá (Vouchers)
        if (voucherRepository.count() == 0) {
            voucherRepository.save(Voucher.builder()
                    .code("FLOWER10")
                    .name("Giảm 10% Cho Mọi Đơn Hoa Tươi")
                    .discountType("PERCENT")
                    .discountValue(new BigDecimal("10"))
                    .minOrderValue(new BigDecimal("200000"))
                    .maxDiscountAmount(new BigDecimal("100000"))
                    .quantity(100)
                    .usedCount(5)
                    .startDate(LocalDate.now().minusDays(10))
                    .endDate(LocalDate.now().plusMonths(3))
                    .isActive(true)
                    .build());

            voucherRepository.save(Voucher.builder()
                    .code("FREESHIP")
                    .name("Miễn Phí Vận Chuyển Toàn Thành Phố")
                    .discountType("FREE_SHIP")
                    .discountValue(new BigDecimal("50000"))
                    .minOrderValue(new BigDecimal("300000"))
                    .quantity(50)
                    .usedCount(12)
                    .startDate(LocalDate.now().minusDays(5))
                    .endDate(LocalDate.now().plusMonths(2))
                    .isActive(true)
                    .build());

            voucherRepository.save(Voucher.builder()
                    .code("TET2026")
                    .name("Giảm 50K Chào Đón Năm Mới")
                    .discountType("FIXED_AMOUNT")
                    .discountValue(new BigDecimal("50000"))
                    .minOrderValue(new BigDecimal("500000"))
                    .quantity(200)
                    .usedCount(28)
                    .startDate(LocalDate.now().minusDays(2))
                    .endDate(LocalDate.now().plusMonths(6))
                    .isActive(true)
                    .build());
        }

        // 5. Cấp bậc thành viên (User Levels)
        if (userLevelRepository.count() == 0) {
            userLevelRepository.save(UserLevel.builder().name("Thành Viên Đồng").minPoint(0).discount(BigDecimal.ZERO).build());
            userLevelRepository.save(UserLevel.builder().name("Thành Viên Bạc").minPoint(200).discount(new BigDecimal("3")).build());
            userLevelRepository.save(UserLevel.builder().name("Thành Viên Vàng").minPoint(500).discount(new BigDecimal("5")).build());
            userLevelRepository.save(UserLevel.builder().name("VIP Kim Cương").minPoint(1000).discount(new BigDecimal("10")).build());
        }

        // Cấp bậc chi nhánh (Store Levels): Tiêu chuẩn (0đ), Cửa hàng uy tín (10tr), Cửa hàng xuất sắc (30tr)
        if (storeLevelRepository.count() == 0) {
            storeLevelRepository.save(StoreLevel.builder().name("Tiêu chuẩn").minPoint(0).discount(BigDecimal.ZERO).isDeleted(false).build());
            storeLevelRepository.save(StoreLevel.builder().name("Cửa hàng uy tín").minPoint(10000000).discount(new BigDecimal("5")).isDeleted(false).build());
            storeLevelRepository.save(StoreLevel.builder().name("Cửa hàng xuất sắc").minPoint(30000000).discount(new BigDecimal("10")).isDeleted(false).build());
        } else {
            // Đồng bộ dữ liệu cũ nếu chưa chuẩn hóa
            for (StoreLevel sl : storeLevelRepository.findAll()) {
                String n = sl.getName().toLowerCase();
                if (n.contains("chuẩn") || (sl.getMinPoint() != null && sl.getMinPoint() == 0)) {
                    if (!sl.getName().equals("Tiêu chuẩn") || sl.getMinPoint() != 0) {
                        sl.setName("Tiêu chuẩn");
                        sl.setMinPoint(0);
                        storeLevelRepository.save(sl);
                    }
                } else if (n.contains("bạc") || n.contains("uy tín")) {
                    sl.setName("Cửa hàng uy tín");
                    sl.setMinPoint(10000000);
                    sl.setDiscount(new BigDecimal("5"));
                    sl.setIsDeleted(false);
                    storeLevelRepository.save(sl);
                } else if (n.contains("vàng") || n.contains("xuất sắc")) {
                    sl.setName("Cửa hàng xuất sắc");
                    sl.setMinPoint(30000000);
                    sl.setDiscount(new BigDecimal("10"));
                    sl.setIsDeleted(false);
                    storeLevelRepository.save(sl);
                }
            }
        }


        // Chính sách hoa hồng sàn (Commissions)
        if (commissionRepository.count() == 0) {
            commissionRepository.save(Commission.builder().name("Gói Cơ Bản (Standard)").cost(new BigDecimal("10")).description("Tỷ lệ chiết khấu sàn 10% áp dụng cho chi nhánh mới gia nhập").isDeleted(false).build());
            commissionRepository.save(Commission.builder().name("Gói Đối Tác Thân Thiết").cost(new BigDecimal("8")).description("Tỷ lệ chiết khấu ưu đãi 8% cho cửa hàng có trên 100 đơn/tháng").isDeleted(false).build());
            commissionRepository.save(Commission.builder().name("Gói Đối Tác Chiến Lược").cost(new BigDecimal("5")).description("Tỷ lệ chiết khấu đặc biệt 5% cho chuỗi phân phối hoa tươi lớn").isDeleted(false).build());
        }

        // 5. Khởi tạo & chuẩn hóa nhóm thuộc tính và giá trị thuộc tính mẫu
        initOrUpdateStylesAndValues();

        // Gán cấp bậc và gói hoa hồng cho các cửa hàng nếu chưa có
        java.util.List<StoreLevel> allSLs = storeLevelRepository.findAll();
        java.util.List<Commission> allComms = commissionRepository.findAll();
        if (!allSLs.isEmpty() && !allComms.isEmpty()) {
            for (Store s : storeRepository.findAll()) {
                boolean changed = false;
                if (s.getStoreLevel() == null) {
                    s.setStoreLevel(allSLs.get(0));
                    changed = true;
                }
                if (s.getCommission() == null) {
                    s.setCommission(allComms.get(0));
                    changed = true;
                }
                if (changed) storeRepository.save(s);
            }
        }

        // Gán cấp bậc thành viên cho người dùng nếu chưa có
        java.util.List<UserLevel> allULs = userLevelRepository.findAll();
        if (!allULs.isEmpty()) {
            for (User u : userRepository.findAll()) {
                if (u.getUserLevel() == null) {
                    u.setUserLevel(allULs.get(0));
                    userRepository.save(u);
                }
            }
        }

        // 6. Danh mục hoa tươi
        Category catSinhNhat = categoryRepository.findByName("Hoa Sinh Nhật")
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .name("Hoa Sinh Nhật")
                        .description("Các mẫu bó hoa, lẵng hoa chúc mừng sinh nhật ngọt ngào, ý nghĩa")
                        .build()));

        Category catKhaiTruong = categoryRepository.findByName("Hoa Khai Trương")
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .name("Hoa Khai Trương")
                        .description("Kệ hoa, lẵng hoa khai trương sang trọng phát tài phát lộc")
                        .build()));

        Category catTinhYeu = categoryRepository.findByName("Hoa Tình Yêu & Valentine")
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .name("Hoa Tình Yêu & Valentine")
                        .description("Hoa hồng đỏ Ecuador, hoa baby trắng lãng mạn tỏ tình")
                        .build()));

        Category catLanHoDiep = categoryRepository.findByName("Lan Hồ Điệp Sang Trọng")
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .name("Lan Hồ Điệp Sang Trọng")
                        .description("Chậu hoa lan hồ điệp cao cấp trưng Tết, biếu sếp, mừng tân gia")
                        .build()));

        // Khởi tạo các danh mục con (Subcategories) có parent_id liên kết với danh mục cha
        Category subBoSinhNhat = categoryRepository.findByName("Bó Hoa Sinh Nhật")
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .name("Bó Hoa Sinh Nhật")
                        .description("Các mẫu bó hoa tươi rực rỡ chúc mừng sinh nhật")
                        .parent(catSinhNhat)
                        .build()));
        if (subBoSinhNhat.getParent() == null) {
            subBoSinhNhat.setParent(catSinhNhat);
            categoryRepository.save(subBoSinhNhat);
        }

        Category subLangSinhNhat = categoryRepository.findByName("Lẵng Hoa Sinh Nhật")
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .name("Lẵng Hoa Sinh Nhật")
                        .description("Lẵng hoa để bàn chúc mừng sinh nhật sang trọng")
                        .parent(catSinhNhat)
                        .build()));
        if (subLangSinhNhat.getParent() == null) {
            subLangSinhNhat.setParent(catSinhNhat);
            categoryRepository.save(subLangSinhNhat);
        }

        Category subBoHongDo = categoryRepository.findByName("Bó Hoa Hồng Đỏ")
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .name("Bó Hoa Hồng Đỏ")
                        .description("Bó hoa hồng đỏ nhập khẩu quyến rũ trao gửi tình yêu")
                        .parent(catTinhYeu)
                        .build()));
        if (subBoHongDo.getParent() == null) {
            subBoHongDo.setParent(catTinhYeu);
            categoryRepository.save(subBoHongDo);
        }

        Category subBoBabyTrang = categoryRepository.findByName("Bó Hoa Baby Trắng")
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .name("Bó Hoa Baby Trắng")
                        .description("Bó hoa baby trắng tinh khôi, nhẹ nhàng lãng mạn")
                        .parent(catTinhYeu)
                        .build()));
        if (subBoBabyTrang.getParent() == null) {
            subBoBabyTrang.setParent(catTinhYeu);
            categoryRepository.save(subBoBabyTrang);
        }

        Category subKeKhaiTruong = categoryRepository.findByName("Kệ Hoa Khai Trương")
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .name("Kệ Hoa Khai Trương")
                        .description("Kệ hoa chúc mừng khai trương hồng phát sang trọng")
                        .parent(catKhaiTruong)
                        .build()));
        if (subKeKhaiTruong.getParent() == null) {
            subKeKhaiTruong.setParent(catKhaiTruong);
            categoryRepository.save(subKeKhaiTruong);
        }

        Category subLanTrang = categoryRepository.findByName("Chậu Lan Hồ Điệp Trắng")
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .name("Chậu Lan Hồ Điệp Trắng")
                        .description("Chậu lan hồ điệp trắng đại diện cho sự thanh cao, sang trọng")
                        .parent(catLanHoDiep)
                        .build()));
        if (subLanTrang.getParent() == null) {
            subLanTrang.setParent(catLanHoDiep);
            categoryRepository.save(subLanTrang);
        }

        // Tự động dọn dẹp và hợp nhất các danh mục trùng lặp liên quan đến "Tình Yêu" / "Valentine"
        java.util.List<Category> allLoveCats = categoryRepository.findAll().stream()
                .filter(c -> c.getName() != null && (c.getName().contains("Tình Yêu") || c.getName().contains("Valentine")))
                .collect(java.util.stream.Collectors.toList());
        if (allLoveCats.size() > 1) {
            final Category mainLoveCat = catTinhYeu;
            for (Category dupCat : allLoveCats) {
                if (!dupCat.getId().equals(mainLoveCat.getId())) {
                    java.util.List<Product> prodsToReassign = productRepository.findByCategoryId(dupCat.getId());
                    if (prodsToReassign != null) {
                        for (Product prod : prodsToReassign) {
                            prod.setCategory(mainLoveCat);
                            productRepository.save(prod);
                        }
                    }
                    try {
                        categoryRepository.delete(dupCat);
                    } catch (Exception ignored) {}
                }
            }
        }

        // 7. Tạo danh sách hoa mẫu phong phú có sold >= 10 phục vụ tiêu chí Khách vãng lai (Guest)
        if (productRepository.count() < 5) {
            Product p1 = Product.builder()
                    .name("Bó Hoa Hồng Đỏ Ecuador Nhập Khẩu 99 Bông 'Tình Yêu Vĩnh Cửu'")
                    .category(catTinhYeu)
                    .store(storeA)
                    .description("Bó hoa hồng đỏ nhập khẩu Ecuador size bông cực lớn, cánh dày nhung mịn, kết hợp lá bạc Dollar sang trọng biểu tượng tình yêu vĩnh cửu bất diệt.")
                    .priceBuy(new BigDecimal("1200000"))
                    .importPrice(new BigDecimal("700000"))
                    .priceRentPerDay(new BigDecimal("150000"))
                    .depositFee(new BigDecimal("500000"))
                    .imageUrl("https://images.unsplash.com/photo-1548094878-84ced0f68b08?w=800")
                    .sold(85)
                    .rating(4.9)
                    .isActive(true)
                    .isSelling(true)
                    .build();

            Product p2 = Product.builder()
                    .name("Kệ Hoa Khai Trương 'Hồng Phát Đại Cát' 2 Tầng")
                    .category(catKhaiTruong)
                    .store(storeA)
                    .description("Kệ hoa thiết kế 2 tầng kết hợp hoa đồng tiền, hoa hướng dương rực rỡ và lan hồ điệp vàng mang lại nhiều may mắn cho ngày khởi đầu kinh doanh.")
                    .priceBuy(new BigDecimal("1850000"))
                    .importPrice(new BigDecimal("1000000"))
                    .priceRentPerDay(new BigDecimal("200000"))
                    .depositFee(new BigDecimal("800000"))
                    .imageUrl("https://images.unsplash.com/photo-1526047932273-341f2a7631f9?w=800&q=80")
                    .sold(64)
                    .rating(4.8)
                    .isActive(true)
                    .isSelling(true)
                    .build();

            Product p3 = Product.builder()
                    .name("Hộp Hoa Sinh Nhật Tone Hồng Pastel 'Thiên Thần Ngọt Ngào'")
                    .category(catSinhNhat)
                    .store(storeB)
                    .description("Hộp hoa cắm tròn xinh xắn phối giữa hoa hồng trà, hoa cẩm chướng hồng và hoa baby trắng bồng bềnh, thích hợp tặng bạn gái hoặc đồng nghiệp.")
                    .priceBuy(new BigDecimal("650000"))
                    .importPrice(new BigDecimal("350000"))
                    .priceRentPerDay(new BigDecimal("80000"))
                    .depositFee(new BigDecimal("300000"))
                    .imageUrl("https://images.unsplash.com/photo-1561181286-d3fee7d55364?w=800&q=80")
                    .sold(120)
                    .rating(5.0)
                    .isActive(true)
                    .isSelling(true)
                    .build();

            Product p4 = Product.builder()
                    .name("Chậu Lan Hồ Điệp 5 Cành Tím Hoàng Gia")
                    .category(catLanHoDiep)
                    .store(storeC)
                    .description("Chậu men sứ cao cấp trồng 5 cành hoa lan hồ điệp tím rực rỡ, hoa nở đều đẹp, độ bền kéo dài từ 4 đến 8 tuần thích hợp trang trí sảnh phòng khách.")
                    .priceBuy(new BigDecimal("2200000"))
                    .importPrice(new BigDecimal("1200000"))
                    .priceRentPerDay(new BigDecimal("250000"))
                    .depositFee(new BigDecimal("1000000"))
                    .imageUrl("https://images.unsplash.com/photo-1525310072745-f49212b5ac6d?w=800&q=80")
                    .sold(38)
                    .rating(4.7)
                    .isActive(true)
                    .isSelling(true)
                    .build();

            Product p5 = Product.builder()
                    .name("Bó Hoa Hướng Dương 'Mặt Trời Tỏa Sáng'")
                    .category(catSinhNhat)
                    .store(storeA)
                    .description("Bó hoa hướng dương 5 bông size lớn gói giấy báo vintage phối kèm hoa cúc tana, hoa phi yến trắng tượng trưng cho niềm tin và hoài bão vươn xa.")
                    .priceBuy(new BigDecimal("480000"))
                    .importPrice(new BigDecimal("250000"))
                    .priceRentPerDay(new BigDecimal("60000"))
                    .depositFee(new BigDecimal("200000"))
                    .imageUrl("https://images.unsplash.com/photo-1597848212624-a19eb35e2651?w=800&q=80")
                    .sold(95)
                    .rating(4.9)
                    .isActive(true)
                    .isSelling(true)
                    .build();

            Product p6 = Product.builder()
                    .name("Bó Hoa Cưới Cầm Tay Cô Dâu Tone Trắng Tinh Khôi")
                    .category(catTinhYeu)
                    .store(storeB)
                    .description("Bó hoa cưới thiết kế tay cầm quấn ruy băng lụa cao cấp, phối hoa tulip Hà Lan trắng, mao lương trắng và hoa hồng Ohara thơm ngát quyến rũ.")
                    .priceBuy(new BigDecimal("890000"))
                    .importPrice(new BigDecimal("500000"))
                    .priceRentPerDay(new BigDecimal("120000"))
                    .depositFee(new BigDecimal("400000"))
                    .imageUrl("https://images.unsplash.com/photo-1533616688419-b7a585564566?w=800&q=80")
                    .sold(42)
                    .rating(4.8)
                    .isActive(true)
                    .isSelling(true)
                    .build();

            productRepository.save(p1);
            productRepository.save(p2);
            productRepository.save(p3);
            productRepository.save(p4);
            productRepository.save(p5);
            productRepository.save(p6);

            // Cấp tồn kho cho các chi nhánh
            storeInventoryRepository.save(StoreInventory.builder().store(storeA).product(p1).quantity(20).build());
            storeInventoryRepository.save(StoreInventory.builder().store(storeB).product(p1).quantity(10).build());
            storeInventoryRepository.save(StoreInventory.builder().store(storeA).product(p2).quantity(15).build());
            storeInventoryRepository.save(StoreInventory.builder().store(storeB).product(p3).quantity(30).build());
            storeInventoryRepository.save(StoreInventory.builder().store(storeC).product(p4).quantity(12).build());
            storeInventoryRepository.save(StoreInventory.builder().store(storeA).product(p5).quantity(25).build());
            storeInventoryRepository.save(StoreInventory.builder().store(storeB).product(p6).quantity(18).build());

            // Review mẫu (>= 50 ký tự theo yêu cầu đề bài)
            if (reviewRepository.count() == 0) {
                reviewRepository.save(Review.builder()
                        .product(p1)
                        .user(customer)
                        .rating(5)
                        .content("Hoa hồng đỏ cực kỳ tươi mới và thơm ngát, gói giấy rất sang trọng và giao đúng giờ hẹn. Mình rất hài lòng về dịch vụ của cửa hàng!")
                        .createdAt(java.time.LocalDateTime.now().minusDays(3))
                        .build());
                reviewRepository.save(Review.builder()
                        .product(p3)
                        .user(customer)
                        .rating(5)
                        .content("Hộp hoa tặng sinh nhật mẹ mình rất ưng ý, hoa tươi giữ được cả tuần luôn. Cảm ơn shop nhiều, sẽ tiếp tục ủng hộ lâu dài!")
                        .createdAt(java.time.LocalDateTime.now().minusDays(1))
                        .build());
            }
        }

        productSeederService.seedMissingProductsForStores();

        // 8. Tự động chuẩn hóa toàn bộ tên sản phẩm và sửa ảnh sản phẩm chính xác theo loài hoa
        productRepository.findAll().forEach(p -> {
            boolean changed = false;
            if (p.getName() != null && p.getName().contains("Forever Love")) {
                p.setName(p.getName().replace("'Forever Love'", "'Tình Yêu Vĩnh Cửu'").replace("Forever Love", "Tình Yêu Vĩnh Cửu"));
                changed = true;
            }
            if (p.getName() != null && p.getName().contains("Sweet Angel")) {
                p.setName(p.getName().replace("'Sweet Angel'", "'Thiên Thần Ngọt Ngào'").replace("Sweet Angel", "Thiên Thần Ngọt Ngào"));
                changed = true;
            }
            if (p.getName() != null && p.getName().contains("Sweet Love")) {
                p.setName(p.getName().replace("Sweet Love", "'Tình Yêu Ngọt Ngào'"));
                changed = true;
            }

            // Sửa ảnh sản phẩm bị lỗi hoặc không khớp loài hoa
            String nameLower = p.getName() != null ? p.getName().toLowerCase() : "";
            String currentImg = p.getImageUrl();
            if (currentImg == null || currentImg.trim().isEmpty() || currentImg.contains("placeholder") || currentImg.contains("via.placeholder")) {
                p.setImageUrl("https://images.unsplash.com/photo-1563245372-f21724e3856d?w=800&q=80");
                changed = true;
            } else if (nameLower.contains("hướng dương") && !currentImg.contains("photo-1597848212624")) {
                p.setImageUrl("https://images.unsplash.com/photo-1597848212624-a19eb35e2651?w=800&q=80");
                changed = true;
            } else if (nameLower.contains("hồ điệp") && !currentImg.contains("photo-1525310072745")) {
                p.setImageUrl("https://images.unsplash.com/photo-1525310072745-f49212b5ac6d?w=800&q=80");
                changed = true;
            } else if ((nameLower.contains("hồng đỏ") || nameLower.contains("ecuador")) && !currentImg.contains("photo-1548094878")) {
                p.setImageUrl("https://images.unsplash.com/photo-1548094878-84ced0f68b08?w=800");
                changed = true;
            } else if ((nameLower.contains("hoa cưới") || nameLower.contains("cô dâu") || nameLower.contains("trắng")) && !currentImg.contains("photo-1533616688419")) {
                p.setImageUrl("https://images.unsplash.com/photo-1533616688419-b7a585564566?w=800&q=80");
                changed = true;
            } else if (nameLower.contains("khai trương") && !currentImg.contains("photo-1526047932273")) {
                p.setImageUrl("https://images.unsplash.com/photo-1526047932273-341f2a7631f9?w=800&q=80");
                changed = true;
            } else if ((nameLower.contains("sinh nhật") || nameLower.contains("pastel") || nameLower.contains("thiên thần")) && !currentImg.contains("photo-1561181286")) {
                p.setImageUrl("https://images.unsplash.com/photo-1561181286-d3fee7d55364?w=800&q=80");
                changed = true;
            }

            if (changed) {
                productRepository.save(p);
            }
        });
    }

    /**
     * Đồng bộ và bổ sung an toàn 4 nhóm thuộc tính cốt lõi và các giá trị tiêu chuẩn khớp bộ lọc khách hàng
     */
    public void initOrUpdateStylesAndValues() {
        // 1. Chuẩn hóa tên các nhóm cũ sang tên mới thống nhất
        styleRepository.findAll().forEach(s -> {
            String n = s.getName().trim();
            if (n.equalsIgnoreCase("Tone màu hoa") || n.equalsIgnoreCase("Tone màu thiết kế")) {
                s.setName("Màu sắc");
                styleRepository.save(s);
            } else if (n.equalsIgnoreCase("Kiểu dáng cắm") || n.equalsIgnoreCase("Kiểu dáng hoa")) {
                s.setName("Kiểu dáng");
                styleRepository.save(s);
            } else if (n.equalsIgnoreCase("Kích cỡ bó hoa") || n.equalsIgnoreCase("Kích cỡ thiết kế")) {
                s.setName("Kích cỡ");
                styleRepository.save(s);
            }
        });

        // 2. Định nghĩa 4 nhóm chính và các nhóm bổ sung cùng giá trị chuẩn
        java.util.Map<String, java.util.List<String>> defaultGroups = new java.util.LinkedHashMap<>();
        defaultGroups.put("Loại hoa", java.util.List.of(
                "Hướng dương", "Hoa hồng", "Tulip", "Cẩm tú cầu", "Lan hồ điệp", "Calla Lily", "Hoa Baby"
        ));
        defaultGroups.put("Kiểu dáng", java.util.List.of(
                "Bó hoa", "Giỏ hoa", "Bình hoa", "Hộp hoa", "Chậu hoa", "Kệ hoa"
        ));
        defaultGroups.put("Màu sắc", java.util.List.of(
                "Vàng", "Hồng", "Trắng", "Đỏ", "Tím", "Xanh", "Cam"
        ));
        defaultGroups.put("Dịp tặng", java.util.List.of(
                "Sinh nhật", "Tốt nghiệp", "Khai trương", "Tình yêu", "Chúc mừng", "Đám cưới", "Chia buồn"
        ));
        defaultGroups.put("Phong cách thiết kế", java.util.List.of(
                "Hàn Quốc hiện đại", "Vintage cổ điển", "Tối giản Minimalist", "Châu Âu sang trọng"
        ));
        defaultGroups.put("Kích cỡ", java.util.List.of(
                "Size S (Nhỏ)", "Size M (Vừa)", "Size L (Lớn)"
        ));

        // 3. Bổ sung nhóm và giá trị còn thiếu mà không xóa hay ghi đè dữ liệu của người dùng
        for (java.util.Map.Entry<String, java.util.List<String>> entry : defaultGroups.entrySet()) {
            String groupName = entry.getKey();
            java.util.List<String> values = entry.getValue();

            Style style = styleRepository.findAll().stream()
                    .filter(s -> s.getName().equalsIgnoreCase(groupName))
                    .findFirst()
                    .orElseGet(() -> styleRepository.save(Style.builder()
                            .name(groupName)
                            .isDeleted(false)
                            .build()));

            java.util.List<StyleValue> existingValues = styleValueRepository.findByStyleId(style.getId());
            java.util.Set<String> existingNames = existingValues.stream()
                    .map(v -> v.getName().toLowerCase().trim())
                    .collect(java.util.stream.Collectors.toSet());

            for (String valName : values) {
                if (!existingNames.contains(valName.toLowerCase().trim())) {
                    styleValueRepository.save(StyleValue.builder()
                            .name(valName)
                            .style(style)
                            .isDeleted(false)
                            .build());
                }
            }
        }
    }
}
