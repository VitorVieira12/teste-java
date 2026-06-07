package com.compass.digitalbank.notification;

import java.math.BigDecimal;

public record TransferFailedEvent(
        Long accountId,
        Long toAccountId,
        BigDecimal amount,
        String message
) {
}
