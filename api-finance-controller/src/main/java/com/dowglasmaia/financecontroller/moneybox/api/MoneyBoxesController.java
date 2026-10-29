package com.dowglasmaia.financecontroller.moneybox.api;

import com.dowglasmaia.financecontroller.generated.moneyboxes.api.MoneyBoxesApi;
import com.dowglasmaia.financecontroller.generated.moneyboxes.model.Currency;
import com.dowglasmaia.financecontroller.generated.moneyboxes.model.MoneyBox;
import com.dowglasmaia.financecontroller.generated.moneyboxes.model.MoneyBoxEntry;
import com.dowglasmaia.financecontroller.generated.moneyboxes.model.SaveMoneyBoxEntryRequest;
import com.dowglasmaia.financecontroller.generated.moneyboxes.model.SaveMoneyBoxRequest;
import com.dowglasmaia.financecontroller.moneybox.application.usecase.CreateMoneyBoxEntryUseCase;
import com.dowglasmaia.financecontroller.moneybox.application.usecase.CreateMoneyBoxUseCase;
import com.dowglasmaia.financecontroller.moneybox.application.usecase.DeleteMoneyBoxEntryUseCase;
import com.dowglasmaia.financecontroller.moneybox.application.usecase.DeleteMoneyBoxUseCase;
import com.dowglasmaia.financecontroller.moneybox.application.usecase.GetMoneyBoxUseCase;
import com.dowglasmaia.financecontroller.moneybox.application.usecase.ListMoneyBoxEntriesUseCase;
import com.dowglasmaia.financecontroller.moneybox.application.usecase.ListMoneyBoxesUseCase;
import com.dowglasmaia.financecontroller.moneybox.application.usecase.UpdateMoneyBoxEntryUseCase;
import com.dowglasmaia.financecontroller.moneybox.application.usecase.UpdateMoneyBoxUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Só mapeia e delega; as regras vivem nos casos de uso. */
@RestController
public class MoneyBoxesController implements MoneyBoxesApi {

    private final ListMoneyBoxesUseCase listMoneyBoxes;
    private final CreateMoneyBoxUseCase createMoneyBox;
    private final GetMoneyBoxUseCase getMoneyBox;
    private final UpdateMoneyBoxUseCase updateMoneyBox;
    private final DeleteMoneyBoxUseCase deleteMoneyBox;
    private final ListMoneyBoxEntriesUseCase listEntries;
    private final CreateMoneyBoxEntryUseCase createEntry;
    private final UpdateMoneyBoxEntryUseCase updateEntry;
    private final DeleteMoneyBoxEntryUseCase deleteEntry;

    public MoneyBoxesController(ListMoneyBoxesUseCase listMoneyBoxes, CreateMoneyBoxUseCase createMoneyBox,
                                GetMoneyBoxUseCase getMoneyBox, UpdateMoneyBoxUseCase updateMoneyBox,
                                DeleteMoneyBoxUseCase deleteMoneyBox, ListMoneyBoxEntriesUseCase listEntries,
                                CreateMoneyBoxEntryUseCase createEntry, UpdateMoneyBoxEntryUseCase updateEntry,
                                DeleteMoneyBoxEntryUseCase deleteEntry) {
        this.listMoneyBoxes = listMoneyBoxes;
        this.createMoneyBox = createMoneyBox;
        this.getMoneyBox = getMoneyBox;
        this.updateMoneyBox = updateMoneyBox;
        this.deleteMoneyBox = deleteMoneyBox;
        this.listEntries = listEntries;
        this.createEntry = createEntry;
        this.updateEntry = updateEntry;
        this.deleteEntry = deleteEntry;
    }

    @Override
    public ResponseEntity<List<MoneyBox>> listMoneyBoxes(Currency currency) {
        return ResponseEntity.ok(listMoneyBoxes.execute(MoneyBoxApiMapper.toDomain(currency)).stream()
            .map(MoneyBoxApiMapper::toResponse).toList());
    }

    @Override
    public ResponseEntity<MoneyBox> createMoneyBox(SaveMoneyBoxRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(MoneyBoxApiMapper.toResponse(createMoneyBox.execute(MoneyBoxApiMapper.toCommand(request))));
    }

    @Override
    public ResponseEntity<MoneyBox> getMoneyBox(UUID moneyBoxId) {
        return ResponseEntity.ok(MoneyBoxApiMapper.toResponse(getMoneyBox.execute(moneyBoxId)));
    }

    @Override
    public ResponseEntity<MoneyBox> updateMoneyBox(UUID moneyBoxId, SaveMoneyBoxRequest request) {
        return ResponseEntity.ok(MoneyBoxApiMapper.toResponse(
            updateMoneyBox.execute(moneyBoxId, MoneyBoxApiMapper.toCommand(request))));
    }

    @Override
    public ResponseEntity<Void> deleteMoneyBox(UUID moneyBoxId) {
        deleteMoneyBox.execute(moneyBoxId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<MoneyBoxEntry>> listMoneyBoxEntries(UUID moneyBoxId) {
        return ResponseEntity.ok(listEntries.execute(moneyBoxId).stream().map(MoneyBoxApiMapper::toResponse).toList());
    }

    @Override
    public ResponseEntity<MoneyBoxEntry> createMoneyBoxEntry(UUID moneyBoxId, SaveMoneyBoxEntryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(MoneyBoxApiMapper.toResponse(
            createEntry.execute(moneyBoxId, MoneyBoxApiMapper.toCommand(request))));
    }

    @Override
    public ResponseEntity<MoneyBoxEntry> updateMoneyBoxEntry(UUID moneyBoxId, UUID entryId,
                                                             SaveMoneyBoxEntryRequest request) {
        return ResponseEntity.ok(MoneyBoxApiMapper.toResponse(
            updateEntry.execute(moneyBoxId, entryId, MoneyBoxApiMapper.toCommand(request))));
    }

    @Override
    public ResponseEntity<Void> deleteMoneyBoxEntry(UUID moneyBoxId, UUID entryId) {
        deleteEntry.execute(moneyBoxId, entryId);
        return ResponseEntity.noContent().build();
    }
}
