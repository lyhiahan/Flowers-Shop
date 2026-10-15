package com.finalproject.utils;

import java.util.List;

/** Shared with demo/reset flows so legacy species/container categories do not return. */
public final class FlowerCategoryDefaults {
    private FlowerCategoryDefaults() {}
    public record Definition(String name, String description) {}
    public static List<Definition> definitions() {
        return List.of(
            new Definition("Hoa Tình Yêu", "Hoa dành cho tình yêu, kỷ niệm và Valentine."),
            new Definition("Hoa Sinh Nhật", "Hoa chúc mừng sinh nhật."),
            new Definition("Hoa Cưới", "Hoa dành cho lễ cưới và cô dâu."),
            new Definition("Hoa Khai Trương", "Hoa chúc mừng khai trương."),
            new Definition("Hoa Tốt Nghiệp", "Hoa chúc mừng tốt nghiệp."),
            new Definition("Hoa Chúc Mừng", "Hoa chúc mừng thành công và các dịp đặc biệt."),
            new Definition("Hoa Chia Buồn", "Hoa chia buồn và kính viếng."),
            new Definition("Hoa Tân Gia", "Hoa mừng nhà mới."),
            new Definition("Hoa Trang Trí", "Hoa và phụ kiện trang trí không gian, sự kiện."),
            new Definition("Hoa Quà Tặng", "Hoa làm quà tặng; chọn thêm dịp phù hợp khi chỉnh sửa sản phẩm.")
        );
    }
}
