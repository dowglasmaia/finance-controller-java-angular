package com.dowglasmaia.financecontroller.report.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dowglasmaia.financecontroller.report.domain.MonthBands.Band;
import com.dowglasmaia.financecontroller.report.domain.MonthBands.Day;
import com.dowglasmaia.financecontroller.shared.error.FieldValidationException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ReportCalculationsTests {

    private static ReportRange range(ReportPeriod period, String anchor) {
        return ReportRange.of(period, LocalDate.parse(anchor));
    }

    private static Day day(String date, String income, String expense) {
        return new Day(LocalDate.parse(date), new BigDecimal(income), new BigDecimal(expense));
    }

    @Test
    void weekIsSundayToSaturdayAroundTheAnchor() {
        var wednesday = range(ReportPeriod.WEEK, "2026-06-24");
        assertEquals(LocalDate.parse("2026-06-21"), wednesday.start());
        assertEquals(LocalDate.parse("2026-06-27"), wednesday.end());

        assertEquals(LocalDate.parse("2026-06-21"), range(ReportPeriod.WEEK, "2026-06-21").start());
        assertEquals(LocalDate.parse("2026-06-21"), range(ReportPeriod.WEEK, "2026-06-27").start());
        assertEquals(LocalDate.parse("2026-06-28"), range(ReportPeriod.WEEK, "2026-06-28").start());
    }

    @Test
    void weekCanCrossMonthAndYear() {
        var crossing = range(ReportPeriod.WEEK, "2026-12-31");
        assertEquals(LocalDate.parse("2026-12-27"), crossing.start());
        assertEquals(LocalDate.parse("2027-01-02"), crossing.end());
    }

    @Test
    void monthAndYearCoverTheirWholeCalendarUnit() {
        var february = range(ReportPeriod.MONTH, "2028-02-15");
        assertEquals(LocalDate.parse("2028-02-01"), february.start());
        assertEquals(LocalDate.parse("2028-02-29"), february.end());

        var year = range(ReportPeriod.YEAR, "2026-06-15");
        assertEquals(LocalDate.parse("2026-01-01"), year.start());
        assertEquals(LocalDate.parse("2026-12-31"), year.end());
    }

    @Test
    void anchorOutsideTheSupportedRangeIsRefused() {
        assertThrows(FieldValidationException.class, () -> range(ReportPeriod.MONTH, "1999-12-31"));
        assertThrows(FieldValidationException.class, () -> range(ReportPeriod.MONTH, "2101-01-01"));
        assertThrows(FieldValidationException.class, () -> ReportRange.of(ReportPeriod.MONTH, null));
    }

    @Test
    void weekBarsAreSevenDaysFromSundayAndIgnoreDaysOutsideTheRange() {
        var range = range(ReportPeriod.WEEK, "2026-06-24");

        List<Band> bars = PeriodBars.of(ReportPeriod.WEEK, range, List.of(
            day("2026-06-20", "999.00", "0.00"), day("2026-06-21", "10.00", "0.00"),
            day("2026-06-27", "0.00", "5.50"), day("2026-06-28", "999.00", "0.00")));

        assertEquals(List.of("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb"), bars.stream().map(Band::label).toList());
        assertEquals(new BigDecimal("10.00"), bars.get(0).income());
        assertEquals(new BigDecimal("5.50"), bars.get(6).expense());
        assertEquals(new BigDecimal("10.00"), bars.stream().map(Band::income).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    @Test
    void monthBarsHaveCeilOfDaysOverSevenBands() {
        assertEquals(4, PeriodBars.of(ReportPeriod.MONTH, range(ReportPeriod.MONTH, "2026-02-15"), List.of()).size());
        assertEquals(5, PeriodBars.of(ReportPeriod.MONTH, range(ReportPeriod.MONTH, "2026-06-15"), List.of()).size());
        assertEquals(5, PeriodBars.of(ReportPeriod.MONTH, range(ReportPeriod.MONTH, "2026-07-15"), List.of()).size());

        var leap = PeriodBars.of(ReportPeriod.MONTH, range(ReportPeriod.MONTH, "2028-02-15"),
            List.of(day("2028-02-29", "7.00", "0.00")));
        assertEquals(5, leap.size());
        assertEquals(new BigDecimal("7.00"), leap.get(4).income());
    }

    @Test
    void yearBarsAreTwelveMonthsJanToDec() {
        var bars = PeriodBars.of(ReportPeriod.YEAR, range(ReportPeriod.YEAR, "2026-06-15"), List.of(
            day("2026-01-31", "1.00", "0.00"), day("2026-12-01", "0.00", "2.00"), day("2025-12-31", "50.00", "0.00")));

        assertEquals(12, bars.size());
        assertEquals("Jan", bars.get(0).label());
        assertEquals("Dez", bars.get(11).label());
        assertEquals(new BigDecimal("1.00"), bars.get(0).income());
        assertEquals(new BigDecimal("2.00"), bars.get(11).expense());
    }

    @Test
    void balanceChangeUsesTheAbsolutePreviousBalanceAndIsAbsentFromZero() {
        assertEquals(Optional.of("50.0"), Percentage.change(new BigDecimal("150"), new BigDecimal("100")));
        assertEquals(Optional.of("-100.0"), Percentage.change(new BigDecimal("0"), new BigDecimal("150")));
        assertEquals(Optional.of("200.0"), Percentage.change(new BigDecimal("50"), new BigDecimal("-50")));
        assertEquals(Optional.of("-200.0"), Percentage.change(new BigDecimal("-50"), new BigDecimal("50")));
        assertEquals(Optional.of("33.3"), Percentage.change(new BigDecimal("4"), new BigDecimal("3")));
        assertEquals(Optional.empty(), Percentage.change(new BigDecimal("10"), BigDecimal.ZERO));
        assertTrue(Percentage.change(new BigDecimal("10"), null).isEmpty());
    }
}
