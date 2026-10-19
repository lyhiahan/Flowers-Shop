package com.finalproject.service;

import com.finalproject.entity.Category;
import com.finalproject.entity.Product;
import com.finalproject.entity.Store;
import com.finalproject.entity.StoreInventory;
import com.finalproject.repository.CategoryRepository;
import com.finalproject.repository.ProductRepository;
import com.finalproject.repository.StoreInventoryRepository;
import com.finalproject.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSeederService {

    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final CategoryRepository categoryRepository;
    private final StoreInventoryRepository storeInventoryRepository;

    public record ProductTemplate(
            String name,
            String categoryName,
            String storeKeyword,
            String description,
            BigDecimal priceBuy,
            BigDecimal importPrice,
            BigDecimal priceRentPerDay,
            BigDecimal depositFee,
            String imageUrl,
            int sold,
            double rating
    ) {}

    @Transactional
    public int seedMissingProductsForStores() {
        // 1. Đảm bảo đầy đủ 9 loại sản phẩm chuẩn trong hệ thống
        Map<String, Category> catMap = ensureAll9Categories();

        // 2. Tìm 3 chi nhánh mẫu
        Store storeA = findStoreByKeyword("Quận 1", "Bến Thành");
        Store storeB = findStoreByKeyword("Quận 7", "Phú Mỹ Hưng");
        Store storeC = findStoreByKeyword("Thủ Đức", "Làng Đại Học");

        List<Store> allStores = storeRepository.findAll();
        if (storeA == null && !allStores.isEmpty()) storeA = allStores.get(0);
        if (storeB == null && allStores.size() > 1) storeB = allStores.get(1);
        else if (storeB == null) storeB = storeA;
        if (storeC == null && allStores.size() > 2) storeC = allStores.get(2);
        else if (storeC == null) storeC = storeB;

        if (storeA == null) {
            log.warn("Chưa có chi nhánh nào trong hệ thống để bổ sung mặt hàng.");
            return 0;
        }

        // 3. Danh mục 27 mặt hàng đầy đủ 9 loại sản phẩm và đủ 6 kiểu dáng (Bó, Giỏ, Hộp, Kệ, Chậu, Bình)
        List<ProductTemplate> templates = List.of(
            // --- CHI NHÁNH BẾN THÀNH (QUẬN 1) --- (9 loại sản phẩm)
            new ProductTemplate(
                "Bó Hoa Hồng Đỏ 99 Bông",
                "Hoa Tình Yêu", "Quận 1",
                "Bó hoa hồng đỏ nhập khẩu Ecuador size bông cực lớn, cánh dày nhung mịn, kết hợp lá bạc Dollar sang trọng biểu tượng tình yêu vĩnh cửu bất diệt.",
                new BigDecimal("1200000"), new BigDecimal("700000"), new BigDecimal("150000"), new BigDecimal("500000"),
                "https://images.unsplash.com/photo-1548094878-84ced0f68b08?w=800",
                85, 4.9
            ),
            new ProductTemplate(
                "Bó Hoa Hướng Dương Vintage",
                "Hoa Sinh Nhật", "Quận 1",
                "Bó hoa hướng dương 5 bông size lớn gói giấy báo vintage phối kèm hoa cúc tana, hoa phi yến trắng tượng trưng cho niềm tin và hoài bão vươn xa.",
                new BigDecimal("480000"), new BigDecimal("250000"), new BigDecimal("60000"), new BigDecimal("200000"),
                "https://images.unsplash.com/photo-1597848212624-a19eb35e2651?w=800&q=80",
                95, 4.9
            ),
            new ProductTemplate(
                "Kệ Hoa Khai Trương Đồng Tiền",
                "Hoa Khai Trương", "Quận 1",
                "Kệ hoa thiết kế 2 tầng kết hợp hoa đồng tiền, hoa hướng dương rực rỡ và lan hồ điệp vàng mang lại nhiều may mắn cho ngày khởi đầu kinh doanh.",
                new BigDecimal("1850000"), new BigDecimal("1000000"), new BigDecimal("200000"), new BigDecimal("800000"),
                "https://images.unsplash.com/photo-1526047932273-341f2a7631f9?w=800&q=80",
                64, 4.8
            ),
            new ProductTemplate(
                "Giỏ Hoa Đồng Tiền Cam",
                "Hoa Quà Tặng", "Quận 1",
                "Giỏ hoa thiết kế phối hoa đồng tiền, hướng dương và hoa hồng cam lãng mạn mang đến tài lộc khởi sắc cho chi nhánh mới.",
                new BigDecimal("950000"), new BigDecimal("550000"), new BigDecimal("120000"), new BigDecimal("400000"),
                "https://images.unsplash.com/photo-1563245372-f21724e3856d?w=800&q=80",
                58, 4.9
            ),
            new ProductTemplate(
                "Hộp Hoa Hồng Đỏ & Baby",
                "Hoa Quà Tặng", "Quận 1",
                "Hộp hoa nhung tròn cao cấp cắm kín hoa hồng đỏ nhung cùng hoa baby trắng biểu tượng tình yêu sâu đậm và nồng cháy.",
                new BigDecimal("850000"), new BigDecimal("480000"), new BigDecimal("100000"), new BigDecimal("350000"),
                "https://images.unsplash.com/photo-1561181286-d3fee7d55364?w=800&q=80",
                72, 5.0
            ),
            new ProductTemplate(
                "Chậu Lan Hồ Điệp Trắng",
                "Hoa Quà Tặng", "Quận 1",
                "Chậu lan hồ điệp trắng nhập khẩu cành dài hoa to kết hợp chậu sứ vân đá cẩm thạch sang trọng thích hợp biếu đối tác doanh nghiệp.",
                new BigDecimal("2500000"), new BigDecimal("1400000"), new BigDecimal("280000"), new BigDecimal("1200000"),
                "https://images.unsplash.com/photo-1525310072745-f49212b5ac6d?w=800&q=80",
                34, 4.9
            ),
            new ProductTemplate(
                "Bình Hoa Hồng Ohara",
                "Hoa Trang Trí", "Quận 1",
                "Bình gốm sứ mạ viền vàng cắm hoa hồng ngoại Ohara thơm ngát phối cúc mẫu đơn Hà Lan trưng phòng khách đẳng cấp.",
                new BigDecimal("780000"), new BigDecimal("420000"), new BigDecimal("90000"), new BigDecimal("300000"),
                "https://images.unsplash.com/photo-1582794543139-8ac9cb0f7b11?w=800&q=80",
                46, 4.8
            ),
            new ProductTemplate(
                "Bó Hoa Cưới Tulip Trắng",
                "Hoa Cưới", "Quận 1",
                "Bó hoa cưới tulip trắng phối mao lương và hoa hồng trắng quấn dải ruy băng lụa satin tôn vinh nét đẹp tinh khôi ngày cưới.",
                new BigDecimal("890000"), new BigDecimal("500000"), new BigDecimal("120000"), new BigDecimal("400000"),
                "https://images.unsplash.com/photo-1533616688419-b7a585564566?w=800&q=80",
                51, 4.9
            ),
            new ProductTemplate(
                "Kệ Hoa Tang Lễ Trắng Tím",
                "Hoa Chia Buồn", "Quận 1",
                "Kệ hoa tang lễ thiết kế trang nghiêm phối hoa cúc trắng, hoa lan tím và hoa ly trắng gửi gắm niềm tiếc thương sâu sắc.",
                new BigDecimal("1150000"), new BigDecimal("650000"), new BigDecimal("150000"), new BigDecimal("500000"),
                "https://images.unsplash.com/photo-1526047932273-341f2a7631f9?w=800&q=80",
                29, 4.8
            ),

            // --- CHI NHÁNH PHÚ MỸ HƯNG (QUẬN 7) --- (9 loại sản phẩm)
            new ProductTemplate(
                "Hộp Hoa Hồng Trà",
                "Hoa Quà Tặng", "Quận 7",
                "Hộp hoa cắm tròn xinh xắn phối giữa hoa hồng trà, hoa cẩm chướng hồng và hoa baby trắng bồng bềnh, thích hợp tặng bạn gái hoặc đồng nghiệp.",
                new BigDecimal("650000"), new BigDecimal("350000"), new BigDecimal("80000"), new BigDecimal("300000"),
                "https://images.unsplash.com/photo-1561181286-d3fee7d55364?w=800&q=80",
                120, 5.0
            ),
            new ProductTemplate(
                "Bó Hoa Cưới Tulip & Hồng",
                "Hoa Cưới", "Quận 7",
                "Bó hoa cưới thiết kế tay cầm quấn ruy băng lụa cao cấp, phối hoa tulip Hà Lan trắng, mao lương trắng và hoa hồng Ohara thơm ngát quyến rũ.",
                new BigDecimal("890000"), new BigDecimal("500000"), new BigDecimal("120000"), new BigDecimal("400000"),
                "https://images.unsplash.com/photo-1533616688419-b7a585564566?w=800&q=80",
                42, 4.8
            ),
            new ProductTemplate(
                "Bó Hoa Tulip Pastel",
                "Hoa Tình Yêu", "Quận 7",
                "Bó hoa tulip nhập khẩu Hà Lan tone hồng pastel ngọt ngào, gói giấy mờ cao cấp phong cách tiểu thư thanh lịch.",
                new BigDecimal("950000"), new BigDecimal("550000"), new BigDecimal("110000"), new BigDecimal("400000"),
                "https://images.unsplash.com/photo-1520763185298-1b434c919102?w=800&q=80",
                68, 4.9
            ),
            new ProductTemplate(
                "Giỏ Hoa Hồng Kem Dâu",
                "Hoa Sinh Nhật", "Quận 7",
                "Giỏ mây cắm hoa hồng kem dâu, cẩm chướng và hoa baby bồng bềnh thích hợp tặng mẹ, chị gái hoặc người thương.",
                new BigDecimal("720000"), new BigDecimal("400000"), new BigDecimal("90000"), new BigDecimal("300000"),
                "https://images.unsplash.com/photo-1563245372-f21724e3856d?w=800&q=80",
                88, 4.9
            ),
            new ProductTemplate(
                "Kệ Hoa Khai Trương Tone Đỏ",
                "Hoa Khai Trương", "Quận 7",
                "Kệ hoa khung tranh hiện đại phối hoa hồng đỏ, cẩm tú cầu và lan vũ nữ đón đầu xu hướng thẩm mỹ sang trọng.",
                new BigDecimal("1950000"), new BigDecimal("1100000"), new BigDecimal("220000"), new BigDecimal("850000"),
                "https://images.unsplash.com/photo-1526047932273-341f2a7631f9?w=800&q=80",
                53, 4.8
            ),
            new ProductTemplate(
                "Chậu Lan Hồ Điệp Vàng",
                "Hoa Quà Tặng", "Quận 7",
                "Chậu lan hồ điệp vàng hoàng yến cánh dày bóng mượt, tượng trưng cho tiền tài sung túc và phát triển bền vững.",
                new BigDecimal("2300000"), new BigDecimal("1300000"), new BigDecimal("260000"), new BigDecimal("1100000"),
                "https://images.unsplash.com/photo-1525310072745-f49212b5ac6d?w=800&q=80",
                39, 4.8
            ),
            new ProductTemplate(
                "Bình Hoa Cẩm Tú Cầu",
                "Hoa Trang Trí", "Quận 7",
                "Bình hoa cẩm tú cầu xanh bồng bềnh kết hợp hoa baby trắng tinh khiết tạo điểm nhấn trang nhã cho không gian sống.",
                new BigDecimal("690000"), new BigDecimal("380000"), new BigDecimal("80000"), new BigDecimal("280000"),
                "https://images.unsplash.com/photo-1582794543139-8ac9cb0f7b11?w=800&q=80",
                44, 4.7
            ),
            new ProductTemplate(
                "Bó Hoa Baby Trắng",
                "Hoa Tình Yêu", "Quận 7",
                "Bó hoa baby trắng khổng lồ bồng bềnh như mây, gói giấy Kraft tối giản tạo vẻ đẹp tinh tế thuần khiết.",
                new BigDecimal("520000"), new BigDecimal("280000"), new BigDecimal("70000"), new BigDecimal("220000"),
                "https://images.unsplash.com/photo-1518895949257-7621c3c786d7?w=800&q=80",
                91, 4.9
            ),
            new ProductTemplate(
                "Bó Hoa Hướng Dương Chuông Xanh",
                "Hoa Chúc Mừng", "Quận 7",
                "Bó hoa hướng dương rực rỡ phối hoa chuông xanh và cúc tana chúc mừng tân gia, mừng thăng chức thành công rực rỡ.",
                new BigDecimal("620000"), new BigDecimal("340000"), new BigDecimal("80000"), new BigDecimal("250000"),
                "https://images.unsplash.com/photo-1597848212624-a19eb35e2651?w=800&q=80",
                77, 4.9
            ),

            // --- CHI NHÁNH LÀNG ĐẠI HỌC (TP. THỦ ĐỨC) --- (9 loại sản phẩm)
            new ProductTemplate(
                "Chậu Lan Hồ Điệp Tím 5 Cành",
                "Hoa Quà Tặng", "Thủ Đức",
                "Chậu men sứ cao cấp trồng 5 cành hoa lan hồ điệp tím rực rỡ, hoa nở đều đẹp, độ bền kéo dài từ 4 đến 8 tuần thích hợp trang trí sảnh phòng khách.",
                new BigDecimal("2200000"), new BigDecimal("1200000"), new BigDecimal("250000"), new BigDecimal("1000000"),
                "https://images.unsplash.com/photo-1525310072745-f49212b5ac6d?w=800&q=80",
                38, 4.7
            ),
            new ProductTemplate(
                "Bó Hoa Hướng Dương Tốt Nghiệp",
                "Hoa Chúc Mừng", "Thủ Đức",
                "Bó hoa hướng dương rực rỡ tặng lễ tốt nghiệp cử nhân, đính kèm gấu bông cử nhân mini và ruy băng chúc mừng.",
                new BigDecimal("390000"), new BigDecimal("200000"), new BigDecimal("50000"), new BigDecimal("180000"),
                "https://images.unsplash.com/photo-1597848212624-a19eb35e2651?w=800&q=80",
                135, 5.0
            ),
            new ProductTemplate(
                "Bó Hoa Cúc Tana",
                "Hoa Sinh Nhật", "Thủ Đức",
                "Bó hoa cúc tana trắng cánh mỏng nhỏ xinh phối lá cỏ đồng nội nhẹ nhàng, món quà sinh nhật mộc mạc đáng yêu.",
                new BigDecimal("350000"), new BigDecimal("180000"), new BigDecimal("50000"), new BigDecimal("150000"),
                "https://images.unsplash.com/photo-1533616688419-b7a585564566?w=800&q=80",
                110, 4.9
            ),
            new ProductTemplate(
                "Giỏ Hoa Cẩm Chướng Pastel",
                "Hoa Quà Tặng", "Thủ Đức",
                "Giỏ gỗ cắm hoa đồng tiền hồng pastel, cẩm chướng và hoa sao tím tri ân thầy cô giáo và chúc mừng bạn bè.",
                new BigDecimal("550000"), new BigDecimal("290000"), new BigDecimal("70000"), new BigDecimal("220000"),
                "https://images.unsplash.com/photo-1563245372-f21724e3856d?w=800&q=80",
                82, 4.8
            ),
            new ProductTemplate(
                "Hộp Hoa Tone Hồng",
                "Hoa Quà Tặng", "Thủ Đức",
                "Hộp mica nắp trong suốt sang xịn, hoa tươi cắm bên trong thắt nơ lụa tinh xảo, thích hợp để bàn học, bàn làm việc.",
                new BigDecimal("490000"), new BigDecimal("260000"), new BigDecimal("60000"), new BigDecimal("200000"),
                "https://images.unsplash.com/photo-1561181286-d3fee7d55364?w=800&q=80",
                67, 4.9
            ),
            new ProductTemplate(
                "Kệ Hoa Hướng Dương 1 Tầng",
                "Hoa Chúc Mừng", "Thủ Đức",
                "Kệ hoa chân sắt gọn nhẹ cắm hoa hướng dương, đồng tiền đỏ và lan mokara tươi sáng cho sự kiện câu lạc bộ, khoa viện.",
                new BigDecimal("850000"), new BigDecimal("450000"), new BigDecimal("110000"), new BigDecimal("380000"),
                "https://images.unsplash.com/photo-1526047932273-341f2a7631f9?w=800&q=80",
                49, 4.8
            ),
            new ProductTemplate(
                "Bình Hoa Cúc Họa Mi",
                "Hoa Trang Trí", "Thủ Đức",
                "Bình thủy tinh nhỏ xinh cắm hoa cúc họa mi trắng thanh khiết, mang lại năng lượng tích cực cho góc học tập.",
                new BigDecimal("280000"), new BigDecimal("140000"), new BigDecimal("40000"), new BigDecimal("120000"),
                "https://images.unsplash.com/photo-1582794543139-8ac9cb0f7b11?w=800&q=80",
                98, 4.9
            ),
            new ProductTemplate(
                "Bó Hoa Hồng Đỏ 20 Bông",
                "Hoa Tình Yêu", "Thủ Đức",
                "Bó hoa hồng đỏ gói giấy đen nhám viền vàng phong cách lãng mạn giúp các bạn sinh viên tỏ tình thành công.",
                new BigDecimal("580000"), new BigDecimal("300000"), new BigDecimal("75000"), new BigDecimal("250000"),
                "/images/demo-rose-bouquet.jpg",
                76, 4.9
            ),
            new ProductTemplate(
                "Kệ Hoa Khai Trương Ngũ Sắc",
                "Hoa Khai Trương", "Thủ Đức",
                "Kệ hoa chân gỗ cắm hoa đồng tiền ngũ sắc và hoa hướng dương chúc mừng lễ thành lập câu lạc bộ, đội nhóm trường đại học.",
                new BigDecimal("790000"), new BigDecimal("420000"), new BigDecimal("100000"), new BigDecimal("350000"),
                "https://images.unsplash.com/photo-1526047932273-341f2a7631f9?w=800&q=80",
                55, 4.8
            )
        );

        List<Product> allCurrent = productRepository.findAll();
        int addedCount = 0;

        for (ProductTemplate t : templates) {
            boolean exists = allCurrent.stream()
                    .anyMatch(p -> p.getName() != null && p.getName().trim().equalsIgnoreCase(t.name().trim()));
            if (!exists) {
                Store assignedStore = t.storeKeyword().equals("Quận 1") ? storeA :
                                      t.storeKeyword().equals("Quận 7") ? storeB : storeC;
                Category category = catMap.getOrDefault(t.categoryName(), catMap.get("Hoa Sinh Nhật"));

                Product p = Product.builder()
                        .name(t.name())
                        .category(category)
                        .store(assignedStore)
                        .description(t.description())
                        .priceBuy(t.priceBuy())
                        .importPrice(t.importPrice())
                        .priceRentPerDay(t.priceRentPerDay())
                        .depositFee(t.depositFee())
                        .imageUrl(t.imageUrl())
                        .sold(t.sold())
                        .rating(t.rating())
                        .isActive(true)
                        .isSelling(true)
                        .build();

                Product saved = productRepository.save(p);
                allCurrent.add(saved);
                addedCount++;

                // Tạo tồn kho chi nhánh
                storeInventoryRepository.save(StoreInventory.builder()
                        .store(assignedStore)
                        .product(saved)
                        .quantity(25)
                        .build());
            }
        }

        log.info("ProductSeederService đã bổ sung {} sản phẩm mẫu mới cho các chi nhánh!", addedCount);
        return addedCount;
    }

    private Map<String, Category> ensureAll9Categories() {
        Map<String, Category> map = new HashMap<>();
        var defaults9 = com.finalproject.utils.FlowerCategoryDefaults.definitions();

        for (var d : defaults9) {
            Category cat = categoryRepository.findByName(d.name())
                .orElseGet(() -> categoryRepository.save(Category.builder()
                    .name(d.name())
                    .description(d.description())
                    .build()));
            map.put(d.name(), cat);
        }
        return map;
    }

    private Store findStoreByKeyword(String... keywords) {
        List<Store> stores = storeRepository.findAll();
        for (Store s : stores) {
            String combined = (s.getName() + " " + s.getAddress()).toLowerCase();
            for (String kw : keywords) {
                if (combined.contains(kw.toLowerCase())) {
                    return s;
                }
            }
        }
        return null;
    }
}
