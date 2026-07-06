package com.onlinebanking.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplicationDto {
    @NotBlank(message = "Loan type is required")
    private String loanType;

    @NotNull(message = "Loan amount is required")
    @DecimalMin(value = "5000.00", message = "Loan amount must be at least ₹5,000")
    private BigDecimal amount;

    @NotNull(message = "Duration in months is required")
    @Min(value = 6, message = "Minimum duration is 6 months")
    @Max(value = 360, message = "Maximum duration is 360 months (30 years)")
    private Integer durationMonths;

    @NotBlank(message = "Loan purpose is required")
    private String purpose;

    private String remarks;
}
