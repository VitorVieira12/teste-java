package com.compass.digitalbank.domain.entity;

import com.compass.digitalbank.domain.enums.NotificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "notification_logs")
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_id")
    private Transfer transfer;

    @Column(nullable = false, length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

    @Column(nullable = false, updatable = false)
    private Instant sentAt;

    protected NotificationLog() {
    }

    public NotificationLog(Account account, Transfer transfer, String message, NotificationStatus status) {
        this.account = account;
        this.transfer = transfer;
        this.message = message;
        this.status = status;
    }

    @PrePersist
    void onCreate() {
        sentAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Account getAccount() {
        return account;
    }

    public Transfer getTransfer() {
        return transfer;
    }

    public String getMessage() {
        return message;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}
