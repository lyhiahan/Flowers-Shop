package com.finalproject.service;

import com.finalproject.dto.response.ChartSeriesDto;
import com.finalproject.dto.response.RevenueChartDto;

public interface AnalyticsService {
    java.util.Map<String, Object> dashboard(Long storeId, String timeframe, String criteria);
    java.util.Map<String, Object> dashboard(Long storeId, ReportRange range, String criteria);
    long currentStockQuantity(Long storeId);
    RevenueChartDto getStoreRevenueStats(Long storeId);

    // Thống kê doanh thu theo giờ, ngày, tháng, năm (Toàn sàn nếu storeId null, hoặc theo shop)
    ChartSeriesDto getRevenueChart(String timeframe, Long storeId);
    ChartSeriesDto getRevenueChart(ReportRange range, Long storeId);

    // Thống kê doanh thu theo từng sản phẩm của cửa hàng
    ChartSeriesDto getProductRevenueChart(Long storeId);
    ChartSeriesDto getProductRevenueChart(Long storeId, ReportRange range);

    // Thống kê người dùng mới theo giờ, ngày, tháng, năm
    ChartSeriesDto getNewUsersChart(String timeframe);
    ChartSeriesDto getNewUsersChart(ReportRange range);

    // Thống kê cửa hàng mới theo giờ, ngày, tháng, năm
    ChartSeriesDto getNewStoresChart(String timeframe);
    ChartSeriesDto getNewStoresChart(ReportRange range);
}
