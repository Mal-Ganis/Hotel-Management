package com.hotelsystem.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions")
@Data
public class PaymentTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long reservationId;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TransactionType type; // PAYMENT or REFUND

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TransactionStatus status;

    private String providerTransactionId; // 第三方支付返回的交易号

    private String note;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum TransactionType {
        PAYMENT,
        REFUND
    }

    public enum TransactionStatus {
        PENDING,
        SUCCESS,
        FAILED
    }
}
