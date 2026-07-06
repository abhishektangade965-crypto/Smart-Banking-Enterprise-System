package com.onlinebanking.service;

import com.onlinebanking.entity.*;
import com.onlinebanking.repository.*;
import com.onlinebanking.util.IdGeneratorUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionSimulatorService {
    private final AccountRepository accountRepo;
    private final TransactionRepository txnRepo;
    private final Random rand = new Random();

    @Scheduled(fixedDelay = 8000) // execute every 8 seconds
    @Transactional
    public void simulateTransaction() {
        try {
            List<Account> accounts = accountRepo.findAll();
            if (accounts.size() < 2) return;

            Account sender = accounts.get(rand.nextInt(accounts.size()));
            Account receiver = accounts.get(rand.nextInt(accounts.size()));

            while (sender.getId().equals(receiver.getId())) {
                receiver = accounts.get(rand.nextInt(accounts.size()));
            }

            BigDecimal amount = BigDecimal.valueOf(rand.nextInt(950) + 50); // random amount between 50 and 1000

            if (sender.getBalance().compareTo(amount) >= 0) {
                sender.setBalance(sender.getBalance().subtract(amount));
                receiver.setBalance(receiver.getBalance().add(amount));

                accountRepo.save(sender);
                accountRepo.save(receiver);

                txnRepo.save(Transaction.builder()
                    .transactionId(IdGeneratorUtil.generateTransactionId())
                    .senderAccount(sender)
                    .receiverAccount(receiver)
                    .amount(amount)
                    .transactionType(Transaction.TransactionType.TRANSFER)
                    .status(Transaction.TransactionStatus.SUCCESS)
                    .remarks("Automated Ledger Settlement")
                    .transactionDate(LocalDateTime.now())
                    .senderBalanceAfter(sender.getBalance())
                    .receiverBalanceAfter(receiver.getBalance()).build());
            } else {
                // Sender doesn't have enough money, simulate deposit
                BigDecimal depositAmount = BigDecimal.valueOf(rand.nextInt(4000) + 1000);
                sender.setBalance(sender.getBalance().add(depositAmount));
                accountRepo.save(sender);

                txnRepo.save(Transaction.builder()
                    .transactionId(IdGeneratorUtil.generateTransactionId())
                    .receiverAccount(sender)
                    .amount(depositAmount)
                    .transactionType(Transaction.TransactionType.DEPOSIT)
                    .status(Transaction.TransactionStatus.SUCCESS)
                    .remarks("Automated Liquidity Replenishment")
                    .transactionDate(LocalDateTime.now())
                    .balanceAfter(sender.getBalance())
                    .receiverBalanceAfter(sender.getBalance()).build());
            }
        } catch (Exception e) {
            log.warn("Simulator transaction failed: " + e.getMessage());
        }
    }
}
