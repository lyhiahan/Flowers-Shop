package com.finalproject.service;

import com.finalproject.dto.response.RevenueChartDto;

public interface AnalyticsService {
    RevenueChartDto getStoreRevenueStats(Long storeId);
}