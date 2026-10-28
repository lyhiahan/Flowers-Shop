package com.finalproject;

import com.finalproject.entity.User;
import com.finalproject.enums.Role;
import com.finalproject.repository.UserRepository;
import com.finalproject.service.CloudinaryService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:profile-images;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc @ActiveProfiles("test")
class ProfileImageUpdateTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @MockitoBean CloudinaryService cloud;
    User customer;

    @BeforeEach void setup() {
        String name="image-"+UUID.randomUUID().toString().substring(0,8);
        customer=users.save(User.builder().username(name).email(name+"@test.local").fullName("Image Customer")
                .password("unused").role(Role.USER).isActive(true).avatarUrl("/images/flower-logo.png")
                .avatarPublicId("avatars/old").cover("/images/florist-artisan-banner.jpg").coverPublicId("covers/old").build());
    }
    @AfterEach void cleanup() {users.deleteById(customer.getId());}

    @Test void bothImagesSaveWithBlankOptionalContactFieldsAndRemainAfterReload() throws Exception {
        preview("profile-before.html", profile());
        var avatar=image("avatarFile"); var cover=image("coverFile");
        when(cloud.upload(any(),eq("flower_shop_avatars"))).thenReturn(new CloudinaryService.UploadResult("https://res.cloudinary.com/test/avatar.png","avatars/new"));
        when(cloud.upload(any(),eq("flower_shop_covers"))).thenReturn(new CloudinaryService.UploadResult("https://res.cloudinary.com/test/cover.png","covers/new"));
        mvc.perform(multipart("/customer/profile/update").file(avatar).file(cover).with(user(customer.getUsername()).roles("USER"))
                        .with(csrf()).param("fullName",customer.getFullName()).param("phone","").param("address",""))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("successMessage"));
        User saved=users.findById(customer.getId()).orElseThrow();
        assertThat(saved.getAvatarUrl()).isEqualTo("https://res.cloudinary.com/test/avatar.png");
        assertThat(saved.getCover()).isEqualTo("https://res.cloudinary.com/test/cover.png");
        assertThat(saved.getPhone()).isNull(); assertThat(saved.getAddress()).isNull();
        verify(cloud).delete("avatars/old"); verify(cloud).delete("covers/old");
        String html=profile();
        assertThat(html).contains(saved.getAvatarUrl(),saved.getCover(),"data-profile-image-trigger=\"avatarFileInput\"", "data-profile-image-trigger=\"coverFileInput\"");
        preview("profile-after.html",html);
    }

    @Test void secondUploadFailurePreservesBothOldImagesAndRemovesTheUncommittedUpload() throws Exception {
        when(cloud.upload(any(),eq("flower_shop_avatars"))).thenReturn(new CloudinaryService.UploadResult("https://res.cloudinary.com/test/new.png","avatars/new"));
        when(cloud.upload(any(),eq("flower_shop_covers"))).thenThrow(new IOException("Upload unavailable"));
        mvc.perform(multipart("/customer/profile/update").file(image("avatarFile")).file(image("coverFile"))
                        .with(user(customer.getUsername()).roles("USER")).with(csrf()).param("fullName","Changed name"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("errorMessage"));
        User saved=users.findById(customer.getId()).orElseThrow();
        assertThat(saved.getFullName()).isEqualTo(customer.getFullName());
        assertThat(saved.getAvatarUrl()).isEqualTo(customer.getAvatarUrl());
        assertThat(saved.getCover()).isEqualTo(customer.getCover());
        verify(cloud).delete("avatars/new");
        verify(cloud,never()).delete("avatars/old"); verify(cloud,never()).delete("covers/old");
    }

    @Test void avatarOnlyPreservesExistingCoverAndContactDetails() throws Exception {
        customer.setPhone("0901234567"); customer.setAddress("Existing address"); users.save(customer);
        when(cloud.upload(any(),eq("flower_shop_avatars"))).thenReturn(new CloudinaryService.UploadResult("https://res.cloudinary.com/test/new.png","avatars/new"));
        mvc.perform(multipart("/customer/profile/update").file(image("avatarFile")).with(user(customer.getUsername()).roles("USER"))
                        .with(csrf()).param("fullName",customer.getFullName()))
                .andExpect(flash().attributeExists("successMessage"));
        User saved=users.findById(customer.getId()).orElseThrow();
        assertThat(saved.getCover()).isEqualTo(customer.getCover());
        assertThat(saved.getPhone()).isEqualTo(customer.getPhone());
        assertThat(saved.getAddress()).isEqualTo(customer.getAddress());
        verify(cloud,never()).upload(any(),eq("flower_shop_covers"));
    }

    @Test void rejectedUploadsNeverReportSuccessOrOverwriteTheStoredImages() throws Exception {
        when(cloud.upload(any(),anyString())).thenThrow(new IllegalArgumentException("Invalid image"));
        mvc.perform(multipart("/customer/profile/update").file(image("avatarFile")).with(user(customer.getUsername()).roles("USER"))
                        .with(csrf()).param("fullName",customer.getFullName()))
                .andExpect(flash().attributeExists("errorMessage")).andExpect(flash().attributeCount(1));
        assertThat(users.findById(customer.getId()).orElseThrow().getAvatarUrl()).isEqualTo(customer.getAvatarUrl());
        verify(cloud,never()).delete(any());
    }

    @Test void imageUploadRequiresAnAuthenticatedOwnerAndCsrf() throws Exception {
        mvc.perform(multipart("/customer/profile/update").file(image("avatarFile")).with(csrf()).param("fullName","Other"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(multipart("/customer/profile/update").file(image("avatarFile"))
                        .with(user(customer.getUsername()).roles("USER")).param("fullName","Other"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(cloud);
    }

    private MockMultipartFile image(String field) throws IOException {
        return new MockMultipartFile(field,"photo.png","image/png",Files.readAllBytes(Path.of("src/main/resources/static/images/flower-logo.png")));
    }
    private String profile() throws Exception {
        return mvc.perform(get("/customer/profile").param("tab","info").with(user(customer.getUsername()).roles("USER")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
    private void preview(String name,String html) throws IOException {
        Path root=Path.of("target/profile-preview"); Files.createDirectories(root); Files.writeString(root.resolve(name),html);
    }
}
