package com.finalproject.service;

import com.finalproject.entity.User;
import com.finalproject.entity.UserAddress;

import java.util.List;

public interface UserService {
    User updateProfileImages(String username, String fullName, String phone, String address,
            org.springframework.web.multipart.MultipartFile avatar, org.springframework.web.multipart.MultipartFile cover) throws java.io.IOException;
    User findByUsername(String username);
    User updateProfile(String username, String fullName, String phone, String address, String avatarUrl, String cover);
    void changePassword(String username, String oldPassword, String newPassword);
    
    // Sổ địa chỉ
    List<UserAddress> getUserAddresses(Long userId);
    UserAddress addAddress(Long userId, String recipientName, String phone, String addressDetail, Boolean isDefault);
    UserAddress updateAddress(Long addressId, Long userId, String recipientName, String phone, String addressDetail, Boolean isDefault);
    void deleteAddress(Long addressId, Long userId);
    void setDefaultAddress(Long addressId, Long userId);
}
