package com.onlinebanking.service;

import com.onlinebanking.dto.TransferDto;
import com.onlinebanking.entity.*;
import com.onlinebanking.repository.*;
import com.onlinebanking.util.IdGeneratorUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TransactionService {
    private final TransactionRepository txnRepo;
    private final AccountRepository accountRepo;
    private final AuditService auditService;

    public Transaction deposit(Account account, BigDecimal amount, String remarks) {
        account.setBalance(account.getBalance().add(amount));
        accountRepo.save(account);
        String username = (account.getCustomer() != null && account.getCustomer().getUser() != null) ? account.getCustomer().getUser().getUsername() : "SYSTEM";
        auditService.log(username, "DEPOSIT", "Deposited ₹" + amount + " to account " + account.getAccountNumber());
        return txnRepo.save(Transaction.builder()
            .transactionId(IdGeneratorUtil.generateTransactionId())
            .receiverAccount(account).amount(amount)
            .transactionType(Transaction.TransactionType.DEPOSIT)
            .status(Transaction.TransactionStatus.SUCCESS)
            .balanceAfter(account.getBalance())
            .receiverBalanceAfter(account.getBalance())
            .remarks(remarks != null ? remarks : "Deposit").build());
    }

    public Transaction withdraw(Account account, BigDecimal amount, String remarks) {
        if (account.getBalance().compareTo(amount) < 0)
            throw new RuntimeException("Insufficient balance");
        account.setBalance(account.getBalance().subtract(amount));
        accountRepo.save(account);
        String username = (account.getCustomer() != null && account.getCustomer().getUser() != null) ? account.getCustomer().getUser().getUsername() : "SYSTEM";
        auditService.log(username, "WITHDRAWAL", "Withdrew ₹" + amount + " from account " + account.getAccountNumber());
        return txnRepo.save(Transaction.builder()
            .transactionId(IdGeneratorUtil.generateTransactionId())
            .senderAccount(account).amount(amount)
            .transactionType(Transaction.TransactionType.WITHDRAWAL)
            .status(Transaction.TransactionStatus.SUCCESS)
            .balanceAfter(account.getBalance())
            .senderBalanceAfter(account.getBalance())
            .remarks(remarks != null ? remarks : "Withdrawal").build());
    }

    public Transaction transfer(TransferDto dto) {
        Account sender = accountRepo.findByAccountNumber(dto.getFromAccount())
            .orElseThrow(() -> new RuntimeException("Sender account not found"));
        Account receiver = accountRepo.findByAccountNumber(dto.getToAccount())
            .orElseThrow(() -> new RuntimeException("Receiver account not found"));
        if (dto.getFromAccount().equals(dto.getToAccount()))
            throw new RuntimeException("Self-transfer is not allowed");
        if (dto.getAmount().compareTo(BigDecimal.ZERO) <= 0)
            throw new RuntimeException("Transfer amount must be positive");
        if (sender.getStatus() != Account.AccountStatus.ACTIVE)
            throw new RuntimeException("Sender account is not active");
        if (receiver.getStatus() != Account.AccountStatus.ACTIVE)
            throw new RuntimeException("Receiver account is not active");
        if (!"1234".equals(dto.getTransactionPin()))
            throw new RuntimeException("Invalid transaction PIN entered");
        if (!"123456".equals(dto.getOtp()))
            throw new RuntimeException("Invalid or expired OTP");
        if (sender.getBalance().compareTo(dto.getAmount()) < 0)
            throw new RuntimeException("Insufficient balance");
        sender.setBalance(sender.getBalance().subtract(dto.getAmount()));
        receiver.setBalance(receiver.getBalance().add(dto.getAmount()));
        accountRepo.save(sender); accountRepo.save(receiver);
        Transaction.TransactionType type = Transaction.TransactionType.TRANSFER;
        if (dto.getTransferType() != null) {
            try { type = Transaction.TransactionType.valueOf(dto.getTransferType()); } catch (Exception ignored) {}
        }
        String username = (sender.getCustomer() != null && sender.getCustomer().getUser() != null) ? sender.getCustomer().getUser().getUsername() : "SYSTEM";
        auditService.log(username, "TRANSFER", "Transferred ₹" + dto.getAmount() + " from " + dto.getFromAccount() + " to " + dto.getToAccount());
        return txnRepo.save(Transaction.builder()
            .transactionId(IdGeneratorUtil.generateTransactionId())
            .senderAccount(sender).receiverAccount(receiver)
            .amount(dto.getAmount()).transactionType(type)
            .status(Transaction.TransactionStatus.SUCCESS)
            .balanceAfter(sender.getBalance())
            .senderBalanceAfter(sender.getBalance())
            .receiverBalanceAfter(receiver.getBalance())
            .remarks(dto.getRemarks() != null ? dto.getRemarks() : "Fund Transfer").build());
    }

    public List<Transaction> getByAccount(Account account) {
        return txnRepo.findBySenderAccountOrReceiverAccountOrderByTransactionDateDesc(account, account);
    }
    public List<Transaction> getAll() { return txnRepo.findAll(); }
}
