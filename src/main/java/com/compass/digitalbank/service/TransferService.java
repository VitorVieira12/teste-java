package com.compass.digitalbank.service;

import com.compass.digitalbank.dto.TransferRequest;
import com.compass.digitalbank.dto.TransferResponse;
import com.compass.digitalbank.exception.AccountNotFoundException;
import com.compass.digitalbank.exception.ForbiddenException;
import com.compass.digitalbank.exception.InsufficientBalanceException;
import com.compass.digitalbank.exception.InvalidTransferException;
import com.compass.digitalbank.notification.TransferFailedEvent;
import com.compass.digitalbank.security.SecurityUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class TransferService {

    private static final int MAX_RETRIES = 3;

    private final TransferTransactionService transferTransactionService;
    private final ApplicationEventPublisher eventPublisher;

    public TransferService(
            TransferTransactionService transferTransactionService,
            ApplicationEventPublisher eventPublisher) {
        this.transferTransactionService = transferTransactionService;
        this.eventPublisher = eventPublisher;
    }

    public TransferResponse transfer(TransferRequest request) {
        try {
            validateRequest(request);
            requireTransferOwnership(request);
            return executeWithRetry(request);
        } catch (InsufficientBalanceException ex) {
            publishTransferFailure(request,
                    "Transfer failed: insufficient balance to send R$ %s to account %d"
                            .formatted(request.amount(), request.toAccountId()));
            throw ex;
        } catch (AccountNotFoundException ex) {
            publishTransferFailure(request, ex.getMessage());
            throw ex;
        } catch (InvalidTransferException ex) {
            publishTransferFailure(request, ex.getMessage());
            throw ex;
        }
    }

    private TransferResponse executeWithRetry(TransferRequest request) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return transferTransactionService.executeTransfer(request);
            } catch (ObjectOptimisticLockingFailureException ex) {
                if (attempt == MAX_RETRIES) {
                    throw new InvalidTransferException("Transfer failed due to concurrent update. Please retry.");
                }
            }
        }

        throw new InvalidTransferException("Transfer failed after retries.");
    }

    private void publishTransferFailure(TransferRequest request, String message) {
        eventPublisher.publishEvent(new TransferFailedEvent(
                request.fromAccountId(),
                request.toAccountId(),
                request.amount(),
                message));
    }

    private void requireTransferOwnership(TransferRequest request) {
        if (!request.fromAccountId().equals(SecurityUtils.currentAccountId())) {
            throw new ForbiddenException("You can only transfer from your own account");
        }
    }

    private void validateRequest(TransferRequest request) {
        if (request.fromAccountId().equals(request.toAccountId())) {
            throw new InvalidTransferException("Source and destination accounts must be different.");
        }

        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransferException("Transfer amount must be greater than zero.");
        }
    }
}
