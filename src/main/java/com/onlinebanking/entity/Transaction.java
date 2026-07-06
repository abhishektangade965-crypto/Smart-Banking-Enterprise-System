package com.onlinebanking.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String transactionId;

    @ManyToOne
    @JoinColumn(name = "sender_account_id")
    private Account senderAccount;

    @ManyToOne
    @JoinColumn(name = "receiver_account_id")
    private Account receiverAccount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.SUCCESS;

    private String remarks;
    private LocalDateTime transactionDate;
    private BigDecimal balanceAfter;
    private BigDecimal senderBalanceAfter;
    private BigDecimal receiverBalanceAfter;

    @PrePersist
    protected void onCreate() { if (transactionDate == null) transactionDate = LocalDateTime.now(); }

    public enum TransactionType { DEPOSIT, WITHDRAWAL, TRANSFER, NEFT, RTGS, IMPS, UPI }
    public enum TransactionStatus { SUCCESS, FAILED, PENDING }
}
