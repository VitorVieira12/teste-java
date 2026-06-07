package com.compass.digitalbank.config;

import com.compass.digitalbank.domain.entity.Account;
import com.compass.digitalbank.domain.entity.User;
import com.compass.digitalbank.repository.AccountRepository;
import com.compass.digitalbank.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(
            AccountRepository accountRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (accountRepository.count() > 0) {
            return;
        }

        seedUser("alice", "senha123", "Alice Silva", new BigDecimal("1000.00"));
        seedUser("bruno", "senha123", "Bruno Costa", new BigDecimal("500.00"));
        seedUser("carla", "senha123", "Carla Souza", new BigDecimal("250.00"));

        log.info("Seeded 3 sample accounts with users (password: senha123)");
    }

    private void seedUser(String username, String password, String name, BigDecimal balance) {
        Account account = accountRepository.save(new Account(name, balance));
        userRepository.save(new User(username, passwordEncoder.encode(password), account));
    }
}
