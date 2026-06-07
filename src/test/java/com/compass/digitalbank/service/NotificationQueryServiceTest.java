package com.compass.digitalbank.service;

import com.compass.digitalbank.domain.entity.Account;
import com.compass.digitalbank.domain.entity.NotificationLog;
import com.compass.digitalbank.domain.enums.NotificationStatus;
import com.compass.digitalbank.repository.NotificationLogRepository;
import com.compass.digitalbank.security.AuthenticatedAccount;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationQueryServiceTest {

    @Mock
    private NotificationLogRepository notificationLogRepository;

    @InjectMocks
    private NotificationQueryService notificationQueryService;

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
    void shouldReturnNotificationsForAuthenticatedAccount() {
        Account account = new Account("Alice", new BigDecimal("100.00"));
        NotificationLog log = new NotificationLog(account, null, "Transfer failed", NotificationStatus.SENT);
        Pageable pageable = Pageable.ofSize(20);

        when(notificationLogRepository.findByAccountIdOrderBySentAtDesc(eq(1L), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(log)));

        var page = notificationQueryService.getMyNotifications(pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().message()).isEqualTo("Transfer failed");
        assertThat(page.getContent().getFirst().transferId()).isNull();
    }
}
