package com.compass.digitalbank.service;

import com.compass.digitalbank.domain.entity.Account;
import com.compass.digitalbank.domain.entity.Transfer;
import com.compass.digitalbank.domain.enums.TransferStatus;
import com.compass.digitalbank.dto.TransferRequest;
import com.compass.digitalbank.dto.TransferResponse;
import com.compass.digitalbank.exception.AccountNotFoundException;
import com.compass.digitalbank.exception.ForbiddenException;
import com.compass.digitalbank.exception.InsufficientBalanceException;
import com.compass.digitalbank.exception.InvalidTransferException;
import com.compass.digitalbank.notification.TransferCompletedEvent;
import com.compass.digitalbank.notification.TransferFailedEvent;
import com.compass.digitalbank.repository.AccountRepository;
import com.compass.digitalbank.repository.MovementRepository;
import com.compass.digitalbank.repository.TransferRepository;
import com.compass.digitalbank.security.AuthenticatedAccount;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private TransferTransactionService transferTransactionService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TransferService transferService;

    @BeforeEach
    void setUp() {
        AuthenticatedAccount principal = new AuthenticatedAccount(1L, "alice", "pwd");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRejectTransferToSameAccount() {
        TransferRequest request = new TransferRequest(1L, 1L, new BigDecimal("10.00"));

        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(InvalidTransferException.class)
                .hasMessageContaining("different");

        verify(transferTransactionService, never()).executeTransfer(any());
        verifyFailureNotificationPublished(request);
    }

    @Test
    void shouldRejectNonPositiveAmount() {
        TransferRequest request = new TransferRequest(1L, 2L, BigDecimal.ZERO);

        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(InvalidTransferException.class)
                .hasMessageContaining("greater than zero");

        verify(transferTransactionService, never()).executeTransfer(any());
        verifyFailureNotificationPublished(request);
    }

    @Test
    void shouldRejectTransferFromAnotherAccount() {
        TransferRequest request = new TransferRequest(2L, 3L, new BigDecimal("50.00"));

        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("your own account");

        verify(transferTransactionService, never()).executeTransfer(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldPublishFailureNotificationOnInsufficientBalance() {
        TransferRequest request = new TransferRequest(1L, 2L, new BigDecimal("500.00"));

        when(transferTransactionService.executeTransfer(request))
                .thenThrow(new InsufficientBalanceException(1L));

        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(InsufficientBalanceException.class);

        ArgumentCaptor<TransferFailedEvent> captor = ArgumentCaptor.forClass(TransferFailedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().accountId()).isEqualTo(1L);
        assertThat(captor.getValue().message()).contains("insufficient balance");
    }

    @Test
    void shouldDelegateValidTransferToTransactionService() {
        TransferRequest request = new TransferRequest(1L, 2L, new BigDecimal("50.00"));
        TransferResponse expected = new TransferResponse(10L, 1L, 2L, request.amount(), TransferStatus.COMPLETED, null);

        when(transferTransactionService.executeTransfer(request)).thenReturn(expected);

        TransferResponse response = transferService.transfer(request);

        assertThat(response).isEqualTo(expected);
        verify(transferTransactionService).executeTransfer(request);
        verify(eventPublisher, never()).publishEvent(any(TransferFailedEvent.class));
    }

    private void verifyFailureNotificationPublished(TransferRequest request) {
        ArgumentCaptor<TransferFailedEvent> captor = ArgumentCaptor.forClass(TransferFailedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().accountId()).isEqualTo(request.fromAccountId());
        assertThat(captor.getValue().toAccountId()).isEqualTo(request.toAccountId());
    }
}

@ExtendWith(MockitoExtension.class)
class TransferTransactionServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private MovementRepository movementRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TransferTransactionService transferTransactionService;

    private Account fromAccount;
    private Account toAccount;

    @BeforeEach
    void setUp() throws Exception {
        fromAccount = new Account("Alice", new BigDecimal("100.00"));
        toAccount = new Account("Bruno", new BigDecimal("50.00"));
        setEntityId(fromAccount, 1L);
        setEntityId(toAccount, 2L);
    }

    private void setEntityId(Account account, Long id) throws Exception {
        Field idField = Account.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(account, id);
    }

    @Test
    void shouldTransferFundsSuccessfully() {
        TransferRequest request = new TransferRequest(1L, 2L, new BigDecimal("30.00"));

        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(toAccount));
        when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransferResponse response = transferTransactionService.executeTransfer(request);

        assertThat(fromAccount.getBalance()).isEqualByComparingTo("70.00");
        assertThat(toAccount.getBalance()).isEqualByComparingTo("80.00");
        assertThat(response.fromAccountId()).isEqualTo(1L);
        assertThat(response.toAccountId()).isEqualTo(2L);
        assertThat(response.amount()).isEqualByComparingTo("30.00");
        assertThat(response.status()).isEqualTo(TransferStatus.COMPLETED);

        verify(movementRepository, org.mockito.Mockito.times(2)).save(any());
        ArgumentCaptor<TransferCompletedEvent> eventCaptor = ArgumentCaptor.forClass(TransferCompletedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().amount()).isEqualByComparingTo("30.00");
    }

    @Test
    void shouldThrowWhenAccountNotFound() {
        TransferRequest request = new TransferRequest(1L, 2L, new BigDecimal("10.00"));

        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transferTransactionService.executeTransfer(request))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void shouldThrowWhenInsufficientBalance() {
        TransferRequest request = new TransferRequest(1L, 2L, new BigDecimal("150.00"));

        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(toAccount));

        assertThatThrownBy(() -> transferTransactionService.executeTransfer(request))
                .isInstanceOf(InsufficientBalanceException.class);

        assertThat(fromAccount.getBalance()).isEqualByComparingTo("100.00");
        assertThat(toAccount.getBalance()).isEqualByComparingTo("50.00");
        verify(transferRepository, never()).save(any());
    }
}
