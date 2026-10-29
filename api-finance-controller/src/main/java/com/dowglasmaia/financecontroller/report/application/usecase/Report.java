package com.dowglasmaia.financecontroller.report.application.usecase;

import com.dowglasmaia.financecontroller.report.domain.MonthBands.Band;
import com.dowglasmaia.financecontroller.report.domain.ReportPeriod;
import com.dowglasmaia.financecontroller.report.domain.ReportRange;
import com.dowglasmaia.financecontroller.shared.money.Currency;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/** Relatório já calculado; {@code monthlySummary} só existe para o período ano. */
public record Report(ReportPeriod period, Currency currency, ReportRange range, MonthlyOverview.Totals summary,
                     List<Band> bars, List<MonthlyOverview.CategoryExpense> expenseByCategory,
                     List<MonthlyOverview.CategoryExpense> ranking, List<MonthlyItem> monthlySummary) {

    public record MonthlyItem(YearMonth month, BigDecimal income, BigDecimal expense, BigDecimal balance,
                              Optional<String> changePercent) {
    }
}
