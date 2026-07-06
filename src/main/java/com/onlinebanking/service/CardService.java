package com.onlinebanking.service;

import com.onlinebanking.entity.*;
import com.onlinebanking.repository.CardRepository;
import com.onlinebanking.util.IdGeneratorUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CardService {
    private final CardRepository cardRepo;

    public Card requestCard(Account account, Card.CardType type) {
        return cardRepo.save(Card.builder()
            .cardNumber(IdGeneratorUtil.generateCardNumber())
            .account(account).cardType(type)
            .expiryDate(LocalDate.now().plusYears(5))
            .cvv(IdGeneratorUtil.generateCVV())
            .status(Card.CardStatus.ACTIVE).build());
    }

    public void blockCard(Long id) {
        Card c = cardRepo.findById(id).orElseThrow();
        c.setStatus(Card.CardStatus.BLOCKED);
        cardRepo.save(c);
    }

    public void unblockCard(Long id) {
        Card c = cardRepo.findById(id).orElseThrow();
        c.setStatus(Card.CardStatus.ACTIVE);
        cardRepo.save(c);
    }

    public List<Card> getByAccount(Account account) { return cardRepo.findByAccount(account); }
    public List<Card> getAll() { return cardRepo.findAll(); }
    public Card getById(Long id) { return cardRepo.findById(id).orElseThrow(); }
}
