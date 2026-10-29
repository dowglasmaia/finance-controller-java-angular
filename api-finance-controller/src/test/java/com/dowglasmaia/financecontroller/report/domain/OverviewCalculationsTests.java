package com.dowglasmaia.financecontroller.report.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class OverviewCalculationsTests {

    @ParameterizedTest
    @CsvSource({
        // atual, anterior, esperado ("-" = sem base)
        "100, 0, -", "100, -5, -", "0, 0, -",
        "1100, 1000, 10.0", "1250, 1000, 25.0", "500, 1000, -50.0", "0, 1000, -100.0", "3000, 1000, 200.0",
        "1004, 1000, 0", "996, 1000, 0", "1004.99, 1000, 0", "1005, 1000, 0.5", "995, 1000, -0.5",
        "1333.33, 1000, 33.3", "1066.66, 1000, 6.7", "100.05, 100, 0", "112.345, 100, 12.3"
    })
    void variationFollowsRnSal02(String current, String previous, String expected) {
        var result = Variation.percent(new BigDecimal(current), new BigDecimal(previous));

        if (expected.equals("-")) {
            assertThat(result).isEmpty();
        } else {
            assertThat(result).contains(expected);
        }
    }

    @Test
    void variationBelowHalfPercentIsExactlyZeroEvenWhenRoundingWouldNotBe() {
        // 0,4999...% ainda é "0"; o corte é feito sobre o valor exato, não sobre o arredondado
        assertThat(Variation.percent(new BigDecimal("100.4999"), new BigDecimal("100"))).contains("0");
        assertThat(Variation.percent(new BigDecimal("100.5000"), new BigDecimal("100"))).contains("0.5");
    }

    @ParameterizedTest
    @CsvSource({"2026-01, 5", "2026-02, 4", "2028-02, 5", "2026-04, 5", "2026-10, 5"})
    void bandCountIsCeilOfLastDayOverSeven(String month, int expected) {
        assertThat(MonthBands.bandCount(YearMonth.parse(month))).isEqualTo(expected);
        assertThat(MonthBands.of(YearMonth.parse(month), List.of())).hasSize(expected);
    }

    @Test
    void daysFallInBandsOfSevenAndTheRestOfTheMonthStaysInTheLastBand() {
        var month = YearMonth.of(2026, 10);
        var bands = MonthBands.of(month, List.of(
            day("2026-10-01", "10.00", "0.00"), day("2026-10-07", "5.00", "1.00"), // S1
            day("2026-10-08", "0.00", "2.00"), day("2026-10-14", "0.00", "3.00"), // S2
            day("2026-10-29", "7.00", "4.00"), day("2026-10-31", "1.00", "1.00"), // S5
            day("2026-09-30", "999.00", "999.00"))); // fora do mês: ignorado

        assertThat(bands).extracting(MonthBands.Band::label).containsExactly("S1", "S2", "S3", "S4", "S5");
        assertThat(bands.get(0).income()).isEqualByComparingTo("15.00");
        assertThat(bands.get(0).expense()).isEqualByComparingTo("1.00");
        assertThat(bands.get(1).expense()).isEqualByComparingTo("5.00");
        assertThat(bands.get(2).income()).isEqualByComparingTo("0.00");
        assertThat(bands.get(4).income()).isEqualByComparingTo("8.00");
        assertThat(bands.get(4).expense()).isEqualByComparingTo("5.00");
    }

    @Test
    void leapDayFebruaryTwentyNinthFallsInS5() {
        var bands = MonthBands.of(YearMonth.of(2028, 2), List.of(day("2028-02-29", "50.00", "0.00"), day("2028-02-28", "1.00", "0.00")));

        assertThat(bands).hasSize(5);
        assertThat(bands.get(4).income()).isEqualByComparingTo("50.00");
        assertThat(bands.get(3).income()).isEqualByComparingTo("1.00");
    }

    @Test
    void monthBoundaryDaysBelongToTheirOwnMonth() {
        var january = MonthBands.of(YearMonth.of(2026, 1), List.of(day("2026-01-31", "10.00", "0.00"), day("2026-02-01", "99.00", "0.00")));
        var february = MonthBands.of(YearMonth.of(2026, 2), List.of(day("2026-01-31", "10.00", "0.00"), day("2026-02-01", "99.00", "0.00")));

        assertThat(january.get(4).income()).isEqualByComparingTo("10.00");
        assertThat(january.stream().map(MonthBands.Band::income).reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("10.00");
        assertThat(february.get(0).income()).isEqualByComparingTo("99.00");
    }

    @Test
    void percentageHasFourDecimalsHalfUpAndZeroTotalIsSafe() {
        assertThat(Percentage.of(new BigDecimal("1"), new BigDecimal("3")).toPlainString()).isEqualTo("33.3333");
        assertThat(Percentage.of(new BigDecimal("2"), new BigDecimal("3")).toPlainString()).isEqualTo("66.6667");
        assertThat(Percentage.of(new BigDecimal("50"), new BigDecimal("200")).toPlainString()).isEqualTo("25.0000");
        assertThat(Percentage.of(new BigDecimal("5"), BigDecimal.ZERO).toPlainString()).isEqualTo("0.0000");
    }

    private static MonthBands.Day day(String date, String income, String expense) {
        return new MonthBands.Day(LocalDate.parse(date), new BigDecimal(income), new BigDecimal(expense));
    }
}
