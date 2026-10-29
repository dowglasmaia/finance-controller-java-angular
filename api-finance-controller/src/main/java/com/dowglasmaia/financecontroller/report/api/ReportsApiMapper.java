package com.dowglasmaia.financecontroller.report.api;

import com.dowglasmaia.financecontroller.generated.reports.model.CategoryRef;
import com.dowglasmaia.financecontroller.generated.reports.model.CategoryShare;
import com.dowglasmaia.financecontroller.generated.reports.model.Currency;
import com.dowglasmaia.financecontroller.generated.reports.model.DateRange;
import com.dowglasmaia.financecontroller.generated.reports.model.MonthlySummaryItem;
import com.dowglasmaia.financecontroller.generated.reports.model.ReportBar;
import com.dowglasmaia.financecontroller.generated.reports.model.ReportPeriod;
import com.dowglasmaia.financecontroller.generated.reports.model.Summary;
import com.dowglasmaia.financecontroller.report.application.usecase.MonthlyOverview;
import com.dowglasmaia.financecontroller.report.application.usecase.Report;

/** Conversão do resultado do caso de uso para os modelos gerados do contrato. */
final class ReportsApiMapper {

    private ReportsApiMapper() {
    }

    static com.dowglasmaia.financecontroller.generated.reports.model.Report toResponse(Report r) {
        var response = new com.dowglasmaia.financecontroller.generated.reports.model.Report(
            ReportPeriod.fromValue(r.period().name()), Currency.fromValue(r.currency().name()),
            new DateRange(r.range().start(), r.range().end()),
            new Summary(r.summary().income().toPlainString(), r.summary().expense().toPlainString(),
                r.summary().balance().toPlainString()),
            r.bars().stream().map(b -> new ReportBar(b.label(), b.income().toPlainString(), b.expense().toPlainString())).toList(),
            r.expenseByCategory().stream().map(ReportsApiMapper::share).toList(),
            r.ranking().stream().map(ReportsApiMapper::share).toList());
        // o modelo gerado inicia a lista vazia; fora do período ano o campo deve ficar ausente
        response.setMonthlySummary(null);
        if (r.monthlySummary() != null) {
            response.monthlySummary(r.monthlySummary().stream().map(item -> {
                var mapped = new MonthlySummaryItem(item.month().toString(), item.income().toPlainString(),
                    item.expense().toPlainString(), item.balance().toPlainString());
                item.changePercent().ifPresent(mapped::changePercent);
                return mapped;
            }).toList());
        }
        return response;
    }

    private static CategoryShare share(MonthlyOverview.CategoryExpense e) {
        var c = e.category();
        return new CategoryShare(new CategoryRef(c.id(), c.name(), c.color(), c.icon()), e.amount().toPlainString(),
            e.percentage().toPlainString());
    }
}
