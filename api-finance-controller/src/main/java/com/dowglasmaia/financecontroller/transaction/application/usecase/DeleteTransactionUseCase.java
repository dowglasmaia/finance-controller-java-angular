package com.dowglasmaia.financecontroller.transaction.application.usecase;

import com.dowglasmaia.financecontroller.identity.CurrentOwner;
import com.dowglasmaia.financecontroller.shared.error.ResourceNotFoundException;
import com.dowglasmaia.financecontroller.transaction.application.port.out.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Remove sem verificar dependências; o gasto da meta é recalculado por consulta. */
@Service
public class DeleteTransactionUseCase {

    private final TransactionRepository transactions;
    private final CurrentOwner owner;

    DeleteTransactionUseCase(TransactionRepository transactions, CurrentOwner owner) {
        this.transactions = transactions;
        this.owner = owner;
    }

    @Transactional
    public void execute(UUID transactionId) {
        if (!transactions.delete(owner.id(), transactionId)) {
            throw new ResourceNotFoundException();
        }
    }
}
