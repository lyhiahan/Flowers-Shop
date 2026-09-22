package com.finalproject.utils;

import java.security.SecureRandom;

public class OtpUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    private OtpUtil() {
        // Private constructor để chống khởi tạo đối tượng tiện ích
    }

    public static String generateOtp() {
        int otp = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(otp);
    }
}