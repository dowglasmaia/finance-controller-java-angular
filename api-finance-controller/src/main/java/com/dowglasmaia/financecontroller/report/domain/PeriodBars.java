package com.dowglasmaia.financecontroller.report.domain;

import com.dowglasmaia.financecontroller.report.domain.MonthBands.Band;
import com.dowglasmaia.financecontroller.report.domain.MonthBands.Day;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Barras do relatório (RN-REP-03), todas calculadas dos mesmos totais por dia do intervalo dos KPIs: semana Dom…Sáb,
 * mês em faixas de 7 dias (S1…S5) e ano Jan…Dez.
 */
public final class PeriodBars {

    private static final String[] WEEK_DAYS = {"Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb"};
    private static final String[] MONTHS = {"Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez"};

    private PeriodBars() {
    }

    public static List<Band> of(ReportPeriod period, ReportRange range, List<Day> days) {
        return switch (period) {
            case WEEK -> week(range.start(), days);
            case MONTH -> MonthBands.of(YearMonth.from(range.start()), days);
            case YEAR -> year(range.start().getYear(), days);
        };
    }

    private static List<Band> week(LocalDate sunday, List<Day> days) {
        BigDecimal[] income = zeros(7);
        BigDecimal[] expense = zeros(7);
        for (Day day : days) {
            int index = (int) java.time.temporal.ChronoUnit.DAYS.between(sunday, day.date());
            if (index >= 0 && index < 7) {
                income[index] = income[index].add(day.income());
                expense[index] = expense[index].add(day.expense());
            }
        }
        return bands(WEEK_DAYS, income, expense);
    }

    private static List<Band> year(int year, List<Day> days) {
        BigDecimal[] income = zeros(12);
        BigDecimal[] expense = zeros(12);
        for (Day day : days) {
            if (day.date().getYear() == year) {
                int index = day.date().getMonthValue() - 1;
                income[index] = income[index].add(day.income());
                expense[index] = expense[index].add(day.expense());
            }
        }
        return bands(MONTHS, income, expense);
    }

    /** Resumo por mês do ano: {@code [mês][0=entradas, 1=saídas]}. */
    public static BigDecimal[][] monthTotals(int year, List<Day> days) {
        BigDecimal[] income = zeros(12);
        BigDecimal[] expense = zeros(12);
        for (Day day : days) {
            if (day.date().getYear() == year) {
                int index = day.date().getMonthValue() - 1;
                income[index] = income[index].add(day.income());
                expense[index] = expense[index].add(day.expense());
            }
        }
        BigDecimal[][] result = new BigDecimal[12][];
        for (int i = 0; i < 12; i++) {
            result[i] = new BigDecimal[] {income[i], expense[i]};
        }
        return result;
    }

    private static List<Band> bands(String[] labels, BigDecimal[] income, BigDecimal[] expense) {
        List<Band> bands = new ArrayList<>(labels.length);
        for (int i = 0; i < labels.length; i++) {
            bands.add(new Band(labels[i], income[i], expense[i]));
        }
        return bands;
    }

    private static BigDecimal[] zeros(int n) {
        BigDecimal[] values = new BigDecimal[n];
        for (int i = 0; i < n; i++) {
            values[i] = BigDecimal.ZERO.setScale(2);
        }
        return values;
    }
}
