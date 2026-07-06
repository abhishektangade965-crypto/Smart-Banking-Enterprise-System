package com.onlinebanking.service;

import com.onlinebanking.dto.RegistrationDto;
import com.onlinebanking.entity.*;
import com.onlinebanking.repository.*;
import com.onlinebanking.util.IdGeneratorUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final CustomerRepository customerRepo;
    private final PasswordEncoder encoder;

    public void registerCustomer(RegistrationDto dto) {
        if (dto.getUsername() == null || dto.getUsername().isBlank())
            throw new RuntimeException("Username is required");
        if (dto.getEmail() == null || dto.getEmail().isBlank())
            throw new RuntimeException("Email is required");
        if (dto.getPassword() == null || dto.getPassword().isBlank())
            throw new RuntimeException("Password is required");
        if (dto.getFullName() == null || dto.getFullName().isBlank())
            throw new RuntimeException("Full name is required");
        if (dto.getMobileNumber() == null || dto.getMobileNumber().isBlank())
            throw new RuntimeException("Mobile number is required");

        // Only check confirm password if it is provided
        if (dto.getConfirmPassword() != null && !dto.getConfirmPassword().isBlank()
                && !dto.getPassword().equals(dto.getConfirmPassword()))
            throw new RuntimeException("Passwords do not match");

        if (userRepo.existsByUsername(dto.getUsername()))
            throw new RuntimeException("Username already exists");
        if (userRepo.existsByEmail(dto.getEmail()))
            throw new RuntimeException("Email already registered");

        Role role = roleRepo.findByName("ROLE_CUSTOMER")
            .orElseThrow(() -> new RuntimeException("Role ROLE_CUSTOMER not found in database"));

        User user = userRepo.save(User.builder()
            .username(dto.getUsername()).email(dto.getEmail())
            .password(encoder.encode(dto.getPassword()))
            .enabled(true).accountNonLocked(true)
            .roles(Set.of(role)).build());

        Customer customer = Customer.builder()
            .customerId(IdGeneratorUtil.generateCustomerId())
            .fullName(dto.getFullName()).email(dto.getEmail())
            .mobileNumber(dto.getMobileNumber())
            .aadhaarNumber(dto.getAadhaarNumber())
            .panNumber(dto.getPanNumber())
            .gender(dto.getGender() != null && !dto.getGender().isBlank()
                ? Customer.Gender.valueOf(dto.getGender().toUpperCase()) : null)
            .address(dto.getAddress()).city(dto.getCity())
            .state(dto.getState()).pincode(dto.getPincode())
            .kycStatus(Customer.KycStatus.PENDING)
            .user(user).build();

        if (dto.getDateOfBirth() != null && !dto.getDateOfBirth().isBlank()) {
            try {
                customer.setDateOfBirth(LocalDate.parse(dto.getDateOfBirth()));
            } catch (Exception ignored) {}
        }

        customerRepo.save(customer);
    }

    public String generatePasswordResetToken(String email) {
        User user = userRepo.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Email address not registered"));
        String token = UUID.randomUUID().toString();
        user.setPasswordResetToken(token);
        user.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(1));
        userRepo.save(user);
        return token;
    }

    public void resetPassword(String token, String newPassword) {
        User user = userRepo.findByPasswordResetToken(token)
            .orElseThrow(() -> new RuntimeException("Invalid or expired password reset token"));
        if (user.getPasswordResetTokenExpiry() == null || user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Password reset token has expired");
        }
        user.setPassword(encoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        userRepo.save(user);
    }
}
