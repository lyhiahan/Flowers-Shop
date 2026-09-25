package com.finalproject.controller.manager;

import com.finalproject.entity.Store;
import com.finalproject.entity.User;
import com.finalproject.repository.StoreRepository;
import com.finalproject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping({"/manager/stores", "/vendor/stores"})
@RequiredArgsConstructor
public class ManagerStoreController {

    private final StoreRepository storeRepository;
    private final UserRepository userRepository;

    @GetMapping
    public String listStores(Principal principal, Model model) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        List<Store> ownedStores = storeRepository.findByOwnerId(user.getId());
        java.util.Set<Store> allStores = new java.util.LinkedHashSet<>(ownedStores);
        if (user.getStore() != null) {
            allStores.add(user.getStore());
        }
        if (allStores.isEmpty()) {
            allStores.addAll(storeRepository.findAll());
        }
        model.addAttribute("stores", new java.util.ArrayList<>(allStores));
        model.addAttribute("currentUser", user);
        return "manager/stores";
    }

    @GetMapping("/create")
    public String showCreateStoreForm(Model model) {
        model.addAttribute("store", new Store());
        return "manager/store-create";
    }

    @PostMapping("/create")
    public String createStore(Principal principal,
                              @ModelAttribute("store") Store store,
                              RedirectAttributes ra) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        store.setOwner(user);
        store.setStatus(true);
        store.setIsOpen(true);
        store.setRating(5.0);
        storeRepository.save(store);

        // Gán chi nhánh làm việc nếu chưa có
        if (user.getStore() == null) {
            user.setStore(store);
            userRepository.save(user);
        }

        ra.addFlashAttribute("successMessage", "Tạo cửa hàng hoa [" + store.getName() + "] thành công!");
        return "redirect:/manager/stores";
    }

    @GetMapping("/edit/{id}")
    public String showEditStoreForm(@PathVariable("id") Long id, Model model) {
        Store store = storeRepository.findById(id).orElseThrow();
        model.addAttribute("store", store);
        return "manager/store-info";
    }

    @PostMapping("/edit/{id}")
    public String updateStore(@PathVariable("id") Long id,
                              @ModelAttribute("store") Store formStore,
                              RedirectAttributes ra) {
        Store store = storeRepository.findById(id).orElseThrow();
        store.setName(formStore.getName());
        store.setPhone(formStore.getPhone());
        store.setAddress(formStore.getAddress());
        store.setBio(formStore.getBio());
        store.setAvatar(formStore.getAvatar());
        store.setCover(formStore.getCover());
        store.setFeaturedImages(formStore.getFeaturedImages());
        store.setIsOpen(formStore.getIsOpen());

        storeRepository.save(store);
        ra.addFlashAttribute("successMessage", "Cập nhật thông tin cửa hàng thành công!");
        return "redirect:/manager/stores";
    }
}
