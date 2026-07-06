package com.onlinebanking.util;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class IdGeneratorUtil {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generateCustomerId() {
        return "CUST" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) +
            String.format("%04d", RANDOM.nextInt(10000));
    }
    public static String generateAccountNumber() {
        long m = (long) (RANDOM.nextDouble() * 9_000_000_000_000_000L) + 1_000_000_000_000_000L;
        return String.format("%016d", m);
    }
    public static String generateTransactionId() {
        return "TXN" + LocalDateTime.now().format(FMT) + String.format("%04d", RANDOM.nextInt(10000));
    }
    public static String generateLoanId() {
        return "LN" + LocalDateTime.now().format(FMT);
    }
    public static String generateCardNumber() {
        long m = (long) (RANDOM.nextDouble() * 900_000_000_000_000L) + 100_000_000_000_000L;
        return String.format("4%015d", m);
    }
    public static String generateEmployeeId() {
        return "EMP" + String.format("%04d", RANDOM.nextInt(9000) + 1000);
    }
    public static String generateBranchId() {
        return "BR" + String.format("%03d", RANDOM.nextInt(999) + 1);
    }
    public static String generateCVV() {
        return String.format("%03d", RANDOM.nextInt(900) + 100);
    }
}
