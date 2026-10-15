package com.finalproject.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductForm {

    private Long id;
    private List<Long> styleValueIds;
    private boolean attributesSubmitted;
    private boolean flowerAttributesSubmitted;
    private String flowerTypes;
    private String flowerColors;
    private String flowerStyles;

    @NotNull(message = "Vui lòng chọn danh mục sản phẩm")
    private Long categoryId;

    private List<Long> additionalCategoryIds;
    private boolean categoriesSubmitted;

    @NotBlank(message = "Vui lòng điền tên sản phẩm")
    @jakarta.validation.constraints.Size(max = 200, message = "Tên sản phẩm tối đa 200 ký tự")
    private String name;

    private String description;

    @NotNull(message = "Vui lòng điền giá nhập hàng")
    @DecimalMin(value = "1", message = "Giá nhập hàng phải từ 1đ")
    @jakarta.validation.constraints.Digits(integer = 16, fraction = 0, message = "Giá nhập phải là số đồng nguyên, tối đa 16 chữ số")
    private BigDecimal importPrice;

    @NotNull(message = "Vui lòng điền giá bán")
    @DecimalMin(value = "1", message = "Giá bán phải từ 1đ")
    @jakarta.validation.constraints.Digits(integer = 16, fraction = 0, message = "Giá bán phải là số đồng nguyên, tối đa 16 chữ số")
    private BigDecimal priceBuy;

    // JDBC commonly returns 120000.00; normalize insignificant zeros before validation.
    public void setPriceBuy(BigDecimal value) { priceBuy = normalizeDong(value); }
    public void setImportPrice(BigDecimal value) { importPrice = normalizeDong(value); }
    private BigDecimal normalizeDong(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().scale() <= 0 ? value.setScale(0) : value;
    }

    private BigDecimal priceRentPerDay = BigDecimal.ZERO;

    private BigDecimal depositFee = BigDecimal.ZERO;

    @jakarta.validation.constraints.Min(0)
    @jakarta.validation.constraints.Max(1000000)
    private int initialStock = 0;

    // Danh sách ảnh tải lên Cloudinary
    private List<MultipartFile> imageFiles;

    // Cờ xác nhận bán lỗ có chủ đích khi Admin chọn [YES] trên cảnh báo Modal
    private boolean confirmSellAtLoss = false;

    // Cửa hàng hoặc phạm vi áp dụng (null = Toàn hệ thống)
    private Long storeId;

    // Trạng thái kinh doanh / Trạng thái bán
    private Boolean isSelling = true;

    // Trạng thái cấp phép / hoạt động
    private Boolean isActive = true;

    // Ảnh chính hiện tại (dùng để xem trước khi chỉnh sửa)
    private String imageUrl;

    // Số lượng đã bán (chỉ đọc, tính từ đơn hàng)
    private Integer sold = 0;

    // Đường dẫn quay lại sau khi lưu / hủy để giữ nguyên bộ lọc và phân trang
    private String returnUrl;
}
