package com.finalproject.config;

import com.finalproject.entity.*;
import com.finalproject.repository.*;
import com.finalproject.service.ProductSeederService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@org.springframework.context.annotation.Profile("dev")
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final VoucherRepository voucherRepository;
    private final DeliveryRepository deliveryRepository;
    private final UserLevelRepository userLevelRepository;
    private final StoreLevelRepository storeLevelRepository;
    private final CommissionRepository commissionRepository;
    private final ProductSeederService productSeederService;
    private final com.finalproject.service.VendorDataSeederService vendorDataSeederService;
    private final com.finalproject.service.DemoBranchSeeder branchSeeder;
    private final com.finalproject.service.DemoProductFixtureSeeder productFixtureSeeder;
    private final com.finalproject.service.DemoStyleSeeder styleSeeder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() > 0 || storeRepository.count() > 0 || productRepository.count() > 0) {
            // Once populated, fixture generation is an explicit admin action. Recreating
            // templates by name would duplicate products that an operator has renamed.
            return;
        }
        var branches = branchSeeder.seed();
        Store storeA = branches.storeA(), storeB = branches.storeB(), storeC = branches.storeC();
        User customer = branches.customer();

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

        // Categories classify occasions; species/containers belong to attributes.
        for (var definition : com.finalproject.utils.FlowerCategoryDefaults.definitions()) {
            if (!categoryRepository.existsByName(definition.name())) {
                categoryRepository.save(Category.builder().name(definition.name()).description(definition.description()).isDeleted(false).build());
            }
        }
        Category catSinhNhat = categoryRepository.findByName("Hoa Sinh Nhật").orElseThrow();
        Category catKhaiTruong = categoryRepository.findByName("Hoa Khai Trương").orElseThrow();
        Category catTinhYeu = categoryRepository.findByName("Hoa Tình Yêu").orElseThrow();
        Category catLanHoDiep = categoryRepository.findByName("Hoa Quà Tặng").orElseThrow();

        // Demo initialization supplements fixtures; it does not merge or delete user categories.

        productFixtureSeeder.seed(storeA, storeB, storeC, customer, catTinhYeu, catKhaiTruong, catSinhNhat, catLanHoDiep);

        productSeederService.seedMissingProductsForStores();
        vendorDataSeederService.seedVendorDemoData();

    }

    public void initOrUpdateStylesAndValues() { styleSeeder.seed(); }
}
