package com.onlinebanking.repository;

import com.onlinebanking.entity.Account;
import com.onlinebanking.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByTransactionId(String transactionId);
    List<Transaction> findBySenderAccountOrReceiverAccountOrderByTransactionDateDesc(Account sender, Account receiver);
    Page<Transaction> findBySenderAccountOrReceiverAccount(Account sender, Account receiver, Pageable pageable);
    List<Transaction> findByTransactionDateBetween(LocalDateTime start, LocalDateTime end);
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.transactionType = 'DEPOSIT' AND t.status = 'SUCCESS'")
    BigDecimal sumTotalDeposits();
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.transactionDate >= :since")
    long countTransactionsSince(LocalDateTime since);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.transactionDate >= :from AND t.transactionDate < :to")
    long countTransactionsBetween(LocalDateTime from, LocalDateTime to);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.status = 'SUCCESS'")
    BigDecimal sumAllTransactionAmounts();

    List<Transaction> findTop10ByOrderByTransactionDateDesc();
}
