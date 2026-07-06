package com.onlinebanking.service;

import com.onlinebanking.dto.DashboardStatsDto;
import com.onlinebanking.entity.*;
import com.onlinebanking.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final CustomerRepository customerRepo;
    private final AccountRepository  accountRepo;
    private final TransactionRepository txnRepo;
    private final LoanRepository     loanRepo;
    private final EmployeeRepository empRepo;
    private final BranchRepository   branchRepo;

    public DashboardStatsDto getStats() {
        BigDecimal totalDeposits = accountRepo.sumTotalDeposits();
        return DashboardStatsDto.builder()
            .totalCustomers(customerRepo.count())
            .totalAccounts(accountRepo.count())
            .totalTransactions(txnRepo.count())
            .pendingLoans(loanRepo.countByStatus(Loan.LoanStatus.PENDING))
            .totalEmployees(empRepo.count())
            .totalBranches(branchRepo.count())
            .totalLoans(loanRepo.count())
            .totalDeposits(totalDeposits != null ? totalDeposits : BigDecimal.ZERO)
            .todayTransactions(txnRepo.countTransactionsSince(
                LocalDate.now().atStartOfDay()))
            .build();
    }

    /** Returns labels (e.g. "Mon 23") and counts for the last 7 days. */
    public Map<String, Object> getTransactionChart() {
        List<String> labels = new ArrayList<>();
        List<Long>   data   = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day   = LocalDate.now().minusDays(i);
            LocalDateTime from = day.atStartOfDay();
            LocalDateTime to   = day.plusDays(1).atStartOfDay();
            long count = txnRepo.countTransactionsBetween(from, to);
            labels.add(day.getDayOfWeek().name().substring(0,3) + " " + day.getDayOfMonth());
            data.add(count);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("data",   data);
        return result;
    }

    /** Loan counts by status for the doughnut chart. */
    public Map<String, Long> getLoanStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("pending",  loanRepo.countByStatus(Loan.LoanStatus.PENDING));
        stats.put("approved", loanRepo.countByStatus(Loan.LoanStatus.APPROVED));
        stats.put("rejected", loanRepo.countByStatus(Loan.LoanStatus.REJECTED));
        stats.put("active",   loanRepo.countByStatus(Loan.LoanStatus.ACTIVE));
        return stats;
    }

    /** Pending KYC count for the notification bell. */
    public long getPendingKycCount() {
        return customerRepo.countByKycStatus(Customer.KycStatus.PENDING);
    }
}
