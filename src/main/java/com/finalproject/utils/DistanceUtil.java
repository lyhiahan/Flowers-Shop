package com.finalproject.utils;

public class DistanceUtil {

    private static final double EARTH_RADIUS = 6371; // Bán kính Trái Đất (km)

    private DistanceUtil() {}

    // Tính khoảng cách giữa 2 tọa độ (latitude, longitude) theo km
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round((EARTH_RADIUS * c) * 10.0) / 10.0; // Làm tròn 1 chữ số thập phân
    }
}