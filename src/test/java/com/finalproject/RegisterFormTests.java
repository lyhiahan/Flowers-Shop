package com.finalproject;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegisterFormTests {
    @Autowired MockMvc mvc;

    private String input(String html, String id) {
        var matcher = Pattern.compile("<input\\b(?=[^>]*id=\"" + id + "\")[^>]*>").matcher(html);
        assertThat(matcher.find()).isTrue();
        return matcher.group();
    }

    @Test void openingRegistrationStartsWithEmptyCredentials() throws Exception {
        String html = mvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("freshRegistration", true))
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("data-fresh-registration=\"true\"", "/js/register-autofill.js");
        assertThat(input(html, "usernameInput")).contains("value=\"\"");
        assertThat(input(html, "passwordInput")).doesNotContain("value=");
    }

    @Test void validationFailurePreservesUsernameButNeverRendersPassword() throws Exception {
        String password = "not-rendered-secret-4917";
        String html = mvc.perform(post("/register").with(csrf())
                        .param("username", "newcustomer4917")
                        .param("password", password)
                        .param("fullName", "")
                        .param("email", "invalid-email")
                        .param("phone", ""))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("data-fresh-registration=\"false\"");
        assertThat(input(html, "usernameInput")).contains("value=\"newcustomer4917\"");
        assertThat(input(html, "passwordInput")).doesNotContain("value=");
        assertThat(html).doesNotContain(password);
    }
}
