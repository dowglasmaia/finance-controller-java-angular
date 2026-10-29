package com.dowglasmaia.financecontroller.transaction.application;

import com.dowglasmaia.financecontroller.shared.money.Currency;
import com.dowglasmaia.financecontroller.transaction.TransactionKind;
import com.dowglasmaia.financecontroller.transaction.TransactionQuery;
import com.dowglasmaia.financecontroller.transaction.application.port.out.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/** Implementa a API pública de leitura sobre a porta de saída; sem regra própria. */
@Service
class TransactionQueryService implements TransactionQuery {

    private final TransactionRepository transactions;

    TransactionQueryService(TransactionRepository transactions) {
        this.transactions = transactions;
    }

    @Override
    @Transactional(readOnly = true)
    public MonthTotals monthTotals(UUID ownerId, Currency currency, YearMonth month) {
        var totals = transactions.totals(ownerId, month, currency);
        return new MonthTotals(totals.income(), totals.expense());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyTotals> dailyTotals(UUID ownerId, Currency currency, YearMonth month) {
        return transactions.dailyTotals(ownerId, month, currency);
    }

    @Override
    @Transactional(readOnly = true)
    public MonthTotals rangeTotals(UUID ownerId, Currency currency, LocalDate from, LocalDate to) {
        var totals = transactions.totals(ownerId, from, to, currency);
        return new MonthTotals(totals.income(), totals.expense());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyTotals> dailyTotals(UUID ownerId, Currency currency, LocalDate from, LocalDate to) {
        return transactions.dailyTotals(ownerId, from, to, currency);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecentTransaction> latest(UUID ownerId, Currency currency, YearMonth month, int limit) {
        return transactions.findPage(ownerId, month, currency, null, 1, limit).items().stream()
            .map(t -> new RecentTransaction(t.id(), TransactionKind.valueOf(t.kind().name()), t.description(), t.amount(),
                t.currency(), t.date(), t.categoryId(), t.subcategoryId()))
            .toList();
    }
}
