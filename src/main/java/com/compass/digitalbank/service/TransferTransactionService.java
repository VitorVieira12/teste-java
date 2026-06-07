package com.compass.digitalbank.service;

import com.compass.digitalbank.domain.entity.Account;
import com.compass.digitalbank.domain.entity.Movement;
import com.compass.digitalbank.domain.entity.Transfer;
import com.compass.digitalbank.domain.enums.MovementType;
import com.compass.digitalbank.domain.enums.TransferStatus;
import com.compass.digitalbank.dto.TransferRequest;
import com.compass.digitalbank.dto.TransferResponse;
import com.compass.digitalbank.exception.AccountNotFoundException;
import com.compass.digitalbank.exception.InsufficientBalanceException;
import com.compass.digitalbank.notification.TransferCompletedEvent;
import com.compass.digitalbank.repository.AccountRepository;
import com.compass.digitalbank.repository.MovementRepository;
import com.compass.digitalbank.repository.TransferRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferTransactionService {

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final MovementRepository movementRepository;
    private final ApplicationEventPublisher eventPublisher;

    public TransferTransactionService(
            AccountRepository accountRepository,
            TransferRepository transferRepository,
            MovementRepository movementRepository,
            ApplicationEventPublisher eventPublisher) {
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
        this.movementRepository = movementRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public TransferResponse executeTransfer(TransferRequest request) {
        Account firstLockedAccount = lockAccountInOrder(request.fromAccountId(), request.toAccountId(), true);
        Account secondLockedAccount = lockAccountInOrder(request.fromAccountId(), request.toAccountId(), false);

        Account fromAccount = request.fromAccountId() < request.toAccountId()
                ? firstLockedAccount
                : secondLockedAccount;
        Account toAccount = request.fromAccountId() < request.toAccountId()
                ? secondLockedAccount
                : firstLockedAccount;

        if (!fromAccount.hasSufficientBalance(request.amount())) {
            throw new InsufficientBalanceException(fromAccount.getId());
        }

        fromAccount.debit(request.amount());
        toAccount.credit(request.amount());

        Transfer transfer = transferRepository.save(
                new Transfer(fromAccount, toAccount, request.amount(), TransferStatus.COMPLETED));

        movementRepository.save(new Movement(fromAccount, transfer, MovementType.DEBIT, request.amount()));
        movementRepository.save(new Movement(toAccount, transfer, MovementType.CREDIT, request.amount()));

        eventPublisher.publishEvent(new TransferCompletedEvent(
                transfer.getId(),
                fromAccount.getId(),
                fromAccount.getName(),
                toAccount.getId(),
                toAccount.getName(),
                transfer.getAmount()));

        return TransferResponse.from(transfer);
    }

    private Account lockAccountInOrder(Long fromAccountId, Long toAccountId, boolean first) {
        Long accountIdToLock = first
                ? Math.min(fromAccountId, toAccountId)
                : Math.max(fromAccountId, toAccountId);

        return accountRepository.findByIdForUpdate(accountIdToLock)
                .orElseThrow(() -> new AccountNotFoundException(accountIdToLock));
    }
}
