package com.finalproject.controller.manager;

import com.finalproject.entity.Store;
import com.finalproject.entity.User;
import com.finalproject.repository.StoreRepository;
import com.finalproject.repository.UserRepository;
import com.finalproject.service.CloudinaryService;
import com.finalproject.service.StoreAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller thực hiện Use Case UC_019: Quản lý thông tin của cửa hàng (Store Management – Profile)
 */
@Controller
@RequestMapping({"/manager/profile", "/manager/stores/profile", "/vendor/profile"})
@RequiredArgsConstructor
@Slf4j
public class ManagerProfileController {

    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final StoreAccessService storeAccess;
    private final CloudinaryService cloudinaryService;

    /** Bước 6 & 7: Vào trang quản lý thông tin cửa hàng từ sidebar */
    @GetMapping
    public String showStoreProfile(Principal principal, Model model) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        Store store = getActiveStore(user);
        if (store == null) return "redirect:/manager/stores";
        storeAccess.requireStore(store);

        model.addAttribute("store", store);
        model.addAttribute("featuredImages", parseFeaturedImages(store.getFeaturedImages()));
        model.addAttribute("activeMenu", "profile");
        model.addAttribute("currentUser", user);
        return "manager/profile";
    }

    /**
     * Main Flow – Cập nhật ảnh đại diện cửa hàng.
     * Validation định dạng/kích thước được thực hiện trong CloudinaryServiceImpl.
     */
    @PostMapping("/avatar")
    public String updateAvatar(Principal principal,
                               @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
                               @RequestParam(value = "avatarUrl", required = false) String avatarUrl,
                               RedirectAttributes ra) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        Store store = getActiveStore(user);
        if (store == null) return "redirect:/manager/stores";
        storeAccess.requireManager(store);

        try {
            String newUrl = null;
            String newPublicId = null;

            if (avatarFile != null && !avatarFile.isEmpty()) {
                CloudinaryService.UploadResult result = cloudinaryService.upload(avatarFile, "rental_shop_stores/avatars");
                if (result != null) {
                    newUrl = result.secureUrl();
                    newPublicId = result.publicId();
                }
            } else if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
                if (!isValidImageUrl(avatarUrl)) {
                    ra.addFlashAttribute("errorMessage", "Hình ảnh không hợp lệ! Vui lòng nhập đường dẫn hình ảnh trực tuyến hợp lệ.");
                    return "redirect:/manager/profile";
                }
                newUrl = avatarUrl.trim();
            } else {
                ra.addFlashAttribute("errorMessage", "Hình ảnh không hợp lệ! Vui lòng chọn một tệp ảnh đại diện mới.");
                return "redirect:/manager/profile";
            }

            // Xóa ảnh cũ trên Cloudinary (best-effort, không throw)
            cloudinaryService.delete(store.getAvatarPublicId());

            store.setAvatar(newUrl);
            store.setAvatarPublicId(newPublicId);
            storeRepository.save(store);
            ra.addFlashAttribute("successMessage", "Ảnh đại diện cửa hàng đã được cập nhật thành công!");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            log.error("Lỗi cập nhật ảnh đại diện", ex);
            ra.addFlashAttribute("errorMessage", "Cập nhật thất bại: " + ex.getMessage());
        }

        return "redirect:/manager/profile";
    }

    /** Cập nhật ảnh bìa (Cover Photo) */
    @PostMapping("/cover")
    public String updateCover(Principal principal,
                              @RequestParam(value = "coverFile", required = false) MultipartFile coverFile,
                              @RequestParam(value = "coverUrl", required = false) String coverUrl,
                              RedirectAttributes ra) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        Store store = getActiveStore(user);
        if (store == null) return "redirect:/manager/stores";
        storeAccess.requireManager(store);

        try {
            String newUrl = null;
            String newPublicId = null;

            if (coverFile != null && !coverFile.isEmpty()) {
                CloudinaryService.UploadResult result = cloudinaryService.upload(coverFile, "rental_shop_stores/covers");
                if (result != null) {
                    newUrl = result.secureUrl();
                    newPublicId = result.publicId();
                }
            } else if (coverUrl != null && !coverUrl.trim().isEmpty()) {
                if (!isValidImageUrl(coverUrl)) {
                    ra.addFlashAttribute("errorMessage", "Hình ảnh không hợp lệ! Vui lòng nhập đường dẫn hình ảnh trực tuyến hợp lệ.");
                    return "redirect:/manager/profile";
                }
                newUrl = coverUrl.trim();
            } else {
                ra.addFlashAttribute("errorMessage", "Hình ảnh không hợp lệ! Vui lòng chọn một tệp ảnh bìa mới.");
                return "redirect:/manager/profile";
            }

            cloudinaryService.delete(store.getCoverPublicId());

            store.setCover(newUrl);
            store.setCoverPublicId(newPublicId);
            storeRepository.save(store);
            ra.addFlashAttribute("successMessage", "Ảnh bìa cửa hàng đã được cập nhật thành công!");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            log.error("Lỗi cập nhật ảnh bìa", ex);
            ra.addFlashAttribute("errorMessage", "Cập nhật thất bại: " + ex.getMessage());
        }

        return "redirect:/manager/profile";
    }

    /** Thêm ảnh đặc trưng mới */
    @PostMapping("/featured/add")
    public String addFeaturedImage(Principal principal,
                                   @RequestParam(value = "featuredFile", required = false) MultipartFile featuredFile,
                                   @RequestParam(value = "imageUrl", required = false) String imageUrl,
                                   RedirectAttributes ra) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        Store store = getActiveStore(user);
        if (store == null) return "redirect:/manager/stores";
        storeAccess.requireManager(store);

        try {
            String newUrl = null;

            if (featuredFile != null && !featuredFile.isEmpty()) {
                CloudinaryService.UploadResult result = cloudinaryService.upload(featuredFile, "rental_shop_stores/featured");
                if (result != null) newUrl = result.secureUrl();
            } else if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                if (!isValidImageUrl(imageUrl)) {
                    ra.addFlashAttribute("errorMessage", "Hình ảnh không hợp lệ! Vui lòng nhập đường dẫn hình ảnh trực tuyến hợp lệ.");
                    return "redirect:/manager/profile";
                }
                newUrl = imageUrl.trim();
            } else {
                ra.addFlashAttribute("errorMessage", "Hình ảnh không hợp lệ! Vui lòng chọn tệp hoặc nhập link ảnh đặc trưng.");
                return "redirect:/manager/profile";
            }

            List<String> list = parseFeaturedImages(store.getFeaturedImages());
            list.add(newUrl);
            store.setFeaturedImages(joinFeaturedImages(list));
            storeRepository.save(store);
            ra.addFlashAttribute("successMessage", "Ảnh đặc trưng mới đã được thêm thành công!");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            log.error("Lỗi thêm ảnh đặc trưng", ex);
            ra.addFlashAttribute("errorMessage", "Cập nhật thất bại: " + ex.getMessage());
        }

        return "redirect:/manager/profile";
    }

    /** Chỉnh sửa ảnh đặc trưng theo index */
    @PostMapping("/featured/edit")
    public String editFeaturedImage(Principal principal,
                                    @RequestParam("index") int index,
                                    @RequestParam(value = "editFile", required = false) MultipartFile editFile,
                                    @RequestParam(value = "editUrl", required = false) String editUrl,
                                    RedirectAttributes ra) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        Store store = getActiveStore(user);
        if (store == null) return "redirect:/manager/stores";
        storeAccess.requireManager(store);

        try {
            List<String> list = parseFeaturedImages(store.getFeaturedImages());
            if (index < 0 || index >= list.size()) {
                ra.addFlashAttribute("errorMessage", "Không tìm thấy ảnh đặc trưng cần chỉnh sửa!");
                return "redirect:/manager/profile";
            }

            String updatedUrl = null;
            if (editFile != null && !editFile.isEmpty()) {
                CloudinaryService.UploadResult result = cloudinaryService.upload(editFile, "rental_shop_stores/featured");
                if (result != null) updatedUrl = result.secureUrl();
            } else if (editUrl != null && !editUrl.trim().isEmpty()) {
                if (!isValidImageUrl(editUrl)) {
                    ra.addFlashAttribute("errorMessage", "Hình ảnh không hợp lệ! Vui lòng nhập đường dẫn hình ảnh hợp lệ.");
                    return "redirect:/manager/profile";
                }
                updatedUrl = editUrl.trim();
            } else {
                ra.addFlashAttribute("errorMessage", "Hình ảnh không hợp lệ! Vui lòng chọn ảnh mới để thay thế.");
                return "redirect:/manager/profile";
            }

            list.set(index, updatedUrl);
            store.setFeaturedImages(joinFeaturedImages(list));
            storeRepository.save(store);
            ra.addFlashAttribute("successMessage", "Ảnh đặc trưng đã được cập nhật thành công!");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            log.error("Lỗi sửa ảnh đặc trưng", ex);
            ra.addFlashAttribute("errorMessage", "Cập nhật thất bại: " + ex.getMessage());
        }

        return "redirect:/manager/profile";
    }

    /** Xóa ảnh đặc trưng theo index */
    @PostMapping("/featured/delete")
    public String deleteFeaturedImage(Principal principal,
                                      @RequestParam("index") int index,
                                      RedirectAttributes ra) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        Store store = getActiveStore(user);
        if (store == null) return "redirect:/manager/stores";
        storeAccess.requireManager(store);

        try {
            List<String> list = parseFeaturedImages(store.getFeaturedImages());
            if (index >= 0 && index < list.size()) {
                list.remove(index);
                store.setFeaturedImages(joinFeaturedImages(list));
                storeRepository.save(store);
                ra.addFlashAttribute("successMessage", "Ảnh đặc trưng xóa thành công!");
            } else {
                ra.addFlashAttribute("errorMessage", "Không tìm thấy ảnh đặc trưng cần xóa!");
            }
        } catch (Exception ex) {
            log.error("Lỗi xóa ảnh đặc trưng", ex);
            ra.addFlashAttribute("errorMessage", "Cập nhật thất bại: " + ex.getMessage());
        }

        return "redirect:/manager/profile";
    }

    /** Cập nhật thông tin văn bản cửa hàng (tên, bio, SĐT, địa chỉ, trạng thái mở cửa) */
    @PostMapping("/info")
    public String updateStoreInfo(Principal principal,
                                  @RequestParam("name") String name,
                                  @RequestParam(value = "bio", required = false) String bio,
                                  @RequestParam(value = "phone", required = false) String phone,
                                  @RequestParam(value = "address", required = false) String address,
                                  @RequestParam(value = "isOpen", defaultValue = "false") boolean isOpen,
                                  RedirectAttributes ra) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        Store store = getActiveStore(user);
        if (store == null) return "redirect:/manager/stores";
        storeAccess.requireManager(store);

        String trimmedName = name != null ? name.trim() : "";
        if (trimmedName.isEmpty() || trimmedName.length() < 3 || trimmedName.length() > 120) {
            ra.addFlashAttribute("errorMessage", "Tên cửa hàng phải có độ dài từ 3 đến 120 ký tự!");
            return "redirect:/manager/profile";
        }

        String cleanPhone = null;
        if (phone != null && !phone.trim().isEmpty()) {
            cleanPhone = phone.trim().replaceAll("[\\s.-]", "");
            if (!cleanPhone.matches("^(?:\\+84|84|0)(?:(?:3[2-9]|5[25689]|7[06-9]|8[1-9]|9[0-9])\\d{7}|2\\d{9})$")) {
                ra.addFlashAttribute("errorMessage", "Số điện thoại không đúng chuẩn Việt Nam! Vui lòng nhập số hợp lệ (ví dụ: 0901234567 hoặc 02838123456).");
                return "redirect:/manager/profile";
            }
        }

        String trimmedAddress = address != null ? address.trim() : null;
        if (trimmedAddress != null && !trimmedAddress.isEmpty() && (trimmedAddress.length() < 5 || trimmedAddress.length() > 200)) {
            ra.addFlashAttribute("errorMessage", "Địa chỉ cửa hàng phải có ít nhất 5 ký tự và tối đa 200 ký tự!");
            return "redirect:/manager/profile";
        }

        try {
            store.setName(trimmedName);
            store.setBio(bio != null ? bio.trim() : null);
            if (cleanPhone != null) store.setPhone(cleanPhone);
            if (trimmedAddress != null) store.setAddress(trimmedAddress);
            store.setIsOpen(isOpen);
            storeRepository.save(store);
            ra.addFlashAttribute("successMessage", "Thông tin cửa hàng đã được lưu thành công!");
        } catch (Exception ex) {
            log.error("Lỗi cập nhật thông tin cửa hàng", ex);
            ra.addFlashAttribute("errorMessage", "Cập nhật thất bại: " + ex.getMessage());
        }

        return "redirect:/manager/profile";
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private Store getActiveStore(User user) {
        if (user.getStore() != null) return user.getStore();
        List<Store> owned = storeRepository.findByOwnerId(user.getId());
        if (!owned.isEmpty()) {
            Store first = owned.get(0);
            user.setStore(first);
            userRepository.save(user);
            return first;
        }
        return null;
    }

    private boolean isValidImageUrl(String url) {
        if (url == null || url.trim().isEmpty()) return false;
        String u = url.trim().toLowerCase();
        return u.startsWith("http://") || u.startsWith("https://")
                || u.startsWith("/upload/") || u.startsWith("/images/");
    }

    private List<String> parseFeaturedImages(String raw) {
        if (raw == null || raw.trim().isEmpty()) return new ArrayList<>();
        String[] parts = raw.split("[,\\r\\n]+");
        List<String> list = new ArrayList<>();
        for (String p : parts) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) list.add(trimmed);
        }
        return list;
    }

    private String joinFeaturedImages(List<String> list) {
        if (list == null || list.isEmpty()) return "";
        return String.join(",", list);
    }
}
