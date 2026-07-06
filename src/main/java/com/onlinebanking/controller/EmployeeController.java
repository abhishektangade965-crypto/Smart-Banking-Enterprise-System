package com.onlinebanking.controller;

import com.onlinebanking.entity.*;
import com.onlinebanking.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/employee")
@PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
@RequiredArgsConstructor
public class EmployeeController {
    private final CustomerService customerService;
    private final LoanService loanService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        try {
            model.addAttribute("pendingLoans", loanService.getPending().size());
            model.addAttribute("pendingKyc", customerService.getAllCustomers().stream()
                .filter(c -> c.getKycStatus() == Customer.KycStatus.PENDING).count());
        } catch (Exception e) {
            model.addAttribute("pendingLoans", 0);
            model.addAttribute("pendingKyc", 0);
        }
        return "employee/dashboard";
    }

    @GetMapping("/kyc")
    public String kyc(Model model) {
        try {
            model.addAttribute("customers", customerService.getAllCustomers());
        } catch (Exception e) {
            model.addAttribute("customers", new ArrayList<>());
            model.addAttribute("error", "Could not load customers: " + e.getMessage());
        }
        return "employee/kyc";
    }

    @PostMapping("/kyc/{id}/verify")
    public String verifyKyc(@PathVariable Long id, RedirectAttributes ra) {
        try {
            customerService.updateKycStatus(id, Customer.KycStatus.VERIFIED);
            ra.addFlashAttribute("success", "KYC verified successfully");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to verify KYC: " + e.getMessage());
        }
        return "redirect:/employee/kyc";
    }

    @PostMapping("/kyc/{id}/reject")
    public String rejectKyc(@PathVariable Long id, RedirectAttributes ra) {
        try {
            customerService.updateKycStatus(id, Customer.KycStatus.REJECTED);
            ra.addFlashAttribute("success", "KYC rejected");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to reject KYC: " + e.getMessage());
        }
        return "redirect:/employee/kyc";
    }

    // ─── Loans ────────────────────────────────────────────────────

    @GetMapping("/loans")
    public String loans(Model model) {
        try {
            List<Loan> pending = loanService.getPending();
            List<Loan> allLoans = loanService.getAll();

            // Count approved/rejected today for the stats cards
            LocalDate today = LocalDate.now();
            long approvedToday = allLoans.stream()
                .filter(l -> l.getStatus() == Loan.LoanStatus.APPROVED
                          && l.getUpdatedAt() != null
                          && l.getUpdatedAt().toLocalDate().equals(today))
                .count();
            long rejectedToday = allLoans.stream()
                .filter(l -> l.getStatus() == Loan.LoanStatus.REJECTED
                          && l.getUpdatedAt() != null
                          && l.getUpdatedAt().toLocalDate().equals(today))
                .count();

            model.addAttribute("loans", pending);
            model.addAttribute("allLoans", allLoans);
            model.addAttribute("pendingCount", pending.size());
            model.addAttribute("approvedCount", approvedToday);
            model.addAttribute("rejectedCount", rejectedToday);
        } catch (Exception e) {
            model.addAttribute("loans", new ArrayList<>());
            model.addAttribute("allLoans", new ArrayList<>());
            model.addAttribute("pendingCount", 0);
            model.addAttribute("approvedCount", 0);
            model.addAttribute("rejectedCount", 0);
            model.addAttribute("error", "Could not load loans: " + e.getMessage());
        }
        return "employee/loans";
    }

    /** Employee approves a pending loan. */
    @PostMapping("/loans/{id}/approve")
    public String approveLoan(@PathVariable Long id, RedirectAttributes ra) {
        try {
            loanService.approveLoan(id);
            ra.addFlashAttribute("success", "Loan approved successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to approve loan: " + e.getMessage());
        }
        return "redirect:/employee/loans";
    }

    /** Employee rejects a pending loan with a reason. */
    @PostMapping("/loans/reject")
    public String rejectLoan(@RequestParam Long loanId,
                             @RequestParam String remarks,
                             @RequestParam(required = false) String additionalRemarks,
                             RedirectAttributes ra) {
        try {
            String fullRemarks = remarks;
            if (additionalRemarks != null && !additionalRemarks.isBlank()) {
                fullRemarks += " — " + additionalRemarks.trim();
            }
            loanService.rejectLoan(loanId, fullRemarks);
            ra.addFlashAttribute("success", "Loan application rejected.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to reject loan: " + e.getMessage());
        }
        return "redirect:/employee/loans";
    }
}
