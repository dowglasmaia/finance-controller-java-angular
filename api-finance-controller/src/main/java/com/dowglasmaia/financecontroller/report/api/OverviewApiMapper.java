package com.dowglasmaia.financecontroller.report.api;

import com.dowglasmaia.financecontroller.category.CategoryDirectory.CategoryView;
import com.dowglasmaia.financecontroller.generated.overview.model.Bar;
import com.dowglasmaia.financecontroller.generated.overview.model.BudgetProgress;
import com.dowglasmaia.financecontroller.generated.overview.model.BudgetStatus;
import com.dowglasmaia.financecontroller.generated.overview.model.CategoryExpense;
import com.dowglasmaia.financecontroller.generated.overview.model.CategoryRef;
import com.dowglasmaia.financecontroller.generated.overview.model.Currency;
import com.dowglasmaia.financecontroller.generated.overview.model.OverviewTransaction;
import com.dowglasmaia.financecontroller.generated.overview.model.SubcategoryRef;
import com.dowglasmaia.financecontroller.generated.overview.model.Totals;
import com.dowglasmaia.financecontroller.generated.overview.model.TransactionKind;
import com.dowglasmaia.financecontroller.report.application.usecase.MonthlyOverview;

/** Conversão do resultado do caso de uso para os modelos gerados do contrato. */
final class OverviewApiMapper {

    private OverviewApiMapper() {
    }

    static com.dowglasmaia.financecontroller.generated.overview.model.MonthlyOverview toResponse(MonthlyOverview o) {
        var response = new com.dowglasmaia.financecontroller.generated.overview.model.MonthlyOverview(
            o.month().toString(),
            Currency.fromValue(o.currency().name()),
            totals(o.summary()),
            totals(o.previous()),
            o.reservesTotal().toPlainString(),
            o.bars().stream().map(b -> new Bar(b.label(), b.income().toPlainString(), b.expense().toPlainString())).toList(),
            o.expenseByCategory().stream().map(OverviewApiMapper::expense).toList(),
            o.latestTransactions().stream().map(OverviewApiMapper::transaction).toList(),
            budget(o));
        o.incomeDeltaPercent().ifPresent(response::incomeDeltaPercent);
        o.expenseDeltaPercent().ifPresent(response::expenseDeltaPercent);
        return response;
    }

    private static Totals totals(MonthlyOverview.Totals t) {
        return new Totals(t.income().toPlainString(), t.expense().toPlainString(), t.balance().toPlainString());
    }

    private static CategoryRef ref(CategoryView c) {
        return new CategoryRef(c.id(), c.name(), c.color(), c.icon());
    }

    private static CategoryExpense expense(MonthlyOverview.CategoryExpense e) {
        return new CategoryExpense(ref(e.category()), e.amount().toPlainString(), e.percentage().toPlainString());
    }

    private static OverviewTransaction transaction(MonthlyOverview.LatestTransaction t) {
        var response = new OverviewTransaction(t.id(), TransactionKind.fromValue(t.kind().name()), t.description(),
            t.amount().toPlainString(), Currency.fromValue(t.currency().name()), t.date(), ref(t.category()));
        if (t.subcategory() != null) {
            response.subcategory(new SubcategoryRef(t.subcategory().id(), t.subcategory().name()));
        }
        return response;
    }

    private static BudgetProgress budget(MonthlyOverview o) {
        var p = o.budgetProgress();
        var response = new BudgetProgress(p.spent().toPlainString(), BudgetStatus.fromValue(p.status().name()));
        if (p.limit() != null) {
            response.limit(p.limit().toPlainString());
            response.fraction(p.fraction().toPlainString());
            response.remaining(p.remaining().toPlainString());
        }
        return response;
    }
}
