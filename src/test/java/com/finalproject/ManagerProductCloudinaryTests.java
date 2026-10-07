package com.finalproject;

import com.finalproject.controller.manager.ManagerProductController;
import com.finalproject.entity.*;
import com.finalproject.repository.*;
import com.finalproject.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class ManagerProductCloudinaryTests {
    @org.mockito.Spy CategoryService categoryService = new CategoryService(mock(CategoryRepository.class), mock(jakarta.persistence.EntityManager.class));
    @Mock ProductRepository products;
    @Mock StoreInventoryRepository stocks;
    @Mock InventoryService inventory;
    @Mock CategoryRepository categories;
    @Mock UserRepository users;
    @Mock StoreAccessService access;
    @Mock StoreRepository stores;
    @Mock CloudinaryService cloud;
    @Mock ProductImageRepository images;
    @InjectMocks ManagerProductController controller;

    @Test void creatingWithUploadedPhotoSavesCloudUrlPublicIdAndBranchStock() throws Exception {
        Store store = Store.builder().id(1L).build(); Category category = Category.builder().id(2L).build();
        when(users.findByUsername("manager")).thenReturn(Optional.of(User.builder().build()));
        when(access.assignedStore()).thenReturn(store); when(categories.findById(2L)).thenReturn(Optional.of(category));
        when(products.save(any())).thenAnswer(call -> { Product p = call.getArgument(0); p.setId(3L); return p; });
        when(images.findByProductId(3L)).thenReturn(List.of());
        var file = new MockMultipartFile("imageFile", "flower.png", "image/png", new byte[]{1, 2});
        when(cloud.upload(file, "rental_shop_products")).thenReturn(new CloudinaryService.UploadResult("https://res.cloudinary.com/project/flower.png", "rental_shop_products/flower"));
        controller.addProduct(() -> "manager", "Hoa", 2L, new BigDecimal("100000"), null, 3,
                "https://old.example/flower.png", file, "Mô tả", null, false, new RedirectAttributesModelMap());
        verify(products).save(argThat(p -> p.getImageUrl().startsWith("https://res.cloudinary.com/")));
        verify(images).save(argThat(p -> p.getCloudinaryPublicId().equals("rental_shop_products/flower") && p.getIsMain()));
        verify(inventory).setStock(3L, 3);
    }

    @Test void updateUsesNewPhotoAsMainAndUploadFailureDoesNotChangeProduct() throws Exception {
        Category category = Category.builder().id(2L).build();
        Product product = Product.builder().id(3L).name("Tên cũ").category(category).imageUrl("https://old.example/flower.png").build();
        when(products.findById(3L)).thenReturn(Optional.of(product)); when(categories.findById(2L)).thenReturn(Optional.of(category));
        var file = new MockMultipartFile("imageFile", "flower.png", "image/png", new byte[]{1, 2});
        when(cloud.upload(file, "rental_shop_products")).thenThrow(new java.io.IOException("Upload unavailable"));
        assertThatThrownBy(() -> controller.updateProduct(3L, "Tên mới", 2L, new BigDecimal("100000"), null, null, file, "", null, false, new RedirectAttributesModelMap()))
                .isInstanceOf(IllegalStateException.class);
        assertThat(product.getName()).isEqualTo("Tên cũ"); verify(products, never()).save(any());
        ProductImage old = ProductImage.builder().isMain(true).build(); when(images.findByProductId(3L)).thenReturn(List.of(old));
        doReturn(new CloudinaryService.UploadResult("https://res.cloudinary.com/project/new.png", "products/new"))
                .when(cloud).upload(file, "rental_shop_products");
        controller.updateProduct(3L, "Tên mới", 2L, new BigDecimal("100000"), null, null, file, "", null, false, new RedirectAttributesModelMap());
        assertThat(old.getIsMain()).isFalse(); assertThat(product.getImageUrl()).endsWith("new.png");
        verify(images).save(argThat(p -> p.getCloudinaryPublicId().equals("products/new") && p.getIsMain()));
    }
}
