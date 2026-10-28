package com.finalproject;

import com.finalproject.service.ReportRange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class ReportRangeTests {
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-12T16:59:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));

    @ParameterizedTest
    @CsvSource({"today,2026-10-12,1", "3days,2026-10-10,3", "7days,2026-10-06,7", "30days,2026-09-13,30", "day,2026-10-06,7"})
    void presetsIncludeTodayAndExactlyTheSelectedDays(String period, LocalDate start, int count) {
        var range = ReportRange.resolve(period, null, null, clock);
        assertEquals(start, range.startDate());
        assertEquals(LocalDate.of(2026, 10, 12), range.endDate());
        assertEquals(count, range.keys().size());
        assertFalse(range.contains(start.atStartOfDay().minusNanos(1)));
        assertTrue(range.contains(start.atStartOfDay()));
        assertTrue(range.contains(range.endExclusive().minusNanos(1)));
        assertFalse(range.contains(range.endExclusive()));
    }

    @Test void customRangeIncludesBothDatesAcrossYearBoundary() {
        var range = ReportRange.resolve("custom", LocalDate.of(2025, 12, 31), LocalDate.of(2026, 1, 2), clock);
        assertEquals(3, range.keys().size());
        assertEquals("31/12/2025 – 02/01/2026", range.label());
        assertEquals("01/01/2026", range.bucketLabel(range.keys().get(1)));
    }

    @Test void emptySelectionDefaultsToSevenDaysButDatesAloneSelectCustom() {
        assertEquals("7days", ReportRange.resolve(null, null, null, clock).period());
        assertEquals("custom", ReportRange.resolve(null, LocalDate.now(clock), LocalDate.now(clock), clock).period());
    }

    @Test void presetsIgnorePreviouslySelectedCustomDates() {
        var range = ReportRange.resolve("today", LocalDate.of(2000, 1, 1), LocalDate.of(2001, 1, 1), clock);
        assertEquals(LocalDate.now(clock), range.startDate());
    }

    @Test void rejectsMissingReversedOrUnsupportedDates() {
        var date = LocalDate.now(clock);
        assertThrows(IllegalArgumentException.class, () -> ReportRange.resolve("custom", date, null, clock));
        assertThrows(IllegalArgumentException.class, () -> ReportRange.resolve("custom", null, date, clock));
        assertThrows(IllegalArgumentException.class, () -> ReportRange.resolve("custom", date, date.minusDays(1), clock));
        assertThrows(IllegalArgumentException.class, () -> ReportRange.resolve("custom", date, LocalDate.of(9999, 1, 1), clock));
        assertThrows(IllegalArgumentException.class, () -> ReportRange.resolve("invalid", null, null, clock));
    }

    @Test void longRangesUseBoundedMonthlyOrYearlyBucketsWithoutExtendingQueryBounds() {
        var monthly = ReportRange.resolve("custom", LocalDate.of(2025, 12, 20), LocalDate.of(2026, 4, 2), clock);
        assertEquals(ReportRange.Bucket.MONTH, monthly.bucket());
        assertEquals(5, monthly.keys().size());
        assertFalse(monthly.contains(LocalDate.of(2025, 12, 19).atStartOfDay()));
        var yearly = ReportRange.resolve("custom", LocalDate.of(2020, 5, 10), LocalDate.of(2026, 10, 12), clock);
        assertEquals(ReportRange.Bucket.YEAR, yearly.bucket());
        assertEquals(7, yearly.keys().size());
    }

    @Test void preservesLegacyHourMonthYearBookmarks() {
        assertEquals(24, ReportRange.resolve("hour", null, null, clock).keys().size());
        assertEquals(12, ReportRange.resolve("month", null, null, clock).keys().size());
        assertEquals(5, ReportRange.resolve("year", null, null, clock).keys().size());
    }
}
