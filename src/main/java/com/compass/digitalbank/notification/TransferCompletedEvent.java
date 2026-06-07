package com.compass.digitalbank.notification;

import java.math.BigDecimal;

public record TransferCompletedEvent(
        Long transferId,
        Long fromAccountId,
        String fromAccountName,
        Long toAccountId,
        String toAccountName,
        BigDecimal amount
) {
}
