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

    @NotBlank(message = "Vui lòng nhập tên sản phẩm, trường này không được bỏ trống.")
    private String name;

    private String description;

    @NotNull(message = "Giá nhập hàng không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá nhập hàng phải lớn hơn 0")
    private BigDecimal importPrice;

    @NotNull(message = "Giá bán đứt không được để trống")
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
}
