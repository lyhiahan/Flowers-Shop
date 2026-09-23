package com.finalproject.service.impl;

import com.finalproject.entity.User;
import com.finalproject.entity.UserAddress;
import com.finalproject.repository.UserAddressRepository;
import com.finalproject.repository.UserRepository;
import com.finalproject.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserAddressRepository userAddressRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng: " + username));
    }

    @Override
    @Transactional
    public User updateProfile(String username, String fullName, String phone, String address, String avatarUrl, String cover) {
        User user = findByUsername(username);
        if (fullName != null && !fullName.trim().isEmpty()) {
            user.setFullName(fullName.trim());
        }
        if (phone != null) user.setPhone(phone.trim());
        if (address != null) user.setAddress(address.trim());
        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) user.setAvatarUrl(avatarUrl.trim());
        if (cover != null && !cover.trim().isEmpty()) user.setCover(cover.trim());
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        User user = findByUsername(username);
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác!");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự!");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public List<UserAddress> getUserAddresses(Long userId) {
        return userAddressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
    }

    @Override
    @Transactional
    public UserAddress addAddress(Long userId, String recipientName, String phone, String addressDetail, Boolean isDefault) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));

        if (Boolean.TRUE.equals(isDefault)) {
            clearDefaultAddress(userId);
        } else {
            // Nếu là địa chỉ đầu tiên thì tự set làm mặc định
            List<UserAddress> existing = userAddressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
            if (existing.isEmpty()) isDefault = true;
        }

        UserAddress address = UserAddress.builder()
                .user(user)
                .recipientName(recipientName)
                .phone(phone)
                .addressDetail(addressDetail)
                .isDefault(isDefault != null && isDefault)
                .build();

        return userAddressRepository.save(address);
    }

    @Override
    @Transactional
    public UserAddress updateAddress(Long addressId, Long userId, String recipientName, String phone, String addressDetail, Boolean isDefault) {
        UserAddress address = userAddressRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Địa chỉ không tồn tại"));

        if (!address.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Không có quyền chỉnh sửa địa chỉ này");
        }

        if (Boolean.TRUE.equals(isDefault)) {
            clearDefaultAddress(userId);
            address.setIsDefault(true);
        }

        address.setRecipientName(recipientName);
        address.setPhone(phone);
        address.setAddressDetail(addressDetail);

        return userAddressRepository.save(address);
    }

    @Override
    @Transactional
    public void deleteAddress(Long addressId, Long userId) {
        UserAddress address = userAddressRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Địa chỉ không tồn tại"));

        if (!address.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Không có quyền xóa địa chỉ này");
        }

        userAddressRepository.delete(address);
    }

    @Override
    @Transactional
    public void setDefaultAddress(Long addressId, Long userId) {
        UserAddress address = userAddressRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Địa chỉ không tồn tại"));

        if (!address.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Không có quyền thao tác địa chỉ này");
        }

        clearDefaultAddress(userId);
        address.setIsDefault(true);
        userAddressRepository.save(address);
    }

    private void clearDefaultAddress(Long userId) {
        userAddressRepository.findByUserIdAndIsDefaultTrue(userId)
                .ifPresent(addr -> {
                    addr.setIsDefault(false);
                    userAddressRepository.save(addr);
                });
    }
}
