package com.onlinebanking.repository;

import com.onlinebanking.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByCustomerId(String customerId);
    Optional<Customer> findByEmail(String email);
    Optional<Customer> findByUser_Id(Long userId);

    @Query("SELECT c FROM Customer c JOIN FETCH c.user u WHERE u.username = :username")
    Optional<Customer> findByUsername(@Param("username") String username);

    List<Customer> findByFullNameContainingIgnoreCase(String name);
    List<Customer> findByKycStatus(Customer.KycStatus kycStatus);
    @Query("SELECT COUNT(c) FROM Customer c WHERE c.kycStatus = 'VERIFIED'")
    long countVerifiedCustomers();

    long countByKycStatus(Customer.KycStatus kycStatus);
}
