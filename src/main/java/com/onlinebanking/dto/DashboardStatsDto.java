package com.onlinebanking.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsDto {
    private long totalCustomers;
    private long totalAccounts;
    private long totalTransactions;
    private long pendingLoans;
    private long totalEmployees;
    private long totalBranches;
    private long totalLoans;
    private BigDecimal totalDeposits;
    private long todayTransactions;
}
