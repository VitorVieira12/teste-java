package com.compass.digitalbank.repository;

import com.compass.digitalbank.domain.entity.NotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    Page<NotificationLog> findByAccountIdOrderBySentAtDesc(Long accountId, Pageable pageable);
}
