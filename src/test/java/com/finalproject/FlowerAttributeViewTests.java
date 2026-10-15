package com.finalproject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class FlowerAttributeViewTests {
    private String render(Map<String, List<String>> suggestions, String types, String colors, String designs) {
        var servletContext = new MockServletContext();
        var request = new MockHttpServletRequest(servletContext);
        var response = new MockHttpServletResponse();
        var application = JakartaServletWebApplication.buildApplication(servletContext);
        var context = new WebContext(application.buildExchange(request, response), Locale.forLanguageTag("vi"));
        if (suggestions != null) context.setVariable("flowerInputSuggestions", suggestions);
        context.setVariable("types", types);
        context.setVariable("colors", colors);
        context.setVariable("designs", designs);
        var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        var engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine.process("fragments/flower-attributes", context);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    void alwaysRendersThreeEditableFieldsRegardlessOfSuggestions(int state) {
        Map<String, List<String>> suggestions = switch (state) {
            case 0 -> null;
            case 1 -> Map.of();
            case 2 -> Map.of("Màu sắc", List.of("Hồng đào"));
            default -> Map.of("Loại hoa", List.of("Baby"), "Màu sắc", List.of("Hồng đào"),
                    "Phong cách", List.of("Tự nhiên"));
        };
        String html = render(suggestions, "", "", "");
        assertThat(Pattern.compile("<textarea\\b").matcher(html).results().count()).isEqualTo(3);
        for (String field : List.of("flowerTypes", "flowerColors", "flowerStyles")) {
            assertThat(html).contains("name=\"" + field + "\"", "id=\"" + field + "\"", "for=\"" + field + "\"");
        }
        assertThat(html).doesNotContain("Tự nhập hoặc chọn gợi ý.", "một bó hoa có thể", "Tối đa 15 giá trị");
        if (state >= 2) assertThat(html).contains("value=\"Hồng đào\"");
    }

    @Test
    void missingSuggestionsStillRestoresSavedValuesAndEscapesHtml() {
        String html = render(null, "Hoa hồng, Baby", "Trắng kem", "<script>alert(1)</script>");
        assertThat(html).contains("Hoa hồng, Baby", "Trắng kem", "&lt;script&gt;alert(1)&lt;/script&gt;")
                .doesNotContain("<script>alert(1)</script>");
    }

    @Test
    void rendersOtherAttributeGroupsAndChecksExistingValues() {
        var servletContext = new MockServletContext();
        var request = new MockHttpServletRequest(servletContext);
        var response = new MockHttpServletResponse();
        var application = JakartaServletWebApplication.buildApplication(servletContext);
        var context = new WebContext(application.buildExchange(request, response), Locale.forLanguageTag("vi"));

        var style = com.finalproject.entity.Style.builder().id(3L).name("Kiểu dáng").build();
        var option1 = com.finalproject.entity.StyleValue.builder().id(8L).name("Bó hoa").style(style).build();
        var option2 = com.finalproject.entity.StyleValue.builder().id(9L).name("Giỏ hoa").style(style).build();
        context.setVariable("otherProductAttributeGroups", Map.of("Kiểu dáng", List.of(option1, option2)));
        context.setVariable("types", "Hoa hồng");
        context.setVariable("colors", "Đỏ");
        context.setVariable("designs", "Cổ điển");
        context.setVariable("selectedIds", List.of(8L));

        var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        var engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        String html = engine.process("fragments/flower-attributes", context);

        assertThat(html).contains("Bó hoa", "Giỏ hoa", "attributesSubmitted");
        assertThat(html).contains("value=\"8\" checked");
        assertThat(html).doesNotContain("value=\"9\" checked");
    }
}
