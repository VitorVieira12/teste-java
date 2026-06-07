package com.compass.digitalbank.service;

import com.compass.digitalbank.config.AppProperties;
import com.compass.digitalbank.domain.entity.Account;
import com.compass.digitalbank.domain.entity.User;
import com.compass.digitalbank.dto.AccountResponse;
import com.compass.digitalbank.dto.CreateAccountRequest;
import com.compass.digitalbank.exception.AccountNotFoundException;
import com.compass.digitalbank.exception.DuplicateUsernameException;
import com.compass.digitalbank.exception.InvalidRegistrationException;
import com.compass.digitalbank.repository.AccountRepository;
import com.compass.digitalbank.repository.UserRepository;
import com.compass.digitalbank.security.SecurityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    public AccountService(
            AccountRepository accountRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AppProperties appProperties) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.appProperties = appProperties;
    }

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateUsernameException(request.username());
        }

        if (request.initialBalance().compareTo(appProperties.account().maxInitialBalance()) > 0) {
            throw new InvalidRegistrationException(
                    "Initial balance cannot exceed " + appProperties.account().maxInitialBalance());
        }

        Account account = accountRepository.save(new Account(request.name(), request.initialBalance()));
        userRepository.save(new User(
                request.username(),
                passwordEncoder.encode(request.password()),
                account));

        return AccountResponse.from(account);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(Long accountId) {
        SecurityUtils.requireAccountOwnership(accountId);
        return AccountResponse.from(findAccountOrThrow(accountId));
    }

    @Transactional(readOnly = true)
    public AccountResponse getMyAccount() {
        return AccountResponse.from(findAccountOrThrow(SecurityUtils.currentAccountId()));
    }

    Account findAccountOrThrow(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }
}
