package com.dowglasmaia.financecontroller.transaction.api;

import com.dowglasmaia.financecontroller.generated.transactions.api.TransactionsApi;
import com.dowglasmaia.financecontroller.generated.transactions.model.Currency;
import com.dowglasmaia.financecontroller.generated.transactions.model.Transaction;
import com.dowglasmaia.financecontroller.generated.transactions.model.TransactionPage;
import com.dowglasmaia.financecontroller.generated.transactions.model.TransactionRequest;
import com.dowglasmaia.financecontroller.generated.transactions.model.TransactionResult;
import com.dowglasmaia.financecontroller.shared.error.FieldValidationException;
import com.dowglasmaia.financecontroller.transaction.application.usecase.CreateTransactionUseCase;
import com.dowglasmaia.financecontroller.transaction.application.usecase.DeleteTransactionUseCase;
import com.dowglasmaia.financecontroller.transaction.application.usecase.GetTransactionUseCase;
import com.dowglasmaia.financecontroller.transaction.application.usecase.ListTransactionsUseCase;
import com.dowglasmaia.financecontroller.transaction.application.usecase.UpdateTransactionUseCase;
import com.dowglasmaia.financecontroller.transaction.domain.TransactionKind;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.UUID;

/** Só mapeia e delega; as regras vivem nos casos de uso. */
@RestController
public class TransactionsController implements TransactionsApi {

    private final ListTransactionsUseCase listTransactions;
    private final CreateTransactionUseCase createTransaction;
    private final GetTransactionUseCase getTransaction;
    private final UpdateTransactionUseCase updateTransaction;
    private final DeleteTransactionUseCase deleteTransaction;

    public TransactionsController(ListTransactionsUseCase listTransactions, CreateTransactionUseCase createTransaction,
                                  GetTransactionUseCase getTransaction, UpdateTransactionUseCase updateTransaction,
                                  DeleteTransactionUseCase deleteTransaction) {
        this.listTransactions = listTransactions;
        this.createTransaction = createTransaction;
        this.getTransaction = getTransaction;
        this.updateTransaction = updateTransaction;
        this.deleteTransaction = deleteTransaction;
    }

    @Override
    public ResponseEntity<TransactionPage> listTransactions(String month, Currency currency, String type, Integer page,
                                                            Integer size) {
        var listing = listTransactions.execute(parseMonth(month, "month"), TransactionApiMapper.toDomain(currency),
            parseKindFilter(type), page, size);
        return ResponseEntity.ok(TransactionApiMapper.toResponse(listing));
    }

    @Override
    public ResponseEntity<TransactionResult> createTransaction(TransactionRequest request) {
        var result = createTransaction.execute(TransactionApiMapper.toCommand(request, referenceMonth(request)));
        return ResponseEntity.status(HttpStatus.CREATED).body(TransactionApiMapper.toResponse(result));
    }

    @Override
    public ResponseEntity<Transaction> getTransaction(UUID transactionId) {
        return ResponseEntity.ok(TransactionApiMapper.toResponse(getTransaction.execute(transactionId)));
    }

    @Override
    public ResponseEntity<TransactionResult> updateTransaction(UUID transactionId, TransactionRequest request) {
        var result = updateTransaction.execute(transactionId,
            TransactionApiMapper.toCommand(request, referenceMonth(request)));
        return ResponseEntity.ok(TransactionApiMapper.toResponse(result));
    }

    @Override
    public ResponseEntity<Void> deleteTransaction(UUID transactionId) {
        deleteTransaction.execute(transactionId);
        return ResponseEntity.noContent().build();
    }

    private static YearMonth referenceMonth(TransactionRequest request) {
        return request.getReferenceMonth() == null ? null : parseMonth(request.getReferenceMonth(), "referenceMonth");
    }

    private static YearMonth parseMonth(String raw, String field) {
        try {
            return YearMonth.parse(raw);
        } catch (DateTimeParseException invalid) {
            throw new FieldValidationException(field, "validation.invalid");
        }
    }

    /** {@code ALL} (padrão) devolve entradas e saídas; qualquer outro valor fora do enum é recusado. */
    private static TransactionKind parseKindFilter(String type) {
        return switch (type == null ? "ALL" : type) {
            case "ALL" -> null;
            case "INCOME" -> TransactionKind.INCOME;
            case "EXPENSE" -> TransactionKind.EXPENSE;
            default -> throw new FieldValidationException("type", "validation.type.invalid");
        };
    }
}
