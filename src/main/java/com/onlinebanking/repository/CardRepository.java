package com.onlinebanking.repository;

import com.onlinebanking.entity.Account;
import com.onlinebanking.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {
    Optional<Card> findByCardNumber(String cardNumber);
    List<Card> findByAccount(Account account);
    List<Card> findByAccountAndStatus(Account account, Card.CardStatus status);
}
