package com.compass.digitalbank.service;

import com.compass.digitalbank.domain.entity.Account;
import com.compass.digitalbank.domain.entity.User;
import com.compass.digitalbank.dto.AuthResponse;
import com.compass.digitalbank.dto.LoginRequest;
import com.compass.digitalbank.security.JwtService;
import com.compass.digitalbank.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldLoginSuccessfully() throws Exception {
        LoginRequest request = new LoginRequest("alice", "senha123");
        Account account = new Account("Alice", new BigDecimal("1000.00"));
        setEntityId(account, 1L);
        User user = new User("alice", "hash", account);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(jwtService.generateToken("alice", 1L)).thenReturn("jwt-token");
        when(jwtService.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInMs()).isEqualTo(86400000L);
        assertThat(response.accountId()).isEqualTo(1L);
    }

    private void setEntityId(Account account, Long id) throws Exception {
        java.lang.reflect.Field idField = Account.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(account, id);
    }

    @Test
    void shouldRejectInvalidCredentials() {
        LoginRequest request = new LoginRequest("alice", "wrong");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }
}
