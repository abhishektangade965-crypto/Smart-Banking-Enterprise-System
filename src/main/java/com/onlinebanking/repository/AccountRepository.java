package com.onlinebanking.repository;

import com.onlinebanking.entity.Account;
import com.onlinebanking.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByAccountNumber(String accountNumber);
    List<Account> findByCustomer(Customer customer);
    List<Account> findByStatus(Account.AccountStatus status);
    @Query("SELECT SUM(a.balance) FROM Account a WHERE a.status = 'ACTIVE'")
    BigDecimal sumTotalDeposits();
    long countByStatus(Account.AccountStatus status);
}
