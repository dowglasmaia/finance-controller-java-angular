package com.dowglasmaia.financecontroller.transaction.api;

import com.dowglasmaia.financecontroller.generated.transactions.model.BudgetAlert;
import com.dowglasmaia.financecontroller.generated.transactions.model.CategoryRef;
import com.dowglasmaia.financecontroller.generated.transactions.model.Currency;
import com.dowglasmaia.financecontroller.generated.transactions.model.SubcategoryRef;
import com.dowglasmaia.financecontroller.generated.transactions.model.Transaction;
import com.dowglasmaia.financecontroller.generated.transactions.model.TransactionKind;
import com.dowglasmaia.financecontroller.generated.transactions.model.TransactionPage;
import com.dowglasmaia.financecontroller.generated.transactions.model.TransactionRequest;
import com.dowglasmaia.financecontroller.generated.transactions.model.TransactionResult;
import com.dowglasmaia.financecontroller.generated.transactions.model.TransactionTotals;
import com.dowglasmaia.financecontroller.transaction.application.usecase.ListTransactionsUseCase;
import com.dowglasmaia.financecontroller.transaction.application.usecase.TransactionCommand;
import com.dowglasmaia.financecontroller.transaction.application.usecase.TransactionDetails;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;

/** Conversão entre os modelos gerados do contrato e o domínio; nenhum tipo gerado vaza para os casos de uso. */
final class TransactionApiMapper {

    private TransactionApiMapper() {
    }

    static com.dowglasmaia.financecontroller.shared.money.Currency toDomain(Currency currency) {
        return com.dowglasmaia.financecontroller.shared.money.Currency.valueOf(currency.getValue());
    }

    static TransactionCommand toCommand(TransactionRequest request, YearMonth referenceMonth) {
        return new TransactionCommand(
            com.dowglasmaia.financecontroller.transaction.TransactionKind.valueOf(request.getKind().getValue()),
            request.getDescription(), request.getAmount(), toDomain(request.getCurrency()), request.getDate(),
            request.getCategoryId(), request.getSubcategoryId(), request.getNotes(), referenceMonth);
    }

    static Transaction toResponse(TransactionDetails details) {
        var t = details.transaction();
        var response = new Transaction(t.id(), TransactionKind.fromValue(t.kind().name()), t.description(),
            t.amount().toPlainString(), Currency.fromValue(t.currency().name()), t.date(),
            new CategoryRef(details.category().id(), details.category().name(), details.category().color(),
                details.category().icon()),
            utc(t.createdAt()), utc(t.updatedAt()));
        if (details.subcategory() != null) {
            response.subcategory(new SubcategoryRef(details.subcategory().id(), details.subcategory().name()));
        }
        if (t.notes() != null) {
            response.notes(t.notes());
        }
        return response;
    }

    static TransactionResult toResponse(TransactionDetails.Result result) {
        var response = new TransactionResult(toResponse(result.details()));
        result.budgetAlert().ifPresent(alert -> {
            var body = new BudgetAlert(BudgetAlert.ScopeEnum.fromValue(alert.scope().name()),
                BudgetAlert.StatusEnum.fromValue(alert.status().name()));
            body.categoryId(alert.categoryId());
            body.categoryName(alert.categoryName());
            response.budgetAlert(body);
        });
        return response;
    }

    static TransactionPage toResponse(ListTransactionsUseCase.Listing listing) {
        var totals = listing.totals();
        return new TransactionPage(listing.items().stream().map(TransactionApiMapper::toResponse).toList(),
            listing.page(), listing.size(), listing.totalItems(), listing.totalPages(),
            new TransactionTotals(totals.income().toPlainString(), totals.expense().toPlainString(),
                totals.balance().toPlainString()));
    }

    private static OffsetDateTime utc(java.time.Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
