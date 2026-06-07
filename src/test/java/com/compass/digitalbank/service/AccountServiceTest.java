package com.compass.digitalbank.service;

import com.compass.digitalbank.config.AppProperties;
import com.compass.digitalbank.domain.entity.Account;
import com.compass.digitalbank.domain.entity.User;
import com.compass.digitalbank.dto.CreateAccountRequest;
import com.compass.digitalbank.exception.AccountNotFoundException;
import com.compass.digitalbank.exception.DuplicateUsernameException;
import com.compass.digitalbank.exception.ForbiddenException;
import com.compass.digitalbank.exception.InvalidRegistrationException;
import com.compass.digitalbank.repository.AccountRepository;
import com.compass.digitalbank.repository.UserRepository;
import com.compass.digitalbank.security.AuthenticatedAccount;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private AccountService accountService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateAccountWithUser() {
        when(appProperties.account()).thenReturn(new AppProperties.Account(new BigDecimal("1000.00")));
        CreateAccountRequest request = new CreateAccountRequest(
                "John Doe", "johndoe", "secret12", new BigDecimal("150.00"));

        when(userRepository.existsByUsername("johndoe")).thenReturn(false);
        when(passwordEncoder.encode("secret12")).thenReturn("encoded-password");
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = accountService.createAccount(request);

        assertThat(response.name()).isEqualTo("John Doe");
        assertThat(response.balance()).isEqualByComparingTo("150.00");
        verify(accountRepository).save(any(Account.class));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldRejectDuplicateUsername() {
        CreateAccountRequest request = new CreateAccountRequest(
                "John Doe", "johndoe", "secret12", new BigDecimal("100.00"));

        when(userRepository.existsByUsername("johndoe")).thenReturn(true);

        assertThatThrownBy(() -> accountService.createAccount(request))
                .isInstanceOf(DuplicateUsernameException.class);

        verify(accountRepository, never()).save(any());
    }

    @Test
    void shouldRejectInitialBalanceAboveLimit() {
        when(appProperties.account()).thenReturn(new AppProperties.Account(new BigDecimal("1000.00")));
        CreateAccountRequest request = new CreateAccountRequest(
                "John Doe", "johndoe", "secret12", new BigDecimal("1500.00"));

        when(userRepository.existsByUsername("johndoe")).thenReturn(false);

        assertThatThrownBy(() -> accountService.createAccount(request))
                .isInstanceOf(InvalidRegistrationException.class);

        verify(accountRepository, never()).save(any());
    }

    @Test
    void shouldReturnAccountWhenOwner() throws Exception {
        Account account = new Account("Jane Doe", new BigDecimal("300.00"));
        setEntityId(account, 1L);
        authenticateAs(1L);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        var response = accountService.getAccount(1L);

        assertThat(response.name()).isEqualTo("Jane Doe");
    }

    @Test
    void shouldDenyAccessToOtherAccount() throws Exception {
        authenticateAs(1L);

        assertThatThrownBy(() -> accountService.getAccount(2L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void shouldThrowWhenAccountNotFound() throws Exception {
        authenticateAs(99L);
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccount(99L))
                .isInstanceOf(AccountNotFoundException.class);
    }

    private void authenticateAs(Long accountId) {
        AuthenticatedAccount principal = new AuthenticatedAccount(accountId, "user", "pwd");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private void setEntityId(Account account, Long id) throws Exception {
        Field idField = Account.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(account, id);
    }
}
