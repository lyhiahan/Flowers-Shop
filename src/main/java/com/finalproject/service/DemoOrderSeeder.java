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

@Service
@RequiredArgsConstructor
@Slf4j
public class DemoOrderSeeder {
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final StoreInventoryRepository storeInventoryRepository;
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final PaymentRepository paymentRepository;
    private final DeliveryRepository deliveryRepository;
    private final PasswordEncoder passwordEncoder;
    private final DemoWalletSeeder walletSeeder;
    @Transactional
    public void seed(Store storeB, User vendorFlower) {
        // Lấy hoặc tạo tài khoản khách hàng để đặt đơn
        User customer1 = userRepository.findByUsername("leminhhoa").or(() -> userRepository.findByEmail("khachhang@gmail.com")).orElseGet(() -> userRepository.save(User.builder()
                .username("leminhhoa")
                .password(passwordEncoder.encode("123456"))
                .fullName("Lê Minh Hoa")
                .email("khachhang@gmail.com")
                .phone("0909999888")
                .role(Role.USER)
                .isActive(true)
                .isEmailActive(true)
                .createdDate(LocalDate.now().minusMonths(6))
                .build()));

        User customer2 = userRepository.findByUsername("nguyenbichtram").or(() -> userRepository.findByEmail("bichtram.flower@gmail.com")).orElseGet(() -> userRepository.save(User.builder()
                .username("nguyenbichtram")
                .password(passwordEncoder.encode("123456"))
                .fullName("Nguyễn Ngọc Bích Trâm")
                .email("bichtram.flower@gmail.com")
                .phone("0908776655")
                .address("Khu biệt thự Chateau, Phú Mỹ Hưng, Quận 7, TP.HCM")
                .role(Role.USER)
                .isActive(true)
                .isEmailActive(true)
                .createdDate(LocalDate.now().minusMonths(3))
                .build()));

        User customer3 = userRepository.findByUsername("namsaigon").or(() -> userRepository.findByEmail("contact@namsaigontech.vn")).orElseGet(() -> userRepository.save(User.builder()
                .username("namsaigon")
                .password(passwordEncoder.encode("123456"))
                .fullName("Công Ty Cổ Phần Công Nghệ Nam Sài Gòn")
                .email("contact@namsaigontech.vn")
                .phone("02873009988")
                .address("Tòa nhà MapleTree, 1060 Nguyễn Văn Linh, Tân Phong, Quận 7, TP.HCM")
                .role(Role.USER)
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
        walletSeeder.seed(storeB, vendorFlower, createdPaidOrders);
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
        // New fixtures have receipts too; never fabricate receipts for existing orders.
        boolean paid = "PAID".equals(paymentStatus);
        paymentRepository.save(Payment.builder().order(saved)
                .method(com.finalproject.enums.PaymentMethod.valueOf(paymentMethod)).amount(totalAmount)
                .status(paid ? com.finalproject.enums.PaymentStatus.SUCCESS
                        : status == OrderStatus.CANCELLED ? com.finalproject.enums.PaymentStatus.FAILED
                        : com.finalproject.enums.PaymentStatus.PENDING)
                .transactionCode(paid ? "DEMO-SEED-" + saved.getId() : null)
                .momoEnvironment("MOMO".equals(paymentMethod) ? "mock" : null)
                .paidAt(paid ? createdAt : null).build());

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

}
