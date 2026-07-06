package com.onlinebanking.repository;

import com.onlinebanking.entity.Customer;
import com.onlinebanking.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    Optional<Loan> findByLoanId(String loanId);
    List<Loan> findByCustomer(Customer customer);
    List<Loan> findByStatus(Loan.LoanStatus status);
    long countByStatus(Loan.LoanStatus status);
}
