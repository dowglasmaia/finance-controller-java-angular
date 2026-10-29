package com.dowglasmaia.financecontroller.moneybox.domain;

import com.dowglasmaia.financecontroller.shared.error.DomainException;
import com.dowglasmaia.financecontroller.shared.error.ErrorCode;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;

/**
 * Histórico de um cofrinho. O saldo é a soma dos depósitos menos a dos saques (RN-BOX-02) e, em qualquer ponto do
 * histórico (por data, depois criação), nunca fica negativo (RN-BOX-05, P-04).
 */
public final class Ledger {

    private static final Comparator<MoneyBoxEntry> CHRONOLOGICAL = Comparator
        .comparing(MoneyBoxEntry::date)
        .thenComparing(MoneyBoxEntry::createdAt)
        .thenComparing(MoneyBoxEntry::id);

    private Ledger() {
    }

    public static BigDecimal balance(Collection<MoneyBoxEntry> entries) {
        return entries.stream().map(MoneyBoxEntry::signedAmount).reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }

    public static boolean neverNegative(Collection<MoneyBoxEntry> entries) {
        BigDecimal running = BigDecimal.ZERO;
        for (MoneyBoxEntry entry : entries.stream().sorted(CHRONOLOGICAL).toList()) {
            running = running.add(entry.signedAmount());
            if (running.signum() < 0) {
                return false;
            }
        }
        return true;
    }

    /** @throws DomainException {@code INSUFFICIENT_BALANCE} se o histórico informado deixaria o saldo negativo */
    public static void requireNeverNegative(Collection<MoneyBoxEntry> entries) {
        if (!neverNegative(entries)) {
            throw new DomainException(ErrorCode.INSUFFICIENT_BALANCE);
        }
    }
}
