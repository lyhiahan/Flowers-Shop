package com.finalproject.service;

import com.finalproject.entity.User;
import com.finalproject.entity.UserAddress;
import com.finalproject.repository.UserAddressRepository;
import com.finalproject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserAddressRepository userAddressRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.finalproject.service.CloudinaryService cloud;
    private final jakarta.persistence.EntityManager entityManager;

     @Transactional(rollbackFor=java.io.IOException.class)
    public User updateProfileImages(String username, String name, String phone, String address,
            org.springframework.web.multipart.MultipartFile avatar, org.springframework.web.multipart.MultipartFile cover) throws java.io.IOException {
        User user = findByUsername(username);
        userRepository.findLockedById(user.getId()).orElseThrow();
        entityManager.refresh(user, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (!Boolean.TRUE.equals(user.getIsActive()) || Boolean.TRUE.equals(user.getAccountDisabledByAdmin()) || user.isRetiredDeliveryAccount())
            throw new org.springframework.security.access.AccessDeniedException("Tài khoản đã bị vô hiệu hóa.");
        // Only submitted fields change; old images remain available until the database commit.
        if (name == null || name.isBlank() || name.trim().length() > 100
                || phone != null && !phone.trim().matches("0[35789][0-9]{8}")
                || address != null && (address.isBlank() || address.trim().length() > 255))
            throw new IllegalArgumentException("Họ tên, số điện thoại hoặc địa chỉ không hợp lệ.");
        user.setFullName(name.trim());
        if (phone != null) user.setPhone(phone.trim());
        if (address != null) user.setAddress(address.trim());
        if (avatar != null && !avatar.isEmpty()) {
            var uploaded = uploadProfileImage(avatar, "rental_shop_avatars", user.getAvatarPublicId());
            user.setAvatarUrl(uploaded.secureUrl()); user.setAvatarPublicId(uploaded.publicId());
        }
        if (cover != null && !cover.isEmpty()) {
            var uploaded = uploadProfileImage(cover, "rental_shop_covers", user.getCoverPublicId());
            user.setCover(uploaded.secureUrl()); user.setCoverPublicId(uploaded.publicId());
        }
        return userRepository.save(user);
    }

    private com.finalproject.service.CloudinaryService.UploadResult uploadProfileImage(
            org.springframework.web.multipart.MultipartFile file, String folder, String previousId) throws java.io.IOException {
        var uploaded = cloud.upload(file, folder);
        if (uploaded == null || uploaded.secureUrl() == null || uploaded.secureUrl().isBlank()
                || uploaded.publicId() == null || uploaded.publicId().isBlank())
            throw new java.io.IOException("Không tải được ảnh mới. Vui lòng thử lại.");
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                     public void afterCompletion(int status) {
                        if (java.util.Objects.equals(previousId, uploaded.publicId())) return;
                        if (status == STATUS_COMMITTED && previousId != null && !previousId.isBlank()) cloud.delete(previousId);
                        else if (status == STATUS_ROLLED_BACK) cloud.delete(uploaded.publicId());
                    }
                });
        return uploaded;
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng: " + username));
    }

    @Transactional
    public User updateProfile(String username, String fullName, String phone, String address, String avatarUrl, String cover) {
        User user = findByUsername(username);
        if (fullName == null || fullName.isBlank() || fullName.trim().length() > 100 || phone == null || !phone.trim().matches("0[35789][0-9]{8}") || address == null || address.isBlank() || address.trim().length() > 255)
            throw new IllegalArgumentException("Họ tên, số điện thoại hoặc địa chỉ không hợp lệ.");
        if (fullName != null && !fullName.trim().isEmpty()) {
            user.setFullName(fullName.trim());
        }
        if (phone != null) user.setPhone(phone.trim());
        if (address != null) user.setAddress(address.trim());
        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) user.setAvatarUrl(avatarUrl.trim());
        if (cover != null && !cover.trim().isEmpty()) user.setCover(cover.trim());
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        User user = findByUsername(username);
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác!");
        }
        if (newPassword == null || newPassword.trim().length() < 6 || newPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự!");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public List<UserAddress> getUserAddresses(Long userId) {
        return userAddressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
    }

    private String normalizeAddress(String input) {
        if (input == null) return "";
        String nfd = Normalizer.normalize(input, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String noAccent = pattern.matcher(nfd).replaceAll("").replace('đ', 'd').replace('Đ', 'd');
        return noAccent.toLowerCase()
                .replaceAll("[,.\\-\\/#]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    @Transactional
    public UserAddress addAddress(Long userId, String recipientName, String phone, String addressDetail, Boolean isDefault) {
        User user = userRepository.findLockedById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));

        if (recipientName == null || recipientName.trim().isEmpty() ||
            phone == null || phone.trim().isEmpty() ||
            addressDetail == null || addressDetail.trim().isEmpty()) {
            throw new IllegalArgumentException("Thông báo thêm thất bại: Vui lòng điền đầy đủ các thông tin địa chỉ!");
        }

        // Số điện thoại phải đúng 10 chữ số
        if (!phone.trim().matches("^[0-9]{10}$")) {
            throw new IllegalArgumentException("Thông báo thêm thất bại: Số điện thoại không hợp lệ (phải đúng 10 chữ số)");
        }

        validateAddress(recipientName, phone, addressDetail);
        String normDetail = normalizeAddress(addressDetail);
        List<UserAddress> existing = userAddressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
        boolean duplicate = existing.stream().anyMatch(a ->
                normalizeAddress(a.getAddressDetail()).equals(normDetail)
        );
        if (duplicate) {
            throw new IllegalArgumentException("Thông báo thêm thất bại: Địa chỉ này đã tồn tại trong sổ địa chỉ của bạn!");
        }

        if (Boolean.TRUE.equals(isDefault)) {
            clearDefaultAddress(userId);
        } else {
            // Nếu là địa chỉ đầu tiên thì tự set làm mặc định
            if (existing.isEmpty()) isDefault = true;
        }

        UserAddress address = UserAddress.builder()
                .user(user)
                .recipientName(recipientName.trim())
                .phone(phone.trim())
                .addressDetail(addressDetail.trim())
                .isDefault(isDefault != null && isDefault)
                .build();

        return userAddressRepository.save(address);
    }

    @Transactional
    public UserAddress updateAddress(Long addressId, Long userId, String recipientName, String phone, String addressDetail, Boolean isDefault) {
        userRepository.findLockedById(userId).orElseThrow();
        UserAddress address = userAddressRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Địa chỉ không tồn tại"));

        if (!address.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Không có quyền chỉnh sửa địa chỉ này");
        }

        validateAddress(recipientName, phone, addressDetail);
        if (userAddressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId).stream().anyMatch(a -> !a.getId().equals(addressId) && normalizeAddress(a.getAddressDetail()).equals(normalizeAddress(addressDetail))))
            throw new IllegalArgumentException("Địa chỉ này đã tồn tại.");
        if (Boolean.TRUE.equals(isDefault)) {
            clearDefaultAddress(userId);
            address.setIsDefault(true);
        }

        address.setRecipientName(recipientName.trim());
        address.setPhone(phone.trim());
        address.setAddressDetail(addressDetail.trim());

        return userAddressRepository.save(address);
    }

    @Transactional
    public void deleteAddress(Long addressId, Long userId) {
        userRepository.findLockedById(userId).orElseThrow();
        UserAddress address = userAddressRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Địa chỉ không tồn tại"));

        if (!address.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Không có quyền xóa địa chỉ này");
        }

        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());
        userAddressRepository.delete(address);
        userAddressRepository.flush();
        if (wasDefault) userAddressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId).stream().findFirst().ifPresent(next -> {next.setIsDefault(true); userAddressRepository.save(next);});
    }

    @Transactional
    public void setDefaultAddress(Long addressId, Long userId) {
        userRepository.findLockedById(userId).orElseThrow();
        UserAddress address = userAddressRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Địa chỉ không tồn tại"));

        if (!address.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Không có quyền thao tác địa chỉ này");
        }

        clearDefaultAddress(userId);
        address.setIsDefault(true);
        userAddressRepository.save(address);
    }

    private void validateAddress(String name, String phone, String detail) {
        if (name == null || name.isBlank() || name.trim().length() > 100 || phone == null || !phone.trim().matches("0[35789][0-9]{8}") || detail == null || detail.isBlank() || detail.trim().length() > 255)
            throw new IllegalArgumentException("Nhập đầy đủ tên (tối đa 100), số điện thoại Việt Nam và địa chỉ (tối đa 255 ký tự).");
    }

    private void clearDefaultAddress(Long userId) {
        userAddressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId).stream().filter(a -> Boolean.TRUE.equals(a.getIsDefault()))
                .forEach(addr -> {
                    addr.setIsDefault(false);
                    userAddressRepository.save(addr);
                });
    }
}
