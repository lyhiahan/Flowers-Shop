package com.finalproject.service;

import com.finalproject.entity.*;
import com.finalproject.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class DemoProductFixtureSeeder {
    private final ProductRepository productRepository;
    private final StoreInventoryRepository storeInventoryRepository;
    private final ReviewRepository reviewRepository;

    @Transactional
    public void seed(Store storeA, Store storeB, Store storeC, User customer, Category catTinhYeu, Category catKhaiTruong, Category catSinhNhat, Category catLanHoDiep) {
        // 7. Tạo danh sách hoa mẫu phong phú có sold >= 10 phục vụ tiêu chí Khách vãng lai (Guest)
        if (productRepository.count() < 5) {
            Product p1 = Product.builder()
                    .name("Bó Hoa Hồng Đỏ Ecuador 99 Bông Nhập Khẩu")
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
                    .name("Kệ Hoa Khai Trương 2 Tầng Đồng Tiền & Lan Vàng")
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
                    .name("Hộp Hoa Tròn Hoa Hồng Trà Phối Cẩm Chướng")
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
                    .name("Chậu Sứ Lan Hồ Điệp Tím 5 Cành")
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
                    .name("Bó Hoa Hướng Dương 5 Bông Gói Báo Vintage")
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
                    .name("Bó Hoa Cưới Tulip Trắng & Hoa Hồng Ohara")
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

    }
}
