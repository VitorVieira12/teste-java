package com.compass.digitalbank.service;

import com.compass.digitalbank.dto.NotificationResponse;
import com.compass.digitalbank.repository.NotificationLogRepository;
import com.compass.digitalbank.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationQueryService {

    private final NotificationLogRepository notificationLogRepository;

    public NotificationQueryService(NotificationLogRepository notificationLogRepository) {
        this.notificationLogRepository = notificationLogRepository;
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(Pageable pageable) {
        Long accountId = SecurityUtils.currentAccountId();
        return notificationLogRepository.findByAccountIdOrderBySentAtDesc(accountId, pageable)
                .map(NotificationResponse::from);
    }
}
