package com.compass.digitalbank.notification;

import com.compass.digitalbank.domain.entity.Account;
import com.compass.digitalbank.domain.entity.NotificationLog;
import com.compass.digitalbank.domain.entity.Transfer;
import com.compass.digitalbank.domain.enums.NotificationStatus;
import com.compass.digitalbank.exception.AccountNotFoundException;
import com.compass.digitalbank.repository.AccountRepository;
import com.compass.digitalbank.repository.NotificationLogRepository;
import com.compass.digitalbank.repository.TransferRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationLogRepository notificationLogRepository;
    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;

    public NotificationService(
            NotificationLogRepository notificationLogRepository,
            AccountRepository accountRepository,
            TransferRepository transferRepository) {
        this.notificationLogRepository = notificationLogRepository;
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleTransferCompleted(TransferCompletedEvent event) {
        Transfer transfer = transferRepository.findById(event.transferId())
                .orElseThrow(() -> new IllegalStateException("Transfer not found: " + event.transferId()));

        notifyAccount(event.fromAccountId(), transfer,
                "Transfer sent: R$ %s to account %d (%s)".formatted(
                        event.amount(), event.toAccountId(), event.toAccountName()));
        notifyAccount(event.toAccountId(), transfer,
                "Transfer received: R$ %s from account %d (%s)".formatted(
                        event.amount(), event.fromAccountId(), event.fromAccountName()));
    }

    @Async
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleTransferFailed(TransferFailedEvent event) {
        Account account = accountRepository.findById(event.accountId())
                .orElseThrow(() -> new AccountNotFoundException(event.accountId()));

        log.warn("Transfer failure notification for account {} ({}): {}",
                account.getId(), account.getName(), event.message());
        notificationLogRepository.save(new NotificationLog(account, null, event.message(), NotificationStatus.SENT));
    }

    private void notifyAccount(Long accountId, Transfer transfer, String message) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        log.info("Notification sent to account {} ({}): {}", account.getId(), account.getName(), message);
        notificationLogRepository.save(new NotificationLog(account, transfer, message, NotificationStatus.SENT));
    }
}
