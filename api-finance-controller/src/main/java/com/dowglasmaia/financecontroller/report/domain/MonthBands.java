package com.dowglasmaia.financecontroller.report.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Faixas de 7 dias do mês, S1…S{@code ceil(últimoDia/7)} (RN-REP-03): dias 1–7 em S1, 8–14 em S2 e assim por diante;
 * o resto do mês fica na última faixa (o dia 29 de fevereiro cai em S5).
 */
public final class MonthBands {

    private static final int DAYS_PER_BAND = 7;

    private MonthBands() {
    }

    public record Band(String label, BigDecimal income, BigDecimal expense) {
    }

    /** Um dia com lançamentos. */
    public record Day(LocalDate date, BigDecimal income, BigDecimal expense) {
    }

    public static int bandCount(YearMonth month) {
        return (month.lengthOfMonth() + DAYS_PER_BAND - 1) / DAYS_PER_BAND;
    }

    public static List<Band> of(YearMonth month, List<Day> days) {
        int count = bandCount(month);
        BigDecimal[] income = new BigDecimal[count];
        BigDecimal[] expense = new BigDecimal[count];
        for (int i = 0; i < count; i++) {
            income[i] = BigDecimal.ZERO.setScale(2);
            expense[i] = BigDecimal.ZERO.setScale(2);
        }
        for (Day day : days) {
            if (!YearMonth.from(day.date()).equals(month)) {
                continue;
            }
            int index = (day.date().getDayOfMonth() - 1) / DAYS_PER_BAND;
            income[index] = income[index].add(day.income());
            expense[index] = expense[index].add(day.expense());
        }
        List<Band> bands = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            bands.add(new Band("S" + (i + 1), income[i], expense[i]));
        }
        return bands;
    }
}
