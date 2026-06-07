package com.compass.digitalbank.integration;

import com.compass.digitalbank.dto.TransferRequest;
import com.compass.digitalbank.repository.AccountRepository;
import com.compass.digitalbank.service.TransferTransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class TransferConcurrencyIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("digitalbank")
            .withUsername("digitalbank")
            .withPassword("digitalbank");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TransferTransactionService transferTransactionService;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void shouldProcessParallelTransfersConsistently() throws Exception {
        BigDecimal aliceInitial = accountRepository.findById(1L).orElseThrow().getBalance();
        BigDecimal brunoInitial = accountRepository.findById(2L).orElseThrow().getBalance();

        int transferCount = 10;
        BigDecimal transferAmount = new BigDecimal("10.00");

        ExecutorService executor = Executors.newFixedThreadPool(transferCount);
        try {
            List<Callable<Void>> tasks = new ArrayList<>();
            for (int i = 0; i < transferCount; i++) {
                tasks.add(() -> {
                    transferTransactionService.executeTransfer(
                            new TransferRequest(1L, 2L, transferAmount));
                    return null;
                });
            }

            List<Future<Void>> results = executor.invokeAll(tasks);
            for (Future<Void> result : results) {
                result.get();
            }
        } finally {
            executor.shutdownNow();
        }

        BigDecimal expectedAlice = aliceInitial.subtract(transferAmount.multiply(BigDecimal.valueOf(transferCount)));
        BigDecimal expectedBruno = brunoInitial.add(transferAmount.multiply(BigDecimal.valueOf(transferCount)));

        assertThat(accountRepository.findById(1L).orElseThrow().getBalance()).isEqualByComparingTo(expectedAlice);
        assertThat(accountRepository.findById(2L).orElseThrow().getBalance()).isEqualByComparingTo(expectedBruno);
    }

    @Test
    void shouldHandleBidirectionalConcurrentTransfersWithoutDeadlock() throws Exception {
        BigDecimal aliceInitial = accountRepository.findById(1L).orElseThrow().getBalance();
        BigDecimal brunoInitial = accountRepository.findById(2L).orElseThrow().getBalance();

        int transfersPerDirection = 5;
        BigDecimal transferAmount = new BigDecimal("20.00");

        ExecutorService executor = Executors.newFixedThreadPool(transfersPerDirection * 2);
        try {
            List<Callable<Void>> tasks = new ArrayList<>();
            for (int i = 0; i < transfersPerDirection; i++) {
                tasks.add(() -> {
                    transferTransactionService.executeTransfer(new TransferRequest(1L, 2L, transferAmount));
                    return null;
                });
                tasks.add(() -> {
                    transferTransactionService.executeTransfer(new TransferRequest(2L, 1L, transferAmount));
                    return null;
                });
            }

            List<Future<Void>> results = executor.invokeAll(tasks);
            for (Future<Void> result : results) {
                result.get();
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(accountRepository.findById(1L).orElseThrow().getBalance()).isEqualByComparingTo(aliceInitial);
        assertThat(accountRepository.findById(2L).orElseThrow().getBalance()).isEqualByComparingTo(brunoInitial);
    }
}
