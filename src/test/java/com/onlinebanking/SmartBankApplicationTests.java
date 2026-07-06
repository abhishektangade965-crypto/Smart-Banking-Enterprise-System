package com.onlinebanking;

import com.onlinebanking.dto.TransferDto;
import com.onlinebanking.entity.*;
import com.onlinebanking.repository.*;
import com.onlinebanking.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("h2")
@Transactional
class SmartBankApplicationTests {

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void contextLoads() {
        assertNotNull(accountService);
        assertNotNull(transactionService);
    }

    @Test
    void testTransferValidationAndBalances() {
        Customer customer = customerRepository.findByUsername("customer1").orElse(null);
        assertNotNull(customer, "Demo customer should exist");

        Branch branch = branchRepository.findAll().get(0);
        Account sender = accountService.openAccount(customer, branch, Account.AccountType.SAVINGS, new BigDecimal("10000.00"));
        Account receiver = accountService.openAccount(customer, branch, Account.AccountType.CURRENT, new BigDecimal("5000.00"));

        assertNotNull(sender.getAccountNumber());
        assertNotNull(receiver.getAccountNumber());

        TransferDto transferDto = new TransferDto();
        transferDto.setFromAccount(sender.getAccountNumber());
        transferDto.setToAccount(receiver.getAccountNumber());
        transferDto.setAmount(new BigDecimal("2000.00"));
        transferDto.setTransferType("IMPS");
        transferDto.setRemarks("Test Transfer");
        transferDto.setTransactionPin("1234");
        transferDto.setOtp("123456");

        Transaction txn = transactionService.transfer(transferDto);
        assertNotNull(txn);
        assertEquals(Transaction.TransactionStatus.SUCCESS, txn.getStatus());

        Account updatedSender = accountService.getByAccountNumber(sender.getAccountNumber());
        Account updatedReceiver = accountService.getByAccountNumber(receiver.getAccountNumber());

        assertEquals(new BigDecimal("8000.00"), updatedSender.getBalance());
        assertEquals(new BigDecimal("7000.00"), updatedReceiver.getBalance());

        assertEquals(new BigDecimal("8000.00"), txn.getSenderBalanceAfter());
        assertEquals(new BigDecimal("7000.00"), txn.getReceiverBalanceAfter());
    }

    @Test
    void testTransferInsufficientBalance() {
        Customer customer = customerRepository.findByUsername("customer1").orElse(null);
        assertNotNull(customer, "Demo customer should exist");
        Branch branch = branchRepository.findAll().get(0);

        Account sender = accountService.openAccount(customer, branch, Account.AccountType.SAVINGS, new BigDecimal("5000.00"));
        Account receiver = accountService.openAccount(customer, branch, Account.AccountType.CURRENT, new BigDecimal("5000.00"));

        TransferDto transferDto = new TransferDto();
        transferDto.setFromAccount(sender.getAccountNumber());
        transferDto.setToAccount(receiver.getAccountNumber());
        transferDto.setAmount(new BigDecimal("6000.00"));
        transferDto.setTransferType("IMPS");
        transferDto.setRemarks("Test Overdraft");
        transferDto.setTransactionPin("1234");
        transferDto.setOtp("123456");

        Exception exception = assertThrows(RuntimeException.class, () -> {
            transactionService.transfer(transferDto);
        });

        assertTrue(exception.getMessage().contains("Insufficient balance"));
    }

    @Test
    void testTransferNegativeAmountRejection() {
        Customer customer = customerRepository.findByUsername("customer1").orElse(null);
        assertNotNull(customer);
        Branch branch = branchRepository.findAll().get(0);
        Account sender = accountService.openAccount(customer, branch, Account.AccountType.SAVINGS, new BigDecimal("5000.00"));
        Account receiver = accountService.openAccount(customer, branch, Account.AccountType.CURRENT, new BigDecimal("5000.00"));

        TransferDto transferDto = new TransferDto();
        transferDto.setFromAccount(sender.getAccountNumber());
        transferDto.setToAccount(receiver.getAccountNumber());
        transferDto.setAmount(new BigDecimal("-100.00"));
        transferDto.setTransferType("IMPS");
        transferDto.setRemarks("Negative amount test");
        transferDto.setTransactionPin("1234");
        transferDto.setOtp("123456");

        Exception exception = assertThrows(RuntimeException.class, () -> {
            transactionService.transfer(transferDto);
        });
        assertTrue(exception.getMessage().contains("Transfer amount must be positive"));
    }

    @Test
    void testSelfTransferRejection() {
        Customer customer = customerRepository.findByUsername("customer1").orElse(null);
        assertNotNull(customer);
        Branch branch = branchRepository.findAll().get(0);
        Account sender = accountService.openAccount(customer, branch, Account.AccountType.SAVINGS, new BigDecimal("5000.00"));

        TransferDto transferDto = new TransferDto();
        transferDto.setFromAccount(sender.getAccountNumber());
        transferDto.setToAccount(sender.getAccountNumber());
        transferDto.setAmount(new BigDecimal("100.00"));
        transferDto.setTransferType("IMPS");
        transferDto.setRemarks("Self transfer test");
        transferDto.setTransactionPin("1234");
        transferDto.setOtp("123456");

        Exception exception = assertThrows(RuntimeException.class, () -> {
            transactionService.transfer(transferDto);
        });
        assertTrue(exception.getMessage().contains("Self-transfer is not allowed"));
    }
}
