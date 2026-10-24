package com.finalproject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerNavigationRenderingTests {
    private WebContext context() {
        var servlet = new MockServletContext();
        var exchange = JakartaServletWebApplication.buildApplication(servlet).buildExchange(
                new MockHttpServletRequest(servlet), new MockHttpServletResponse());
        return new WebContext(exchange, Locale.forLanguageTag("vi"));
    }

    private String ordersHeading(String status) throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/customer/orders.html"));
        int start = template.indexOf("<section class=\"customer-orders-heading\"");
        String heading = template.substring(start, template.indexOf("</section>", start) + "</section>".length());
        var context = context();
        context.setVariable("currentStatus", status);
        context.setVariable("totalOrders", 21);
        var engine = new SpringTemplateEngine();
        engine.setTemplateResolver(new StringTemplateResolver());
        return engine.process(heading, context);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ALL", "PENDING", "CONFIRMED", "PREPARING", "DELIVERING", "COMPLETED",
            "CANCELLED", "REFUND_REQUESTED", "REFUNDED"})
    void statusMenuRetainsEveryFilterAndSelectsTheActiveOne(String status) throws Exception {
        String html = ordersHeading(status);
        assertThat(html).contains("method=\"get\"", "action=\"/customer/orders\"", "name=\"status\"",
                "for=\"orderStatusFilter\"", "21 đơn hàng", "<noscript>");
        assertThat(html).containsPattern("<option value=\"" + status + "\" selected=\"selected\">");
        assertThat(html.split("<option ")).hasSize(10);
        assertThat(html.split("selected=\"selected\"")).hasSize(2);
        assertThat(html).doesNotContain("floral-hero-card", "status-filter-pill", "Theo dõi tiến trình");
    }

    @Test
    void defaultViewSelectsAllOrders() throws Exception {
        assertThat(ordersHeading(null)).contains("<option value=\"ALL\" selected=\"selected\">");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 4})
    void orderPaginationStaysFlatAndPreservesItsFilter(int page) throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/customer/orders.html"));
        int start = template.indexOf("<nav th:if=\"${totalPages > 1}\"");
        String pagination = template.substring(start, template.indexOf("</nav>", start) + "</nav>".length());
        var context = context();
        context.setVariable("currentStatus", "CANCELLED");
        context.setVariable("currentPage", page);
        context.setVariable("totalPages", 5);
        context.setVariable("hasPrevious", page > 0);
        context.setVariable("hasNext", page < 4);
        var engine = new SpringTemplateEngine();
        engine.setTemplateResolver(new StringTemplateResolver());
        String html = engine.process(pagination, context);
        assertThat(html).contains("pagination pagination-rounded gap-1", "Trang trước", "Trang sau",
                "status=CANCELLED", "page=" + page, "page-item active");
        assertThat(html).doesNotContain("shadow");
    }

    @Test
    void sharedSearchFragmentRendersOnceAndEscapesItsKeyword() {
        var resolver = new FileTemplateResolver();
        resolver.setPrefix("src/main/resources/templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        var engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        var context = context();
        context.setVariable("keyword", "\"><script>alert(1)</script>");
        String html = engine.process("fragments/floral", Set.of("search"), context);
        assertThat(html).contains("role=\"search\"", "type=\"search\"", "/products#product-catalog",
                "value=\"products\"", "value=\"stores\"", "value=\"users\"", "/js/global-search.js",
                "aria-label=\"Phạm vi tìm kiếm\"", "&lt;script&gt;");
        assertThat(html.split("id=\"globalSearchForm\"")).hasSize(2);
        assertThat(html.split("header-search-type-icon")).hasSize(2);
        assertThat(html).doesNotContain("<script>alert(1)</script>", "style=\"width:", "expand_more");
    }
}
