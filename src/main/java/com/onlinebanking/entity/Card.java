package com.onlinebanking.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cards")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String cardNumber;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    private CardType cardType;

    private LocalDate expiryDate;

    @Transient
    private String cvv;

    public String getCvv() {
        if (this.cvv == null && this.cardNumber != null) {
            int hash = Math.abs(this.cardNumber.hashCode());
            this.cvv = String.format("%03d", (hash % 900) + 100);
        }
        return this.cvv;
    }

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CardStatus status = CardStatus.ACTIVE;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum CardType { DEBIT, CREDIT }
    public enum CardStatus { ACTIVE, BLOCKED, EXPIRED, CANCELLED }
}
