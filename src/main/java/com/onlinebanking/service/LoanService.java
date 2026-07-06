package com.onlinebanking.service;

import com.onlinebanking.dto.LoanApplicationDto;
import com.onlinebanking.entity.*;
import com.onlinebanking.repository.LoanRepository;
import com.onlinebanking.util.IdGeneratorUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class LoanService {
    private final LoanRepository loanRepo;

    private BigDecimal getInterestRate(Loan.LoanType type) {
        return switch (type) {
            case HOME       -> new BigDecimal("8.5");
            case CAR        -> new BigDecimal("9.0");
            case EDUCATION  -> new BigDecimal("7.5");
            case PERSONAL   -> new BigDecimal("12.0");
            case BUSINESS   -> new BigDecimal("11.0");
        };
    }

    private BigDecimal calculateEmi(BigDecimal principal, BigDecimal annualRate, int months) {
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        double pow = Math.pow(onePlusR.doubleValue(), months);
        BigDecimal numerator = principal.multiply(monthlyRate).multiply(BigDecimal.valueOf(pow));
        BigDecimal denominator = BigDecimal.valueOf(pow - 1);
        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    public Loan applyLoan(Customer customer, LoanApplicationDto dto) {
        Loan.LoanType type = Loan.LoanType.valueOf(dto.getLoanType());
        BigDecimal rate = getInterestRate(type);
        BigDecimal emi  = calculateEmi(dto.getAmount(), rate, dto.getDurationMonths());
        return loanRepo.save(Loan.builder()
            .loanId(IdGeneratorUtil.generateLoanId())
            .customer(customer).loanType(type)
            .amount(dto.getAmount()).interestRate(rate)
            .emi(emi).durationMonths(dto.getDurationMonths())
            .startDate(LocalDate.now())
            .endDate(LocalDate.now().plusMonths(dto.getDurationMonths()))
            .purpose(dto.getPurpose())
            .status(Loan.LoanStatus.PENDING).build());
    }

    public void approveLoan(Long id) {
        Loan l = loanRepo.findById(id).orElseThrow(() -> new RuntimeException("Loan not found"));
        l.setStatus(Loan.LoanStatus.APPROVED);
        loanRepo.save(l);
    }

    public void rejectLoan(Long id, String remarks) {
        Loan l = loanRepo.findById(id).orElseThrow(() -> new RuntimeException("Loan not found"));
        l.setStatus(Loan.LoanStatus.REJECTED);
        l.setRemarks(remarks);
        loanRepo.save(l);
    }

    public void activateLoan(Long id) {
        Loan l = loanRepo.findById(id).orElseThrow(() -> new RuntimeException("Loan not found"));
        if (l.getStatus() != Loan.LoanStatus.APPROVED)
            throw new RuntimeException("Loan must be approved before activation");
        l.setStatus(Loan.LoanStatus.ACTIVE);
        loanRepo.save(l);
    }

    public List<Loan> getByCustomer(Customer customer) { return loanRepo.findByCustomer(customer); }
    public List<Loan> getAll()     { return loanRepo.findAll(); }
    public List<Loan> getPending() { return loanRepo.findByStatus(Loan.LoanStatus.PENDING); }
    public Loan getById(Long id)   { return loanRepo.findById(id).orElseThrow(); }
}
