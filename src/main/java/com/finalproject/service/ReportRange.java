package com.finalproject.service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Inclusive calendar dates; database queries use [start, endExclusive) in application time. */
public record ReportRange(String period, LocalDate startDate, LocalDate endDate, Bucket bucket) {
    public enum Bucket { HOUR, DAY, MONTH, YEAR }

    public ReportRange {
        if (startDate == null || endDate == null)
            throw new IllegalArgumentException("Vui lòng chọn đầy đủ từ ngày và đến ngày.");
        if (startDate.isAfter(endDate))
            throw new IllegalArgumentException("Từ ngày không được sau đến ngày.");
        if (startDate.getYear() < 1 || endDate.getYear() > 9998)
            throw new IllegalArgumentException("Khoảng ngày nằm ngoài phạm vi dữ liệu được hỗ trợ.");
    }

    public static ReportRange resolve(String requested, LocalDate from, LocalDate to, Clock clock) {
        String period = requested == null || requested.isBlank()
                ? (from != null || to != null ? "custom" : "7days") : requested.toLowerCase(Locale.ROOT);
        LocalDate today = LocalDate.now(clock);
        return switch (period) {
            case "today" -> new ReportRange("today", today, today, Bucket.DAY);
            case "3days", "3day" -> new ReportRange("3days", today.minusDays(2), today, Bucket.DAY);
            case "7days", "day" -> new ReportRange("7days", today.minusDays(6), today, Bucket.DAY);
            case "30days" -> new ReportRange("30days", today.minusDays(29), today, Bucket.DAY);
            case "custom" -> {
                if (from == null || to == null)
                    throw new IllegalArgumentException("Vui lòng chọn đầy đủ từ ngày và đến ngày.");
                long days = ChronoUnit.DAYS.between(from, to) + 1;
                yield new ReportRange("custom", from, to, days <= 90 ? Bucket.DAY : days <= 730 ? Bucket.MONTH : Bucket.YEAR);
            }
            // Compatibility for existing bookmarks/API calls.
            case "hour" -> new ReportRange("hour", today, today, Bucket.HOUR);
            case "month" -> new ReportRange("month", today.withDayOfYear(1), LocalDate.of(today.getYear(), 12, 31), Bucket.MONTH);
            case "year" -> new ReportRange("year", LocalDate.of(today.getYear() - 4, 1, 1), LocalDate.of(today.getYear(), 12, 31), Bucket.YEAR);
            default -> throw new IllegalArgumentException("Khoảng thời gian thống kê không hợp lệ.");
        };
    }

    public LocalDateTime start() { return startDate.atStartOfDay(); }
    public LocalDateTime endExclusive() { return endDate.plusDays(1).atStartOfDay(); }
    public boolean contains(LocalDateTime time) {
        return time != null && !time.isBefore(start()) && time.isBefore(endExclusive());
    }
    public String label() {
        var format = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return startDate.format(format) + " – " + endDate.format(format);
    }
    public LocalDateTime key(LocalDateTime time) {
        return switch (bucket) {
            case HOUR -> time.withMinute(0).withSecond(0).withNano(0);
            case DAY -> time.toLocalDate().atStartOfDay();
            case MONTH -> time.toLocalDate().withDayOfMonth(1).atStartOfDay();
            case YEAR -> time.toLocalDate().withDayOfYear(1).atStartOfDay();
        };
    }
    public List<LocalDateTime> keys() {
        List<LocalDateTime> keys = new ArrayList<>();
        for (LocalDateTime current = key(start()); current.isBefore(endExclusive()); current = switch (bucket) {
            case HOUR -> current.plusHours(1);
            case DAY -> current.plusDays(1);
            case MONTH -> current.plusMonths(1);
            case YEAR -> current.plusYears(1);
        }) keys.add(current);
        return keys;
    }
    public String bucketLabel(LocalDateTime key) {
        return switch (bucket) {
            case HOUR -> key.getHour() + "h";
            case DAY -> key.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            case MONTH -> key.format(DateTimeFormatter.ofPattern("MM/yyyy"));
            case YEAR -> String.valueOf(key.getYear());
        };
    }
}
