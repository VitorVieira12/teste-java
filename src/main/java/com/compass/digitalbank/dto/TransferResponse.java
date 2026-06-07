package com.compass.digitalbank.dto;

import com.compass.digitalbank.domain.entity.Transfer;
import com.compass.digitalbank.domain.enums.TransferStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record TransferResponse(
        Long id,
        Long fromAccountId,
        Long toAccountId,
        BigDecimal amount,
        TransferStatus status,
        Instant createdAt
) {

    public static TransferResponse from(Transfer transfer) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getFromAccount().getId(),
                transfer.getToAccount().getId(),
                transfer.getAmount(),
                transfer.getStatus(),
                transfer.getCreatedAt()
        );
    }
}
