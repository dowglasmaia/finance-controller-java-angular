package com.dowglasmaia.financecontroller.report.domain;

import com.dowglasmaia.financecontroller.shared.error.FieldValidationException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;

/**
 * Intervalo do relatório (RN-REP-01): semana de domingo a sábado, mês ou ano que contêm a data âncora; limites
 * inclusivos por dia (sem hora, então sem dependência de fuso).
 */
public record ReportRange(LocalDate start, LocalDate end) {

    public static final LocalDate MIN_ANCHOR = LocalDate.of(2000, 1, 1);
    public static final LocalDate MAX_ANCHOR = LocalDate.of(2100, 12, 31);

    public static ReportRange of(ReportPeriod period, LocalDate anchor) {
        if (anchor == null || anchor.isBefore(MIN_ANCHOR) || anchor.isAfter(MAX_ANCHOR)) {
            throw new FieldValidationException("anchorDate", "validation.date.range");
        }
        return switch (period) {
            case WEEK -> {
                LocalDate sunday = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
                yield new ReportRange(sunday, sunday.plusDays(6));
            }
            case MONTH -> new ReportRange(YearMonth.from(anchor).atDay(1), YearMonth.from(anchor).atEndOfMonth());
            case YEAR -> new ReportRange(Year.from(anchor).atDay(1), LocalDate.of(anchor.getYear(), 12, 31));
        };
    }
}
