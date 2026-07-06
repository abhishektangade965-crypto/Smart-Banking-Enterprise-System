package com.onlinebanking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
public class SmartBankApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartBankApplication.class, args);
    }
}
