package com.compass.digitalbank.exception;

public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(Long accountId) {
        super("Insufficient balance for account id: " + accountId);
    }
}
