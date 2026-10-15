package com.finalproject.service;

import com.finalproject.entity.*;
import com.finalproject.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DemoStyleSeeder {
    private final StyleRepository styleRepository;
    private final StyleValueRepository styleValueRepository;

    @Transactional
    public void seed() {
        // Existing names and deletion status are managed by users; only supplement missing defaults.
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
                    .filter(s -> s.getName().equalsIgnoreCase(groupName)
                            || groupName.equals("Kiểu dáng") && java.util.Set.of("kiểu dáng cắm", "kiểu dáng hoa", "phong cách cắm").contains(s.getName().trim().toLowerCase(java.util.Locale.ROOT))
                            || groupName.equals("Màu sắc") && java.util.Set.of("tone màu hoa", "tone màu thiết kế").contains(s.getName().trim().toLowerCase(java.util.Locale.ROOT))
                            || groupName.equals("Kích cỡ") && java.util.Set.of("kích cỡ bó hoa", "kích cỡ thiết kế").contains(s.getName().trim().toLowerCase(java.util.Locale.ROOT)))
                    .findFirst()
                    .orElseGet(() -> styleRepository.save(Style.builder()
                            .name(groupName)
                            .isDeleted(false)
                            .build()));

            if (Boolean.TRUE.equals(style.getIsDeleted())) continue;

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
