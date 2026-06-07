package com.compass.digitalbank.notification;

import com.compass.digitalbank.domain.entity.Account;
import com.compass.digitalbank.domain.entity.NotificationLog;
import com.compass.digitalbank.domain.enums.NotificationStatus;
import com.compass.digitalbank.repository.AccountRepository;
import com.compass.digitalbank.repository.NotificationLogRepository;
import com.compass.digitalbank.repository.TransferRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationLogRepository notificationLogRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransferRepository transferRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void shouldPersistFailureNotificationWithoutTransfer() {
        Account account = new Account("Alice", new BigDecimal("100.00"));
        TransferFailedEvent event = new TransferFailedEvent(
                1L,
                2L,
                new BigDecimal("500.00"),
                "Transfer failed: insufficient balance to send R$ 500.00 to account 2");

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(notificationLogRepository.save(any(NotificationLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.handleTransferFailed(event);

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(captor.capture());

        NotificationLog saved = captor.getValue();
        assertThat(saved.getTransfer()).isNull();
        assertThat(saved.getMessage()).contains("insufficient balance");
        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.SENT);
    }
}
