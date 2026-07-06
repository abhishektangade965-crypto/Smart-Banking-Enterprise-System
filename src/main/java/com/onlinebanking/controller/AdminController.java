package com.onlinebanking.controller;

import com.onlinebanking.dto.LoanApplicationDto;
import com.onlinebanking.dto.RegistrationDto;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.ResponseEntity;
import java.util.HashMap;
import java.util.Map;
import com.onlinebanking.entity.*;
import com.onlinebanking.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {
    private final CustomerService customerService;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final LoanService loanService;
    private final EmployeeService employeeService;
    private final BranchService branchService;
    private final CardService cardService;
    private final DashboardService dashboardService;
    private final AuthService authService;

    // ─── Customers ───────────────────────────────────────────────
    @GetMapping("/customers")
    public String customers(Model model, @RequestParam(required = false) String search) {
        model.addAttribute("customers",
            search != null && !search.isBlank() ? customerService.search(search) : customerService.getAllCustomers());
        return "admin/customers";
    }

    @PostMapping("/customers/add")
    public String addCustomer(@ModelAttribute RegistrationDto dto, RedirectAttributes ra) {
        try {
            // Basic validation
            if (dto.getFullName() == null || dto.getFullName().isBlank())
                throw new RuntimeException("Full name is required");
            if (dto.getEmail() == null || dto.getEmail().isBlank())
                throw new RuntimeException("Email is required");
            if (dto.getMobileNumber() == null || dto.getMobileNumber().isBlank())
                throw new RuntimeException("Mobile number is required");
            if (dto.getUsername() == null || dto.getUsername().isBlank())
                throw new RuntimeException("Username is required");
            if (dto.getPassword() == null || dto.getPassword().isBlank())
                throw new RuntimeException("Password is required");
            if (!dto.getPassword().equals(dto.getConfirmPassword()))
                throw new RuntimeException("Passwords do not match");

            authService.registerCustomer(dto);
            ra.addFlashAttribute("success",
                "Customer added successfully! Username: " + dto.getUsername());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to add customer: " + e.getMessage());
        }
        return "redirect:/admin/customers";
    }

    @PostMapping("/customers/{id}/kyc/{status}")
    public String updateKyc(@PathVariable Long id, @PathVariable String status, RedirectAttributes ra) {
        try {
            customerService.updateKycStatus(id, Customer.KycStatus.valueOf(status));
            ra.addFlashAttribute("success", "KYC status updated to " + status);
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to update KYC: " + e.getMessage());
        }
        return "redirect:/admin/customers";
    }

    @PostMapping("/customers/{id}/delete")
    public String deleteCustomer(@PathVariable Long id, RedirectAttributes ra) {
        try {
            customerService.delete(id);
            ra.addFlashAttribute("success", "Customer deleted successfully");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to delete customer: " + e.getMessage());
        }
        return "redirect:/admin/customers";
    }

    // ─── Accounts ────────────────────────────────────────────────
    @GetMapping("/accounts")
    public String accounts(Model model) {
        model.addAttribute("accounts", accountService.getAllAccounts());
        model.addAttribute("customers", customerService.getAllCustomers());
        model.addAttribute("branches", branchService.getAll());
        return "admin/accounts";
    }

    @PostMapping("/accounts/open")
    public String openAccount(@RequestParam Long customerId, @RequestParam Long branchId,
                              @RequestParam String accountType,
                              @RequestParam(defaultValue = "5000") java.math.BigDecimal initialDeposit,
                              RedirectAttributes ra) {
        try {
            Customer customer = customerService.getAllCustomers().stream()
                .filter(c -> c.getId().equals(customerId)).findFirst()
                .orElseThrow(() -> new RuntimeException("Customer not found"));
            Branch branch = branchService.getById(branchId);
            accountService.openAccount(customer, branch, Account.AccountType.valueOf(accountType), initialDeposit);
            ra.addFlashAttribute("success", "Account opened successfully with initial deposit of ₹" + String.format("%,.2f", initialDeposit));
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to open account: " + e.getMessage());
        }
        return "redirect:/admin/accounts";
    }

    @PostMapping("/accounts/fund")
    public String fundAccount(@RequestParam String accountNumber,
                              @RequestParam String operation,
                              @RequestParam java.math.BigDecimal amount,
                              @RequestParam(required = false) String remarks,
                              RedirectAttributes ra) {
        try {
            com.onlinebanking.entity.Account account = accountService.getByAccountNumber(accountNumber);
            if ("DEPOSIT".equals(operation)) {
                transactionService.deposit(account, amount, remarks != null ? remarks : "Admin deposit");
                ra.addFlashAttribute("success", String.format("Deposited ₹%,.2f into account %s successfully.", amount, accountNumber));
            } else if ("WITHDRAW".equals(operation)) {
                transactionService.withdraw(account, amount, remarks != null ? remarks : "Admin withdrawal");
                ra.addFlashAttribute("success", String.format("Withdrew ₹%,.2f from account %s successfully.", amount, accountNumber));
            } else {
                throw new RuntimeException("Invalid operation: " + operation);
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Transaction failed: " + e.getMessage());
        }
        return "redirect:/admin/accounts";
    }

    @PostMapping("/accounts/{id}/status/{status}")
    public String updateAccountStatus(@PathVariable Long id, @PathVariable String status, RedirectAttributes ra) {
        try {
            accountService.updateStatus(id, Account.AccountStatus.valueOf(status));
            ra.addFlashAttribute("success", "Account status updated");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to update account status: " + e.getMessage());
        }
        return "redirect:/admin/accounts";
    }

    // ─── Transactions ────────────────────────────────────────────
    @GetMapping("/transactions")
    public String transactions(Model model) {
        model.addAttribute("transactions", transactionService.getAll());
        return "admin/transactions";
    }

    // ─── Loans ───────────────────────────────────────────────────
    @GetMapping("/loans")
    public String loans(Model model) {
        java.util.List<com.onlinebanking.entity.Loan> allLoans = loanService.getAll();
        model.addAttribute("loans", allLoans);
        model.addAttribute("customers", customerService.getAllCustomers());
        // Pre-compute counts so Thymeleaf doesn't need Java stream syntax
        model.addAttribute("loanCountPending",  allLoans.stream().filter(l -> l.getStatus() == com.onlinebanking.entity.Loan.LoanStatus.PENDING).count());
        model.addAttribute("loanCountApproved", allLoans.stream().filter(l -> l.getStatus() == com.onlinebanking.entity.Loan.LoanStatus.APPROVED).count());
        model.addAttribute("loanCountActive",   allLoans.stream().filter(l -> l.getStatus() == com.onlinebanking.entity.Loan.LoanStatus.ACTIVE).count());
        model.addAttribute("loanCountRejected", allLoans.stream().filter(l -> l.getStatus() == com.onlinebanking.entity.Loan.LoanStatus.REJECTED).count());
        return "admin/loans";
    }

    @PostMapping("/loans/apply")
    public String applyLoan(@RequestParam Long customerId,
                            @RequestParam String loanType,
                            @RequestParam java.math.BigDecimal amount,
                            @RequestParam Integer durationMonths,
                            @RequestParam(required = false) String purpose,
                            RedirectAttributes ra) {
        try {
            Customer customer = customerService.getAllCustomers().stream()
                .filter(c -> c.getId().equals(customerId)).findFirst()
                .orElseThrow(() -> new RuntimeException("Customer not found"));
            LoanApplicationDto dto = new LoanApplicationDto(loanType, amount, durationMonths, purpose, null);
            loanService.applyLoan(customer, dto);
            ra.addFlashAttribute("success", "Loan application submitted successfully");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to apply loan: " + e.getMessage());
        }
        return "redirect:/admin/loans";
    }

    @PostMapping("/loans/{id}/approve")
    public String approveLoan(@PathVariable Long id, RedirectAttributes ra) {
        try {
            loanService.approveLoan(id);
            ra.addFlashAttribute("success", "Loan approved");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/loans";
    }

    @PostMapping("/loans/{id}/reject")
    public String rejectLoan(@PathVariable Long id, RedirectAttributes ra) {
        try {
            loanService.rejectLoan(id, "Rejected by admin");
            ra.addFlashAttribute("success", "Loan rejected");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/loans";
    }

    @PostMapping("/loans/{id}/activate")
    public String activateLoan(@PathVariable Long id, RedirectAttributes ra) {
        try {
            loanService.activateLoan(id);
            ra.addFlashAttribute("success", "Loan activated and funds disbursed");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/loans";
    }

    // ─── Employees ───────────────────────────────────────────────
    @GetMapping("/employees")
    public String employees(Model model) {
        model.addAttribute("employees", employeeService.getAll());
        model.addAttribute("branches", branchService.getAll());
        return "admin/employees";
    }

    @PostMapping("/employees/add")
    public String addEmployee(@RequestParam String fullName, @RequestParam String email,
                              @RequestParam String mobile, @RequestParam String designation,
                              @RequestParam(required = false) Long branchId, RedirectAttributes ra) {
        try {
            employeeService.addEmployee(fullName, email, mobile, designation, branchId);
            ra.addFlashAttribute("success", "Employee added successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to add employee: " + e.getMessage());
        }
        return "redirect:/admin/employees";
    }

    @PostMapping("/employees/{id}/delete")
    public String deleteEmployee(@PathVariable Long id, RedirectAttributes ra) {
        try {
            employeeService.delete(id);
            ra.addFlashAttribute("success", "Employee deleted");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to delete employee: " + e.getMessage());
        }
        return "redirect:/admin/employees";
    }

    // ─── Branches ────────────────────────────────────────────────
    @GetMapping("/branches")
    public String branches(Model model) {
        model.addAttribute("branches", branchService.getAll());
        return "admin/branches";
    }

    @PostMapping("/branches/add")
    public String addBranch(@RequestParam String name, @RequestParam String ifsc,
                            @RequestParam String address, @RequestParam String city,
                            @RequestParam String state, @RequestParam String manager, RedirectAttributes ra) {
        try {
            branchService.add(name, ifsc, address, city, state, manager);
            ra.addFlashAttribute("success", "Branch added successfully");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to add branch: " + e.getMessage());
        }
        return "redirect:/admin/branches";
    }

    @PostMapping("/branches/{id}/delete")
    public String deleteBranch(@PathVariable Long id, RedirectAttributes ra) {
        try {
            branchService.delete(id);
            ra.addFlashAttribute("success", "Branch deleted");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to delete branch: " + e.getMessage());
        }
        return "redirect:/admin/branches";
    }

    // ─── Cards ───────────────────────────────────────────────────
    @GetMapping("/cards")
    public String cards(Model model) {
        model.addAttribute("cards", cardService.getAll());
        return "admin/cards";
    }

    @PostMapping("/cards/{id}/block")
    public String blockCard(@PathVariable Long id, RedirectAttributes ra) {
        try {
            cardService.blockCard(id);
            ra.addFlashAttribute("success", "Card blocked");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/cards";
    }

    @PostMapping("/cards/{id}/unblock")
    public String unblockCard(@PathVariable Long id, RedirectAttributes ra) {
        try {
            cardService.unblockCard(id);
            ra.addFlashAttribute("success", "Card unblocked");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/cards";
    }
}
