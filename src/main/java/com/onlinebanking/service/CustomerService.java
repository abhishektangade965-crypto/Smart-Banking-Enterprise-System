package com.onlinebanking.service;

import com.onlinebanking.entity.Customer;
import com.onlinebanking.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerService {
    private final CustomerRepository customerRepo;

    public List<Customer> getAllCustomers() { return customerRepo.findAll(); }
    public Customer getByCustomerId(String id) {
        return customerRepo.findByCustomerId(id).orElseThrow(() -> new RuntimeException("Customer not found"));
    }
    public Customer getByEmail(String email) {
        return customerRepo.findByEmail(email).orElseThrow(() -> new RuntimeException("Customer not found"));
    }
    public Customer getByUserId(Long userId) {
        return customerRepo.findByUser_Id(userId).orElseThrow(() -> new RuntimeException("Customer not found"));
    }
    public Customer getByUsername(String username) {
        return customerRepo.findByUsername(username).orElseThrow(() -> new RuntimeException("Customer not found for user: " + username));
    }
    public Customer save(Customer customer) { return customerRepo.save(customer); }
    public void delete(Long id) { customerRepo.deleteById(id); }
    public List<Customer> search(String name) { return customerRepo.findByFullNameContainingIgnoreCase(name); }
    public void updateKycStatus(Long id, Customer.KycStatus status) {
        Customer c = customerRepo.findById(id).orElseThrow();
        c.setKycStatus(status);
        customerRepo.save(c);
    }
}
