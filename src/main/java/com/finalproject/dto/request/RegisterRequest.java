package com.finalproject.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Vui lòng điền tên đăng nhập")
    @Size(min = 4, max = 50, message = "Tên đăng nhập phải từ 4 đến 50 ký tự")
    @Pattern(regexp = "^[a-zA-Z0-9_.]+$", message = "Tên đăng nhập chỉ bao gồm chữ cái, số, dấu gạch dưới hoặc dấu chấm (không chứa dấu cách)")
    private String username;

    @NotBlank(message = "Vui lòng điền mật khẩu")
    @Size(min = 6, max = 100, message = "Mật khẩu phải từ 6 ký tự trở lên")
    private String password;

    @NotBlank(message = "Vui lòng điền họ và tên")
    @Size(min = 2, max = 100, message = "Họ và tên phải từ 2 đến 100 ký tự")
    private String fullName;

    @NotBlank(message = "Vui lòng điền địa chỉ email")
    @Email(message = "Email không đúng định dạng (VD: example@domain.com)")
    private String email;

    @NotBlank(message = "Vui lòng điền số điện thoại")
    @Pattern(regexp = "^(0|\\+?84)[35789][0-9]{8}$", message = "Số điện thoại không đúng chuẩn Việt Nam (10 chữ số, bắt đầu bằng 03, 05, 07, 08, 09)")
    private String phone;

    private String province;

    private String district;

    private String detailAddress;

    private String address;

    public String getFullAddress() {
        if (address != null && !address.trim().isEmpty()) {
            return address.trim();
        }
        StringBuilder sb = new StringBuilder();
        if (detailAddress != null && !detailAddress.trim().isEmpty()) {
            sb.append(detailAddress.trim()).append(", ");
        }
        if (district != null && !district.trim().isEmpty()) {
            sb.append(district.trim()).append(", ");
        }
        if (province != null && !province.trim().isEmpty()) {
            sb.append(province.trim());
        }
        return sb.toString();
    }
}