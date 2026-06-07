package com.compass.digitalbank.dto;

import com.compass.digitalbank.domain.entity.Movement;
import com.compass.digitalbank.domain.enums.MovementType;

import java.math.BigDecimal;
import java.time.Instant;

public record MovementResponse(
        Long id,
        Long accountId,
        Long transferId,
        MovementType type,
        BigDecimal amount,
        Instant createdAt
) {

    public static MovementResponse from(Movement movement) {
        return new MovementResponse(
                movement.getId(),
                movement.getAccount().getId(),
                movement.getTransfer().getId(),
                movement.getType(),
                movement.getAmount(),
                movement.getCreatedAt()
        );
    }
}
