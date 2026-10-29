package com.dowglasmaia.financecontroller.moneybox.api;

import com.dowglasmaia.financecontroller.generated.moneyboxes.model.Currency;
import com.dowglasmaia.financecontroller.generated.moneyboxes.model.MoneyBox;
import com.dowglasmaia.financecontroller.generated.moneyboxes.model.MoneyBoxEntry;
import com.dowglasmaia.financecontroller.generated.moneyboxes.model.MoneyBoxEntryKind;
import com.dowglasmaia.financecontroller.generated.moneyboxes.model.SaveMoneyBoxEntryRequest;
import com.dowglasmaia.financecontroller.generated.moneyboxes.model.SaveMoneyBoxRequest;
import com.dowglasmaia.financecontroller.moneybox.application.usecase.EntryView;
import com.dowglasmaia.financecontroller.moneybox.application.usecase.MoneyBoxView;
import com.dowglasmaia.financecontroller.moneybox.application.usecase.SaveEntryCommand;
import com.dowglasmaia.financecontroller.moneybox.application.usecase.SaveMoneyBoxCommand;
import com.dowglasmaia.financecontroller.moneybox.domain.EntryKind;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** Conversão entre os modelos gerados do contrato e o domínio; nenhum tipo gerado vaza para os casos de uso. */
final class MoneyBoxApiMapper {

    private MoneyBoxApiMapper() {
    }

    static com.dowglasmaia.financecontroller.shared.money.Currency toDomain(Currency currency) {
        return currency == null ? null : com.dowglasmaia.financecontroller.shared.money.Currency.valueOf(currency.getValue());
    }

    static SaveMoneyBoxCommand toCommand(SaveMoneyBoxRequest request) {
        return new SaveMoneyBoxCommand(request.getName(), request.getDescription(), request.getIcon(),
            request.getColor(), request.getGoal(), toDomain(request.getCurrency()));
    }

    static SaveEntryCommand toCommand(SaveMoneyBoxEntryRequest request) {
        return new SaveEntryCommand(EntryKind.valueOf(request.getKind().getValue()), request.getAmount(),
            request.getDate(), request.getNote(), toDomain(request.getCurrency()));
    }

    static MoneyBox toResponse(MoneyBoxView view) {
        var box = view.box();
        var response = new MoneyBox(box.id(), box.name(), box.icon(), box.color(),
            Currency.fromValue(box.currency().name()), view.balance().toPlainString(),
            view.progress().toPlainString(), utc(box.createdAt()));
        if (box.description() != null) {
            response.description(box.description());
        }
        if (box.goal() != null) {
            response.goal(box.goal().toPlainString());
        }
        return response;
    }

    static MoneyBoxEntry toResponse(EntryView view) {
        var entry = view.entry();
        var response = new MoneyBoxEntry(entry.id(), entry.moneyBoxId(), MoneyBoxEntryKind.fromValue(entry.kind().name()),
            entry.amount().toPlainString(), Currency.fromValue(view.currency().name()), entry.date(),
            utc(entry.createdAt()));
        if (entry.note() != null) {
            response.note(entry.note());
        }
        return response;
    }

    private static OffsetDateTime utc(java.time.Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
