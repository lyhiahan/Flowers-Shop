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

    @NotNull(message = "Vui lòng chọn danh mục sản phẩm")
    private Long categoryId;

    @NotBlank(message = "Vui lòng điền tên sản phẩm")
    private String name;

    private String description;

    @NotNull(message = "Vui lòng điền giá nhập hàng")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá nhập hàng phải lớn hơn 0")
    private BigDecimal importPrice;

    @NotNull(message = "Vui lòng điền giá bán")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá bán đứt phải là số dương lớn hơn 0. Vui lòng nhập lại.")
    private BigDecimal priceBuy;

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
