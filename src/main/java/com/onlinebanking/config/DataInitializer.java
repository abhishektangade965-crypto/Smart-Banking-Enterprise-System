package com.onlinebanking.config;

import com.onlinebanking.entity.*;
import com.onlinebanking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final BranchRepository branchRepository;
    private final AccountRepository accountRepository;
    private final LoanRepository loanRepository;
    private final TransactionRepository transactionRepository;
    private final CardRepository cardRepository;
    private final PasswordEncoder encoder;

    @Override
    public void run(String... args) {
        Role adminRole = getOrCreateRole("ROLE_ADMIN");
        Role customerRole = getOrCreateRole("ROLE_CUSTOMER");
        Role employeeRole = getOrCreateRole("ROLE_EMPLOYEE");

        // Seed 5 Branches
        List<Branch> branches = new ArrayList<>();
        if (branchRepository.count() < 5) {
            String[] cities = {"Mumbai", "Delhi", "Bangalore", "Chennai", "Kolkata"};
            String[] states = {"Maharashtra", "Delhi", "Karnataka", "Tamil Nadu", "West Bengal"};
            for (int i = 0; i < 5; i++) {
                branches.add(branchRepository.save(Branch.builder()
                    .branchId("BR00" + (i + 1))
                    .branchName(cities[i] + " Hub")
                    .ifscCode("SBNK000" + (i + 1))
                    .address("Tech Tower " + (i + 1))
                    .city(cities[i])
                    .state(states[i])
                    .manager("Manager " + (i + 1))
                    .active(true).build()));
            }
            log.info("5 branches seeded");
        } else {
            branches = branchRepository.findAll();
        }

        Branch fallbackBranch = branches.get(0);
        Random rand = new Random();

        // Seed Admin user if missing
        if (!userRepository.existsByUsername("admin")) {
            User adminUser = userRepository.save(User.builder()
                .username("admin").email("admin@smartbank.com")
                .password(encoder.encode("Admin@123")).enabled(true)
                .accountNonLocked(true).roles(Set.of(adminRole)).build());
            employeeRepository.save(Employee.builder()
                .employeeId("EMP001").fullName("System Administrator")
                .email("admin@smartbank.com").mobileNumber("9000000001")
                .designation("Administrator").joiningDate(LocalDate.now())
                .branch(fallbackBranch).user(adminUser).active(true).build());
        }

        // Seed 200 Employees
        if (employeeRepository.count() < 200) {
            List<User> usersToSave = new ArrayList<>();
            List<Employee> employeesToSave = new ArrayList<>();
            for (int i = 2; i <= 201; i++) {
                String username = "employee" + i;
                if (!userRepository.existsByUsername(username)) {
                    User empUser = User.builder()
                        .username(username).email(username + "@smartbank.com")
                        .password(encoder.encode("Employee@123")).enabled(true)
                        .accountNonLocked(true).roles(Set.of(employeeRole)).build();
                    usersToSave.add(empUser);
                }
            }
            List<User> savedUsers = userRepository.saveAll(usersToSave);
            for (int i = 0; i < savedUsers.size(); i++) {
                int empIdx = i + 2;
                Branch b = branches.get(rand.nextInt(branches.size()));
                employeesToSave.add(Employee.builder()
                    .employeeId("EMP" + String.format("%03d", empIdx))
                    .fullName("Employee Officer " + empIdx)
                    .email(savedUsers.get(i).getEmail())
                    .mobileNumber("9100000" + String.format("%03d", empIdx))
                    .designation("Relationship Officer")
                    .joiningDate(LocalDate.now())
                    .branch(b)
                    .user(savedUsers.get(i))
                    .active(true).build());
            }
            employeeRepository.saveAll(employeesToSave);
            log.info("200 Employees seeded");
        }

        // Seed 200 Customers
        List<Customer> savedCustomers = new ArrayList<>();
        if (customerRepository.count() < 200) {
            List<User> usersToSave = new ArrayList<>();
            List<Customer> customersToSave = new ArrayList<>();
            for (int i = 1; i <= 200; i++) {
                String username = "customer" + i;
                if (!userRepository.existsByUsername(username)) {
                    User custUser = User.builder()
                        .username(username).email(username + "@smartbank.com")
                        .password(encoder.encode("Customer@123")).enabled(true)
                        .accountNonLocked(true).roles(Set.of(customerRole)).build();
                    usersToSave.add(custUser);
                }
            }
            List<User> savedUsers = userRepository.saveAll(usersToSave);
            for (int i = 0; i < savedUsers.size(); i++) {
                int custIdx = i + 1;
                customersToSave.add(Customer.builder()
                    .customerId("CUST" + String.format("%03d", custIdx))
                    .fullName("Customer Client " + custIdx)
                    .email(savedUsers.get(i).getEmail())
                    .mobileNumber("9800000" + String.format("%03d", custIdx))
                    .aadhaarNumber("12345678" + String.format("%04d", custIdx))
                    .panNumber("ABCDE" + String.format("%04d", custIdx) + "F")
                    .gender(Customer.Gender.MALE)
                    .dateOfBirth(LocalDate.of(1990, 10, 12))
                    .address("Block " + custIdx + " Tech Residency")
                    .city("Mumbai").state("Maharashtra").pincode("400001")
                    .kycStatus(Customer.KycStatus.VERIFIED)
                    .user(savedUsers.get(i)).build());
            }
            savedCustomers = customerRepository.saveAll(customersToSave);
            log.info("200 Customers seeded");
        } else {
            savedCustomers = customerRepository.findAll();
        }

        // Seed 200 Accounts
        List<Account> savedAccounts = new ArrayList<>();
        if (accountRepository.count() < 200) {
            List<Account> accountsToSave = new ArrayList<>();
            for (int i = 0; i < savedCustomers.size(); i++) {
                Customer c = savedCustomers.get(i);
                Branch b = branches.get(rand.nextInt(branches.size()));
                accountsToSave.add(Account.builder()
                    .accountNumber("10000000" + String.format("%08d", (i + 1)))
                    .customer(c)
                    .branch(b)
                    .accountType(Account.AccountType.SAVINGS)
                    .balance(new BigDecimal("75000.00"))
                    .openingDate(LocalDate.now())
                    .status(Account.AccountStatus.ACTIVE).build());
            }
            savedAccounts = accountRepository.saveAll(accountsToSave);
            log.info("200 Accounts seeded");
        } else {
            savedAccounts = accountRepository.findAll();
        }

        // Seed 100 Loans
        if (loanRepository.count() < 100) {
            List<Loan> loansToSave = new ArrayList<>();
            for (int i = 1; i <= 100; i++) {
                Customer c = savedCustomers.get(rand.nextInt(savedCustomers.size()));
                Loan.LoanStatus status;
                if (i % 4 == 0) status = Loan.LoanStatus.PENDING;
                else if (i % 4 == 1) status = Loan.LoanStatus.APPROVED;
                else if (i % 4 == 2) status = Loan.LoanStatus.REJECTED;
                else status = Loan.LoanStatus.ACTIVE;

                loansToSave.add(Loan.builder()
                    .loanId("LN" + String.format("%08d", i))
                    .customer(c)
                    .loanType(Loan.LoanType.PERSONAL)
                    .amount(new BigDecimal("120000.00"))
                    .interestRate(new BigDecimal("11.5"))
                    .emi(new BigDecimal("2800.00"))
                    .durationMonths(48)
                    .startDate(LocalDate.now())
                    .endDate(LocalDate.now().plusYears(4))
                    .purpose("Liquidity Support")
                    .status(status).build());
            }
            loanRepository.saveAll(loansToSave);
            log.info("100 Loans seeded");
        }

        // Seed 500 Historical Transactions
        if (transactionRepository.count() < 500 && savedAccounts.size() >= 2) {
            List<Transaction> txnsToSave = new ArrayList<>();
            for (int i = 1; i <= 500; i++) {
                Account from = savedAccounts.get(rand.nextInt(savedAccounts.size()));
                Account to = savedAccounts.get(rand.nextInt(savedAccounts.size()));
                while (from.getId().equals(to.getId())) {
                    to = savedAccounts.get(rand.nextInt(savedAccounts.size()));
                }
                txnsToSave.add(Transaction.builder()
                    .transactionId("TXN" + System.currentTimeMillis() + i)
                    .senderAccount(from)
                    .receiverAccount(to)
                    .amount(new BigDecimal("250.00"))
                    .transactionType(Transaction.TransactionType.TRANSFER)
                    .status(Transaction.TransactionStatus.SUCCESS)
                    .remarks("Simulated Ledger Transfer")
                    .transactionDate(LocalDateTime.now().minusHours(i % 120))
                    .senderBalanceAfter(from.getBalance())
                    .receiverBalanceAfter(to.getBalance()).build());
            }
            transactionRepository.saveAll(txnsToSave);
            log.info("500 historical transactions seeded");
        }

        // Seed 200 Cards for the seeded accounts
        if (cardRepository.count() < 200) {
            List<Card> cardsToSave = new ArrayList<>();
            for (int i = 0; i < savedAccounts.size(); i++) {
                Account acc = savedAccounts.get(i);
                cardsToSave.add(Card.builder()
                    .cardNumber("4532" + String.format("%012d", (i + 1L)))
                    .account(acc)
                    .cardType(Card.CardType.DEBIT)
                    .expiryDate(LocalDate.now().plusYears(5))
                    .status(Card.CardStatus.ACTIVE).build());
            }
            cardRepository.saveAll(cardsToSave);
            log.info("200 Cards seeded");
        }
    }

    private Role getOrCreateRole(String name) {
        return roleRepository.findByName(name).orElseGet(() ->
            roleRepository.save(new Role(null, name)));
    }
}
