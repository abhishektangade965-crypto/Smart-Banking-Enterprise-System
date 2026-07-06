package com.onlinebanking.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferDto {
    @NotBlank(message = "Sender account number is required")
    private String fromAccount;

    @NotBlank(message = "Receiver account number is required")
    private String toAccount;

    @NotNull(message = "Transfer amount is required")
    @DecimalMin(value = "1.00", message = "Transfer amount must be at least ₹1.00")
    private BigDecimal amount;

    private String transferType;
    private String remarks;
    private String upiId;

    @NotBlank(message = "Transaction PIN is required")
    @Size(min = 4, max = 4, message = "Transaction PIN must be exactly 4 digits")
    private String transactionPin;

    @NotBlank(message = "OTP is required")
    @Size(min = 6, max = 6, message = "OTP must be exactly 6 digits")
    private String otp;
}
