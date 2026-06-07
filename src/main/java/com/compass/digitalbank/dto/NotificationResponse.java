package com.compass.digitalbank.dto;

import com.compass.digitalbank.domain.entity.NotificationLog;
import com.compass.digitalbank.domain.enums.NotificationStatus;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        Long accountId,
        Long transferId,
        String message,
        NotificationStatus status,
        Instant sentAt
) {

    public static NotificationResponse from(NotificationLog notificationLog) {
        return new NotificationResponse(
                notificationLog.getId(),
                notificationLog.getAccount().getId(),
                notificationLog.getTransfer() != null ? notificationLog.getTransfer().getId() : null,
                notificationLog.getMessage(),
                notificationLog.getStatus(),
                notificationLog.getSentAt());
    }
}
