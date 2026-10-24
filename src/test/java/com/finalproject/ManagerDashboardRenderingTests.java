package com.finalproject;

import com.finalproject.entity.Store;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ManagerDashboardRenderingTests {
    private String render(Boolean multipleStores) throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/manager/dashboard.html"));
        int start = template.indexOf("<header class=\"d-flex justify-content-between");
        String section = template.substring(start, template.indexOf("<form action=\"/manager/dashboard\"", start));
        var servlet = new MockServletContext();
        var exchange = JakartaServletWebApplication.buildApplication(servlet).buildExchange(
                new MockHttpServletRequest(servlet), new MockHttpServletResponse());
        var context = new WebContext(exchange, Locale.forLanguageTag("vi"));
        Store store = Store.builder().id(1L).name("Chi nhánh Bến Thành").build();
        context.setVariables(Map.of("store", store, "accessibleStores", List.of(store, Store.builder().id(2L).name("Chi nhánh Q7").build()),
                "currentStockQuantity", 25, "totalProductsCount", 3, "totalOrdersCount", 2,
                "flowerRevenue", new BigDecimal("350000")));
        if (multipleStores != null) context.setVariable("canManageMultipleStores", multipleStores);
        var engine = new SpringTemplateEngine();
        engine.setTemplateResolver(new StringTemplateResolver());
        return engine.process(section, context);
    }

    @Test void missingPermissionFlagStillRendersAllKpis() throws Exception {
        assertThat(render(null)).contains("id=\"currentStockQuantity\"", "id=\"totalProductsCount\"",
                "id=\"totalOrdersCount\"", "350,000", "Cửa hàng của tôi").doesNotContain("id=\"dashboardStoreSelect\"");
    }

    @Test void managerCannotSeeStoreSelectorAndAdminCan() throws Exception {
        assertThat(render(false)).contains("id=\"currentStockQuantity\"").doesNotContain("id=\"dashboardStoreSelect\"");
        assertThat(render(true)).contains("id=\"dashboardStoreSelect\"", "id=\"currentStockQuantity\"");
    }
}
