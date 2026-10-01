package com.finalproject.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class ProfileUpdateRequest {
    @NotBlank(message = "Vui lòng điền họ và tên")
    private String fullName;

    private String phone;
    private String address;
    private MultipartFile avatarFile;
}