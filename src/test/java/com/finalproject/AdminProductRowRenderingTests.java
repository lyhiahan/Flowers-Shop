package com.finalproject;

import com.finalproject.entity.Product;
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

import static org.junit.jupiter.api.Assertions.*;

class AdminProductRowRenderingTests {
    private String renderRow(Map<Long, Long> openOrders, boolean deleted) throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/admin/products-manage.html"));
        int start = template.indexOf("<tr th:each=\"p : ${products}\"");
        assertTrue(start >= 0);
        String row = template.substring(start, template.indexOf("</tr>", start) + 5);

        var servletContext = new MockServletContext();
        var request = new MockHttpServletRequest(servletContext);
        var response = new MockHttpServletResponse();
        var exchange = JakartaServletWebApplication.buildApplication(servletContext).buildExchange(request, response);
        var context = new WebContext(exchange, Locale.forLanguageTag("vi-VN"));
        Product product = Product.builder().id(32L).name("Bó Hoa Cúc Tana")
                .priceBuy(new BigDecimal("120000")).importPrice(new BigDecimal("80000"))
                .sold(2).isActive(true).isSelling(true).build();
        context.setVariables(Map.of("products", List.of(product), "stockMap", Map.of(32L, 8),
                "inventorySummaryMap", Map.of(), "currentTab", deleted ? "DELETED" : "ALL",
                "currentPage", 0, "pageSize", 10,
                "_csrf", Map.of("parameterName", "_csrf", "token", "test-token")));
        if (openOrders != null) context.setVariable("openOrderCounts", openOrders);
        var engine = new SpringTemplateEngine();
        engine.setTemplateResolver(new StringTemplateResolver());
        return engine.process("<table><tbody>" + row + "</tbody></table>", context);
    }

    @Test
    void missingOrderCountsDoesNotTruncatePricesStockOrActions() throws Exception {
        String html = renderRow(null, false);
        assertEquals(8, java.util.regex.Pattern.compile("<td(?:\\s|>)").matcher(html).results().count());
        assertTrue(html.contains("120,000"));
        assertTrue(html.contains("8 bó"));
        assertTrue(html.contains("Chi tiết"));
        assertTrue(html.contains("/admin/products-manage/edit/32"));
        assertTrue(html.contains("Ngừng bán"));
        assertTrue(html.contains("Cấm kinh doanh"));
        assertTrue(html.contains("data-open-orders=\"-1\""));
        assertFalse(html.contains("disabled=\"disabled\""));
    }

    @Test
    void openOrdersArePassedToDialogWithoutInlineWarningOrDisabledButton() throws Exception {
        String html = renderRow(Map.of(32L, 2L), false);
        assertFalse(html.contains("đơn đang xử lý, chưa thể xóa"));
        assertTrue(html.contains("data-open-orders=\"2\""));
        assertFalse(html.contains("disabled=\"disabled\""));
        assertTrue(html.contains("/admin/products-manage/edit/32"));
    }

    @Test
    void noOpenOrdersAllowDeletionAndDeletedTabOffersRestore() throws Exception {
        String active = renderRow(Map.of(), false);
        assertFalse(active.contains("disabled=\"disabled\""));
        assertTrue(active.contains("Xóa sản phẩm"));
        String deleted = renderRow(null, true);
        assertTrue(deleted.contains("/admin/products-manage/restore/32"));
        assertFalse(deleted.contains("/admin/products-manage/edit/32"));
    }
}
