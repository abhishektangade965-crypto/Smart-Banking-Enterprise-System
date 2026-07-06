package com.onlinebanking.repository;

import com.onlinebanking.entity.Beneficiary;
import com.onlinebanking.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {
    List<Beneficiary> findByCustomer(Customer customer);
    List<Beneficiary> findByCustomerAndVerified(Customer customer, boolean verified);
    boolean existsByCustomerAndAccountNumber(Customer customer, String accountNumber);
}
