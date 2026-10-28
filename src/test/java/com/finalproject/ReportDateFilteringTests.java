package com.finalproject;

import com.finalproject.dto.response.ChartSeriesDto;
import com.finalproject.entity.*;
import com.finalproject.enums.*;
import com.finalproject.repository.*;
import com.finalproject.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReportDateFilteringTests {
    @Autowired MockMvc mvc;
    @Autowired AnalyticsService analytics;
    @Autowired Clock clock;
    @Autowired UserRepository users;
    @Autowired StoreRepository stores;
    @Autowired ProductRepository products;
    @Autowired CategoryRepository categories;
    @Autowired OrderRepository orders;
    @Autowired OrderDetailRepository details;
    @Autowired StoreMembershipRepository memberships;
    private LocalDate today;
    private Store own, foreign;
    private User manager;
    private Product product;

    @BeforeEach void fixtures() {
        today = LocalDate.now(clock);
        user("range_admin", Role.ADMIN, today.minusDays(40).atStartOfDay());
        manager = user("range_manager", Role.MANAGER, today.minusDays(40).atStartOfDay());
        own = stores.save(Store.builder().name("Range own").owner(manager).address("Own branch")
                .createdAt(today.atStartOfDay()).status(true).build());
        foreign = stores.save(Store.builder().name("Range foreign").address("Foreign branch")
                .createdAt(today.minusDays(7).atStartOfDay()).status(true).build());
        manager.setStore(own); users.save(manager);
        for (int age : new int[]{0, 6, 7, 29, 30, -1})
            user("range_user_" + age, Role.USER, today.minusDays(age).atTime(23, 59, 59));
        product = product(own, "Range bouquet", today.atStartOfDay());
        product(own, "Range older product", today.minusDays(7).atStartOfDay());
        var otherProduct = product(foreign, "Foreign bouquet", today.atStartOfDay());
        for (int[] row : new int[][]{{0, 100}, {6, 200}, {7, 300}, {29, 400}, {30, 500}, {-1, 600}})
            order(own, product, today.minusDays(row[0]).atTime(23, 59, 59, 999999000), row[1], true);
        order(own, product, today.atStartOfDay(), 700, false);
        order(foreign, otherProduct, today.atStartOfDay(), 900, true);
    }

    private User user(String name, Role role, LocalDateTime date) {
        return users.save(User.builder().username(name).email(name + "@range.test").password("unused")
                .fullName(name).role(role).isActive(true).createdAt(date).createdDate(date.toLocalDate())
                .creationTimeKnown(true).build());
    }
    private Product product(Store store, String name, LocalDateTime date) {
        return products.save(Product.builder().name(name).store(store)
                .category(categories.save(Category.builder().name(name + " category").build())).priceBuy(BigDecimal.valueOf(50))
                .importPrice(BigDecimal.TEN)
                .isActive(true).isDeleted(false).createdAt(date).build());
    }
    private void order(Store store, Product product, LocalDateTime date, int amount, boolean paid) {
        var order = orders.save(Order.builder().user(manager).pickupStore(store).orderType(OrderType.BUY)
                .status(paid ? OrderStatus.COMPLETED : OrderStatus.PENDING).paymentStatus(paid ? "PAID" : "UNPAID")
                .totalAmount(BigDecimal.valueOf(amount)).createdAt(date).build());
        var detail = details.save(OrderDetail.builder().order(order).product(product).quantity(2)
                .unitPrice(BigDecimal.valueOf(amount / 2)).orderType(OrderType.BUY).build());
        order.setOrderDetails(new ArrayList<>(List.of(detail)));
    }
    private ReportRange range(String period) { return ReportRange.resolve(period, null, null, clock); }
    private void amount(long expected, BigDecimal actual) { assertEquals(0, BigDecimal.valueOf(expected).compareTo(actual)); }

    @ParameterizedTest
    @CsvSource({"today,100,2,1,1", "7days,300,3,2,1", "30days,1000,5,4,2"})
    void presetsScopeEveryMetricToDatesAndBranch(String period, long revenue, long orderCount, long userCount, long storeCount) {
        var range = range(period);
        var dashboard = analytics.dashboard(own.getId(), range, "revenue");
        amount(revenue, (BigDecimal) dashboard.get("flowerRevenue"));
        assertEquals(orderCount, dashboard.get("totalOrdersCount"));
        assertEquals(orderCount, ((List<?>) dashboard.get("recentOrders")).size());
        amount(revenue, analytics.getRevenueChart(range, own.getId()).getTotal());
        amount(revenue, analytics.getProductRevenueChart(own.getId(), range).getTotal());
        amount(userCount, analytics.getNewUsersChart(range).getTotal());
        amount(storeCount, analytics.getNewStoresChart(range).getTotal());
        assertEquals(period.equals("30days") ? 2L : 1L, dashboard.get("totalProductsCount"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void customSingleDayIncludesLateEveningButExcludesNextMidnightAndPreviousDays() {
        var day = today.minusDays(6);
        var range = ReportRange.resolve("custom", day, day, clock);
        order(own, product, day.atStartOfDay(), 50, true);
        order(own, product, day.plusDays(1).atStartOfDay(), 1000, true);
        amount(250, analytics.getRevenueChart(range, own.getId()).getTotal());
        amount(250, analytics.getProductRevenueChart(own.getId(), range).getTotal());
        var dashboard = analytics.dashboard(own.getId(), range, "products");
        assertEquals(2L, dashboard.get("totalOrdersCount"));
        amount(4, (BigDecimal) ((List<?>) dashboard.get("chartValues")).getFirst());
        amount(1, analytics.getNewUsersChart(range).getTotal());
    }

    @Test void longRangeOnlyCountsPartialEdgeMonthsAndFillsEmptyMonths() {
        var start = today.minusDays(130);
        var end = today.minusDays(20);
        order(own, product, start.atStartOfDay(), 100, true);
        order(own, product, start.minusDays(1).atStartOfDay(), 9000, true);
        order(own, product, end.atTime(23, 59, 59), 200, true);
        order(own, product, end.plusDays(1).atStartOfDay(), 9000, true);
        var range = ReportRange.resolve("custom", start, end, clock);
        var chart = analytics.getRevenueChart(range, own.getId());
        amount(1200, chart.getTotal()); // 100 + 200 + fixture orders at -29 and -30 days
        assertEquals(range.keys().size(), chart.getLabels().size());
        assertTrue(chart.getData().stream().anyMatch(value -> value.signum() == 0));
    }

    @Test void todayIncludesHistoricalDateOnlyRegistrationsButLegacyHoursDoNotInventTimes() {
        users.save(User.builder().username("range_legacy").email("range_legacy@range.test").password("unused")
                .fullName("Legacy").role(Role.USER).createdDate(today).build());
        amount(2, analytics.getNewUsersChart(range("today")).getTotal());
        amount(1, analytics.getNewUsersChart(range("hour")).getTotal());
    }

    @Test @WithMockUser(username="range_admin", roles="ADMIN")
    void adminFiltersAllCardsChartsRanksAndPreservesCustomFormState() throws Exception {
        var result = mvc.perform(get("/admin/dashboard").param("timeframe", "custom")
                .param("startDate", today.minusDays(6).toString()).param("endDate", today.toString())
                .param("criteria", "orders").param("chartType", "pie").param("userChartType", "bar")
                .param("storeChartType", "line"))
                .andExpect(status().isOk()).andExpect(model().attribute("dataLoadError", false))
                .andExpect(model().attribute("totalUsersCount", 2L))
                .andExpect(model().attribute("totalStoresCount", 1L))
                .andExpect(model().attribute("totalProductsCount", 2L))
                .andExpect(model().attribute("totalOrdersCount", 4L))
                .andExpect(content().string(containsString("value=\"30days\"")))
                .andExpect(content().string(containsString("value=\"" + today.minusDays(6) + "\"")))
                .andExpect(content().string(containsString("name=\"userChartType\" value=\"bar\"")))
                .andReturn();
        var model = result.getModelAndView().getModel();
        amount(1200, (BigDecimal) model.get("flowerRevenue"));
        amount(2, ((ChartSeriesDto) model.get("usersChart")).getTotal());
        amount(1, ((ChartSeriesDto) model.get("storesChart")).getTotal());
        assertEquals(2, ((List<?>) model.get("topPointUsers")).size());
        assertEquals(1, ((List<?>) model.get("topPointStores")).size());
        assertEquals(4, ((List<?>) model.get("recentOrders")).size());
    }

    @Test @WithMockUser(username="range_manager", roles="MANAGER")
    void managerDashboardAndBothAnalyticsModesShareCustomRange() throws Exception {
        var start = today.minusDays(6).toString();
        mvc.perform(get("/manager/dashboard").param("timeRange", "custom").param("startDate", start)
                .param("endDate", today.toString()).param("chartType", "bar"))
                .andExpect(status().isOk()).andExpect(model().attribute("totalOrdersCount", 3L))
                .andExpect(model().attribute("totalProductsCount", 1L))
                .andExpect(content().string(containsString("id=\"filterTime30Days\"")));
        for (String type : List.of("order", "product")) {
            var result = mvc.perform(get("/manager/analytics").param("type", type).param("timeframe", "custom")
                    .param("startDate", start).param("endDate", today.toString()))
                    .andExpect(status().isOk()).andExpect(content().string(containsString("id=\"analyticsTimeCustom\"")))
                    .andExpect(content().string(containsString("startDate=" + start)))
                    .andExpect(content().string(containsString("endDate=" + today)))
                    .andReturn();
            amount(300, ((ChartSeriesDto) result.getModelAndView().getModel().get("revenueChart")).getTotal());
        }
    }

    @Test @WithMockUser(username="range_admin", roles="ADMIN")
    void invalidDateInputsReturnBadRequest() throws Exception {
        mvc.perform(get("/admin/dashboard").param("timeframe", "custom")).andExpect(status().isBadRequest());
        mvc.perform(get("/admin/dashboard").param("timeframe", "custom").param("startDate", today.toString())
                .param("endDate", today.minusDays(1).toString())).andExpect(status().isBadRequest());
        mvc.perform(get("/admin/dashboard").param("startDate", "invalid")).andExpect(status().isBadRequest());
        mvc.perform(get("/admin/dashboard").param("timeframe", "unsupported")).andExpect(status().isBadRequest());
    }

    @Test @WithMockUser(username="range_manager", roles="MANAGER")
    void selectingDatesCannotAccessAnotherManagersBranch() throws Exception {
        mvc.perform(get("/manager/dashboard").param("storeId", foreign.getId().toString()).param("timeRange", "30days"))
                .andExpect(status().isForbidden());
    }

    @Test @WithMockUser(username="range_employee", roles="MANAGER")
    void branchEmployeeCannotUseFinancialReportsReservedForManagers() throws Exception {
        var employee = user("range_employee", Role.MANAGER, today.atStartOfDay());
        employee.setStore(own); users.save(employee);
        memberships.save(StoreMembership.builder().user(employee).store(own).accessRole("STAFF").active(true).build());
        mvc.perform(get("/manager/dashboard").param("timeRange", "today")).andExpect(status().isForbidden());
        mvc.perform(get("/manager/analytics").param("timeframe", "today")).andExpect(status().isForbidden());
    }

    @Test @WithMockUser(username="range_admin", roles="ADMIN")
    void rankingPaginationKeepsDateFilterAndChartSelections() throws Exception {
        for (int i = 0; i < 21; i++) user("range_pager_" + i, Role.USER, today.atStartOfDay());
        mvc.perform(get("/admin/dashboard?timeframe=custom&startDate=" + today + "&endDate=" + today + "&userChartType=pie"))
                .andExpect(status().isOk()).andExpect(model().attribute("totalUsersCount", 22L))
                .andExpect(content().string(containsString("startDate=" + today + "&amp;endDate=" + today)))
                .andExpect(content().string(containsString("userChartType=pie&amp;topPointUsersPage=1")));
    }
}
