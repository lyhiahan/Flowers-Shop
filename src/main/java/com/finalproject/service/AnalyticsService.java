package com.finalproject.service;

import com.finalproject.dto.response.ChartSeriesDto;
import com.finalproject.dto.response.RevenueChartDto;

public interface AnalyticsService {
    RevenueChartDto getStoreRevenueStats(Long storeId);

    // Thống kê doanh thu theo giờ, ngày, tháng, năm (Toàn sàn nếu storeId null, hoặc theo shop)
    ChartSeriesDto getRevenueChart(String timeframe, Long storeId);

    // Thống kê người dùng mới theo giờ, ngày, tháng, năm
    ChartSeriesDto getNewUsersChart(String timeframe);

    // Thống kê cửa hàng mới theo giờ, ngày, tháng, năm
    ChartSeriesDto getNewStoresChart(String timeframe);
}