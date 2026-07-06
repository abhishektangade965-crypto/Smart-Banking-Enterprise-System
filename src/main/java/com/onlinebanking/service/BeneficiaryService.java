package com.onlinebanking.service;

import com.onlinebanking.entity.*;
import com.onlinebanking.repository.BeneficiaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BeneficiaryService {
    private final BeneficiaryRepository benefRepo;

    public Beneficiary add(Customer customer, String name, String accountNo, String ifsc, String bank, String nick) {
        if (benefRepo.existsByCustomerAndAccountNumber(customer, accountNo))
            throw new RuntimeException("Beneficiary already added");
        return benefRepo.save(Beneficiary.builder()
            .customer(customer).beneficiaryName(name)
            .accountNumber(accountNo).ifscCode(ifsc)
            .bankName(bank).nickname(nick).verified(false).build());
    }

    public void delete(Long id) { benefRepo.deleteById(id); }
    public void verify(Long id) {
        Beneficiary b = benefRepo.findById(id).orElseThrow();
        b.setVerified(true);
        benefRepo.save(b);
    }
    public List<Beneficiary> getByCustomer(Customer customer) { return benefRepo.findByCustomer(customer); }
    public Beneficiary getById(Long id) { return benefRepo.findById(id).orElseThrow(); }
}
