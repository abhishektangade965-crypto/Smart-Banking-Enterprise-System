package com.onlinebanking.service;

import com.onlinebanking.entity.*;
import com.onlinebanking.repository.*;
import com.onlinebanking.util.IdGeneratorUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountService {
    private final AccountRepository accountRepo;

    private static final BigDecimal MINIMUM_OPENING_BALANCE = new BigDecimal("5000.00");

    /** Opens an account with the default minimum opening balance of ₹5,000. */
    public Account openAccount(Customer customer, Branch branch, Account.AccountType type) {
        return openAccount(customer, branch, type, MINIMUM_OPENING_BALANCE);
    }

    /** Opens an account with a specified initial deposit (must be ≥ ₹5,000). */
    public Account openAccount(Customer customer, Branch branch, Account.AccountType type, BigDecimal initialDeposit) {
        if (initialDeposit == null || initialDeposit.compareTo(MINIMUM_OPENING_BALANCE) < 0) {
            throw new RuntimeException("Initial deposit must be at least \u20B95,000 to open an account");
        }
        Account account = Account.builder()
            .accountNumber(IdGeneratorUtil.generateAccountNumber())
            .customer(customer).branch(branch)
            .accountType(type).balance(initialDeposit)
            .openingDate(LocalDate.now())
            .status(Account.AccountStatus.ACTIVE).build();
        return accountRepo.save(account);
    }

    public Account getByAccountNumber(String number) {
        return accountRepo.findByAccountNumber(number)
            .orElseThrow(() -> new RuntimeException("Account not found: " + number));
    }

    public List<Account> getByCustomer(Customer customer) { return accountRepo.findByCustomer(customer); }
    public List<Account> getAllAccounts() { return accountRepo.findAll(); }

    public void updateStatus(Long id, Account.AccountStatus status) {
        Account a = accountRepo.findById(id).orElseThrow();
        a.setStatus(status);
        accountRepo.save(a);
    }

    public void deposit(Account account, BigDecimal amount) {
        account.setBalance(account.getBalance().add(amount));
        accountRepo.save(account);
    }

    public void withdraw(Account account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0)
            throw new RuntimeException("Insufficient balance");
        account.setBalance(account.getBalance().subtract(amount));
        accountRepo.save(account);
    }
}
