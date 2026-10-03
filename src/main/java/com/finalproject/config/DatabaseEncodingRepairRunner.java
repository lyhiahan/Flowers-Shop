package com.finalproject.config;

import com.finalproject.entity.*;
import com.finalproject.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

@Component
@org.springframework.core.annotation.Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class DatabaseEncodingRepairRunner implements CommandLineRunner {

    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final VoucherRepository voucherRepository;
    private final DeliveryRepository deliveryRepository;
    private final UserLevelRepository userLevelRepository;
    private final StoreLevelRepository storeLevelRepository;
    private final StyleRepository styleRepository;
    private final StyleValueRepository styleValueRepository;
    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
    private final TransactionRepository transactionRepository;

    public static String fixMojibake(String str) {
        if (str == null || str.isEmpty() || !containsMojibake(str)) {
            return str;
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            for (int i = 0; i < str.length(); i++) {
                char ch = str.charAt(i);
                int cp = (int) ch;
                if (cp <= 0x7F) {
                    baos.write(cp);
                } else if (cp == 0x201C) { // Left double quote
                    baos.write(0x93);
                } else if (cp == 0x201D) { // Right double quote
                    baos.write(0x94);
                } else if (cp == 0x2018) { // Left single quote
                    baos.write(0x91);
                } else if (cp == 0x2019) { // Right single quote
                    baos.write(0x92);
                } else if (cp == 0x2022) { // Bullet
                    baos.write(0x95);
                } else if (cp == 0x2013) { // En dash
                    baos.write(0x96);
                } else if (cp == 0x2014) { // Em dash
                    baos.write(0x97);
                } else if (cp == 0x02DC) { // Small tilde
                    baos.write(0x98);
                } else if (cp == 0x2122) { // Trademark
                    baos.write(0x99);
                } else if (cp == 0x0161) { // s caron
                    baos.write(0x9A);
                } else if (cp == 0x203A) { // single right angle quote
                    baos.write(0x9B);
                } else if (cp == 0x0153) { // oe ligature
                    baos.write(0x9C);
                } else if (cp == 0x017E) { // z caron
                    baos.write(0x9E);
                } else if (cp == 0x0178) { // Y diaeresis
                    baos.write(0x9F);
                } else if (cp == 0x20AC) { // Euro
                    baos.write(0x80);
                } else if (cp == 0x201A) {
                    baos.write(0x82);
                } else if (cp == 0x0192) {
                    baos.write(0x83);
                } else if (cp == 0x201E) {
                    baos.write(0x84);
                } else if (cp == 0x2026) {
                    baos.write(0x85);
                } else if (cp == 0x2020) {
                    baos.write(0x86);
                } else if (cp == 0x2021) {
                    baos.write(0x87);
                } else if (cp == 0x02C6) {
                    baos.write(0x88);
                } else if (cp == 0x2030) {
                    baos.write(0x89);
                } else if (cp == 0x0160) {
                    baos.write(0x8A);
                } else if (cp == 0x2039) {
                    baos.write(0x8B);
                } else if (cp == 0x0152) {
                    baos.write(0x8C);
                } else if (cp == 0x017D) {
                    baos.write(0x8E);
                } else if (cp <= 0xFF) {
                    baos.write(cp);
                } else {
                    baos.write(cp & 0xFF);
                }
            }

            byte[] bytes = baos.toByteArray();
            String restored = new String(bytes, StandardCharsets.UTF_8);
            if (!restored.contains("\uFFFD")) {
                return restored;
            }
        } catch (Exception ignored) {}

        return str;
    }

    public static boolean containsMojibake(String s) {
        if (s == null) return false;
        return s.contains("Ã") || s.contains("áº") || s.contains("á»") || 
               s.contains("Ä") || s.contains("Å") || s.contains("Â") ||
               s.contains("â€") || s.contains("Ã ") || s.contains("Ã¡");
    }

    @Override
    @Transactional
    public void run(String... args) {
        try {
            log.info("[EncodingRepair] Đang quét và tự động sửa các chuỗi ký tự tiếng Việt bị lỗi mã hóa trong DB...");
            int fixedStores = 0;
            for (Store s : storeRepository.findAll()) {
                boolean changed = false;
                String fixedName = fixMojibake(s.getName());
                if (fixedName != null && !fixedName.equals(s.getName())) {
                    s.setName(fixedName);
                    changed = true;
                }
                String fixedAddr = fixMojibake(s.getAddress());
                if (fixedAddr != null && !fixedAddr.equals(s.getAddress())) {
                    s.setAddress(fixedAddr);
                    changed = true;
                }
                String fixedBio = fixMojibake(s.getBio());
                if (fixedBio != null && !fixedBio.equals(s.getBio())) {
                    s.setBio(fixedBio);
                    changed = true;
                }
                if (changed) {
                    storeRepository.save(s);
                    fixedStores++;
                }
            }

            int fixedCats = 0;
            for (Category c : categoryRepository.findAll()) {
                boolean changed = false;
                String fixedName = fixMojibake(c.getName());
                if (fixedName != null && !fixedName.equals(c.getName())) {
                    java.util.Optional<Category> existing = categoryRepository.findFirstByName(fixedName);
                    if (existing.isPresent() && !existing.get().getId().equals(c.getId())) {
                        log.info("[EncodingRepair] Danh mục '{}' đã có id={}, bỏ qua đổi tên id={}", fixedName, existing.get().getId(), c.getId());
                    } else {
                        c.setName(fixedName);
                        changed = true;
                    }
                }
                String fixedDesc = fixMojibake(c.getDescription());
                if (fixedDesc != null && !fixedDesc.equals(c.getDescription())) {
                    c.setDescription(fixedDesc);
                    changed = true;
                }
                if (changed) {
                    categoryRepository.save(c);
                    fixedCats++;
                }
            }

            int fixedProds = 0;
            for (Product p : productRepository.findAll()) {
                boolean changed = false;
                String fixedName = fixMojibake(p.getName());
                if (fixedName != null && !fixedName.equals(p.getName())) {
                    p.setName(fixedName);
                    changed = true;
                }
                String fixedDesc = fixMojibake(p.getDescription());
                if (fixedDesc != null && !fixedDesc.equals(p.getDescription())) {
                    p.setDescription(fixedDesc);
                    changed = true;
                }
                
                // Sửa ảnh bị lỗi 404 (Unsplash link chết 1548094878) hoặc ảnh bị trùng
                String img = p.getImageUrl();
                String nameLower = (p.getName() != null) ? p.getName().toLowerCase() : "";
                if (img == null || img.contains("1548094878-84ced0f68b08") || img.contains("1490750967868-88df5691cc63")) {
                    p.setImageUrl("https://images.unsplash.com/photo-1562690868-60bbe7293e94?w=800&q=80");
                    changed = true;
                } else if ((nameLower.contains("tana") || nameLower.contains("cúc")) && (img.contains("1561181286") || img.contains("1548094878"))) {
                    p.setImageUrl("https://images.unsplash.com/photo-1606041008023-472dfb5e530f?w=800&q=80");
                    changed = true;
                }

                if (changed) {
                    productRepository.save(p);
                    fixedProds++;
                }
            }

            int fixedUsers = 0;
            for (User u : userRepository.findAll()) {
                boolean changed = false;
                String fixedName = fixMojibake(u.getFullName());
                if (fixedName != null && !fixedName.equals(u.getFullName())) {
                    u.setFullName(fixedName);
                    changed = true;
                }
                String fixedAddr = fixMojibake(u.getAddress());
                if (fixedAddr != null && !fixedAddr.equals(u.getAddress())) {
                    u.setAddress(fixedAddr);
                    changed = true;
                }
                if (changed) {
                    userRepository.save(u);
                    fixedUsers++;
                }
            }

            int fixedVouchers = 0;
            for (Voucher v : voucherRepository.findAll()) {
                String fixedName = fixMojibake(v.getName());
                if (fixedName != null && !fixedName.equals(v.getName())) {
                    v.setName(fixedName);
                    voucherRepository.save(v);
                    fixedVouchers++;
                }
            }

            int fixedDeliveries = 0;
            for (Delivery d : deliveryRepository.findAll()) {
                boolean changed = false;
                String fixedName = fixMojibake(d.getName());
                if (fixedName != null && !fixedName.equals(d.getName())) {
                    d.setName(fixedName);
                    changed = true;
                }
                String fixedDesc = fixMojibake(d.getDescription());
                if (fixedDesc != null && !fixedDesc.equals(d.getDescription())) {
                    d.setDescription(fixedDesc);
                    changed = true;
                }
                if (changed) {
                    deliveryRepository.save(d);
                    fixedDeliveries++;
                }
            }

            int fixedUserLevels = 0;
            for (UserLevel ul : userLevelRepository.findAll()) {
                String fixedName = fixMojibake(ul.getName());
                if (fixedName != null && !fixedName.equals(ul.getName())) {
                    ul.setName(fixedName);
                    userLevelRepository.save(ul);
                    fixedUserLevels++;
                }
            }

            int fixedStoreLevels = 0;
            for (StoreLevel sl : storeLevelRepository.findAll()) {
                String fixedName = fixMojibake(sl.getName());
                if (fixedName != null && !fixedName.equals(sl.getName())) {
                    sl.setName(fixedName);
                    storeLevelRepository.save(sl);
                    fixedStoreLevels++;
                }
            }

            int fixedStyles = 0;
            for (Style st : styleRepository.findAll()) {
                String fixedName = fixMojibake(st.getName());
                if (fixedName != null && !fixedName.equals(st.getName())) {
                    st.setName(fixedName);
                    styleRepository.save(st);
                    fixedStyles++;
                }
            }

            int fixedStyleVals = 0;
            for (StyleValue sv : styleValueRepository.findAll()) {
                String fixedName = fixMojibake(sv.getName());
                if (fixedName != null && !fixedName.equals(sv.getName())) {
                    sv.setName(fixedName);
                    styleValueRepository.save(sv);
                    fixedStyleVals++;
                }
            }

            int fixedOrders = 0;
            for (com.finalproject.entity.Order o : orderRepository.findAll()) {
                boolean changed = false;
                String fixedName = fixMojibake(o.getRecipientName());
                if (fixedName != null && !fixedName.equals(o.getRecipientName())) {
                    o.setRecipientName(fixedName);
                    changed = true;
                }
                String fixedAddr = fixMojibake(o.getRecipientAddress());
                if (fixedAddr != null && !fixedAddr.equals(o.getRecipientAddress())) {
                    o.setRecipientAddress(fixedAddr);
                    changed = true;
                }
                String fixedMsg = fixMojibake(o.getGreetingMessage());
                if (fixedMsg != null && !fixedMsg.equals(o.getGreetingMessage())) {
                    o.setGreetingMessage(fixedMsg);
                    changed = true;
                }
                if (changed) {
                    orderRepository.save(o);
                    fixedOrders++;
                }
            }

            int fixedReviews = 0;
            for (Review r : reviewRepository.findAll()) {
                String fixedContent = fixMojibake(r.getContent());
                if (fixedContent != null && !fixedContent.equals(r.getContent())) {
                    r.setContent(fixedContent);
                    reviewRepository.save(r);
                    fixedReviews++;
                }
            }

            int fixedTx = 0;
            for (Transaction t : transactionRepository.findAll()) {
                String fixedDesc = fixMojibake(t.getDescription());
                if (fixedDesc != null && !fixedDesc.equals(t.getDescription())) {
                    t.setDescription(fixedDesc);
                    transactionRepository.save(t);
                    fixedTx++;
                }
            }

            log.info("[EncodingRepair] Đã phục hồi thành công: stores={}, products={}, categories={}, users={}, vouchers={}, deliveries={}, userLevels={}, storeLevels={}, styles={}, styleVals={}, orders={}, reviews={}, tx={}",
                    fixedStores, fixedProds, fixedCats, fixedUsers, fixedVouchers, fixedDeliveries, fixedUserLevels, fixedStoreLevels, fixedStyles, fixedStyleVals, fixedOrders, fixedReviews, fixedTx);

        } catch (Exception e) {
            log.warn("[EncodingRepair] Gặp lỗi trong quá trình quét sửa encoding: {}", e.getMessage(), e);
        }
    }
}
