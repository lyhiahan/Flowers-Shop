package com.finalproject;

import com.finalproject.entity.*;
import com.finalproject.enums.*;
import com.finalproject.repository.*;
import com.finalproject.service.CloudinaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ImageUploadRoutesTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired StoreRepository stores;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired ProductImageRepository images;
    @Autowired OrderRepository orders;
    @Autowired ReviewRepository reviews;
    @MockitoBean CloudinaryService cloudinary;
    Store store;
    Product product;
    Category category;
    User manager, customer;
    Order order;

    @BeforeEach void setup() throws Exception {
        store = stores.save(Store.builder().name("Upload shop").address("123 Le Loi")
                .phone("0912345678").status(true).isOpen(true).build());
        manager = users.save(User.builder().username("upload_manager").fullName("Manager")
                .email("upload.manager@test.local").password("unused").role(Role.MANAGER).store(store).isActive(true).build());
        store.setOwner(manager);
        stores.save(store);
        customer = users.save(User.builder().username("upload_customer").fullName("Customer")
                .email("upload.customer@test.local").password("unused").role(Role.CUSTOMER).isActive(true).build());
        category = categories.save(Category.builder().name("Upload flowers").build());
        product = products.save(Product.builder().name("Upload product").category(category).store(store)
                .priceBuy(BigDecimal.valueOf(100000)).importPrice(BigDecimal.valueOf(60000))
                .priceRentPerDay(BigDecimal.ZERO).depositFee(BigDecimal.ZERO).build());
        order = Order.builder().user(customer).pickupStore(store).orderType(OrderType.BUY)
                .status(OrderStatus.COMPLETED).build();
        order.setOrderDetails(List.of(OrderDetail.builder().order(order).product(product)
                .quantity(1).unitPrice(BigDecimal.valueOf(100000)).orderType(OrderType.BUY).build()));
        orders.saveAndFlush(order);
        when(cloudinary.upload(any(), anyString())).thenAnswer(call -> new CloudinaryService.UploadResult(
                "https://res.cloudinary.com/test/image/upload/" + call.getArgument(1) + "/photo.png",
                call.getArgument(1) + "/photo"));
    }

    MockMultipartFile photo(String name) {
        return new MockMultipartFile(name, "photo.png", "image/png", new byte[]{1, 2, 3});
    }

    @Test void managerCanCreateProductWithFile() throws Exception {
        mvc.perform(multipart("/manager/products/add").file(photo("imageFile"))
                .param("name", "New uploaded flower").param("categoryId", category.getId().toString())
                .param("priceBuy", "100000").param("initialStock", "3")
                .with(user(manager.getUsername()).roles("MANAGER")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        var created = products.findAll().stream().filter(p -> p.getName().equals("New uploaded flower")).findFirst().orElseThrow();
        assertThat(created.getImageUrl()).contains("rental_shop_products");
        assertThat(images.findByProductIdAndIsMainTrue(created.getId()).orElseThrow().getCloudinaryPublicId())
                .isEqualTo("rental_shop_products/photo");
    }

    @Test void vendorUpdatePrioritizesFileOverUrlAndUpdatesGallery() throws Exception {
        // Manager and Vendor share the same controller/routes and store access rules.
        mvc.perform(multipart("/vendor/products/update/" + product.getId()).file(photo("imageFile"))
                .param("name", product.getName()).param("categoryId", category.getId().toString())
                .param("priceBuy", "100000").param("imageUrl", "https://example.com/ignored.png")
                .with(user(manager.getUsername()).roles("VENDOR")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(products.findById(product.getId()).orElseThrow().getImageUrl()).contains("res.cloudinary.com");
        assertThat(images.findByProductIdAndIsMainTrue(product.getId()).orElseThrow().getImageUrl())
                .isEqualTo(product.getImageUrl());
    }

    @Test void anotherStoresProductCannotTriggerUpload() throws Exception {
        var other = stores.save(Store.builder().name("Other shop").address("Other address").build());
        product.setStore(other);
        products.save(product);
        mvc.perform(multipart("/manager/products/update/" + product.getId()).file(photo("imageFile"))
                .param("name", "Forbidden").param("categoryId", category.getId().toString()).param("priceBuy", "100000")
                .with(user(manager.getUsername()).roles("MANAGER")).with(csrf()))
                .andExpect(status().isForbidden());
        verify(cloudinary, never()).upload(any(), anyString());
    }

    @Test void productUrlOnlyRemainsSupportedWithoutCallingCloudinary() throws Exception {
        mvc.perform(post("/manager/products/update/" + product.getId())
                .param("name", product.getName()).param("categoryId", category.getId().toString())
                .param("priceBuy", "100000").param("imageUrl", "https://example.com/flower.png")
                .with(user(manager.getUsername()).roles("MANAGER")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(product.getImageUrl()).isEqualTo("https://example.com/flower.png");
        verify(cloudinary, never()).upload(any(), anyString());
    }

    @Test void reviewFromProductCanUploadAnImage() throws Exception {
        mvc.perform(multipart("/product/" + product.getId() + "/review").file(photo("imageFile"))
                .param("orderId", order.getId().toString()).param("rating", "5").param("content", "Hoa rat dep. ".repeat(8))
                .param("imageUrl", "https://example.com/ignored.png")
                .with(user(customer.getUsername()).roles("CUSTOMER")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        var saved = reviews.findByOrderId(order.getId()).get(0);
        assertThat(saved.getImageUrl()).contains("rental_shop_reviews");
        assertThat(saved.getImagePublicId()).isEqualTo("rental_shop_reviews/photo");
    }

    @Test void reviewFromOrderRouteCanUploadAnImage() throws Exception {
        mvc.perform(multipart("/products/" + product.getId() + "/review").file(photo("imageFile"))
                .param("orderId", order.getId().toString()).param("rating", "4").param("content", "Hoa rat dep. ".repeat(8))
                .with(user(customer.getUsername()).roles("CUSTOMER")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(reviews.findByOrderId(order.getId()).get(0).getImageUrl()).contains("res.cloudinary.com");
    }

    @Test void invalidReviewDoesNotUploadAnything() throws Exception {
        mvc.perform(multipart("/product/" + product.getId() + "/review").file(photo("imageFile"))
                .param("orderId", order.getId().toString()).param("rating", "5").param("content", "Too short")
                .with(user(customer.getUsername()).roles("CUSTOMER")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        verify(cloudinary, never()).upload(any(), anyString());
        assertThat(reviews.findByOrderId(order.getId())).isEmpty();
    }

    @Test void cloudFailureDoesNotSaveReviewWithMissingImage() throws Exception {
        when(cloudinary.upload(any(), anyString())).thenThrow(new java.io.IOException("Upload failed"));
        mvc.perform(multipart("/product/" + product.getId() + "/review").file(photo("imageFile"))
                .param("orderId", order.getId().toString()).param("rating", "5").param("content", "Hoa rat dep. ".repeat(8))
                .with(user(customer.getUsername()).roles("CUSTOMER")).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("errorMessage"));
        assertThat(reviews.findByOrderId(order.getId())).isEmpty();
    }

    @Test void storeCreateUploadsAvatarCoverAndFeaturedImages() throws Exception {
        mvc.perform(multipart("/manager/stores/create").file(photo("avatarFile")).file(photo("coverFile"))
                .file(photo("featuredFiles")).param("name", "New upload shop").param("phone", "0912345678")
                .param("address", "123 Le Loi, Quan 1").with(user(manager.getUsername()).roles("MANAGER")).with(csrf()))
                .andExpect(redirectedUrl("/manager/dashboard"));
        var created = stores.findByOwnerId(manager.getId()).stream().filter(s -> s.getName().equals("New upload shop")).findFirst().orElseThrow();
        assertThat(created.getAvatarPublicId()).isEqualTo("rental_shop_stores/avatars/photo");
        assertThat(created.getCoverPublicId()).isEqualTo("rental_shop_stores/covers/photo");
        assertThat(created.getFeaturedImages()).contains("rental_shop_stores/featured");
    }

    @Test void storeEditUploadsFilesAndRetainsFeaturedUrls() throws Exception {
        mvc.perform(multipart("/manager/stores/edit/" + store.getId()).file(photo("avatarFile"))
                .file(photo("coverFile")).file(photo("featuredFiles"))
                .param("name", store.getName()).param("phone", "0912345678").param("address", store.getAddress())
                .param("featuredImages", "https://example.com/existing.png")
                .with(user(manager.getUsername()).roles("MANAGER")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(store.getAvatar()).contains("rental_shop_stores/avatars");
        assertThat(store.getCover()).contains("rental_shop_stores/covers");
        assertThat(store.getFeaturedImages()).contains("existing.png", "rental_shop_stores/featured");
    }

    @Test void affectedFormsRenderFileInputsAndMultipartEncoding() throws Exception {
        mvc.perform(get("/manager/products").with(user(manager.getUsername()).roles("MANAGER")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"imageFile\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("enctype=\"multipart/form-data\"")));
        mvc.perform(get("/manager/stores/edit/" + store.getId()).with(user(manager.getUsername()).roles("MANAGER")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"featuredFiles\"")));
        mvc.perform(get("/customer/orders/detail/" + order.getId()).with(user(customer.getUsername()).roles("CUSTOMER")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"imageFile\"")));
        mvc.perform(get("/product/" + product.getId()).with(user(customer.getUsername()).roles("CUSTOMER")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"imageFile\"")));
    }
}
