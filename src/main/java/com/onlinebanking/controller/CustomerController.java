package com.onlinebanking.controller;

import com.onlinebanking.dto.*;
import com.onlinebanking.entity.*;
import com.onlinebanking.service.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/customer")
@PreAuthorize("hasAnyRole('ADMIN','CUSTOMER')")
@RequiredArgsConstructor
public class CustomerController {
    private final CustomerService customerService;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final LoanService loanService;
    private final CardService cardService;
    private final BeneficiaryService beneficiaryService;
    private final StatementExportService exportService;

    private Customer getCustomer(Authentication auth) {
        return customerService.getByUsername(auth.getName());
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        try {
            Customer c = getCustomer(auth);
            model.addAttribute("customer", c);
            model.addAttribute("accounts", accountService.getByCustomer(c));
        } catch (Exception e) {
            model.addAttribute("accounts", new ArrayList<>());
        }
        return "customer/dashboard";
    }

    @GetMapping("/accounts")
    public String accounts(Authentication auth, Model model) {
        try {
            Customer c = getCustomer(auth);
            model.addAttribute("accounts", accountService.getByCustomer(c));
            model.addAttribute("customer", c);
        } catch (Exception e) {
            model.addAttribute("accounts", new ArrayList<>());
            model.addAttribute("error", "Could not load accounts: " + e.getMessage());
        }
        return "customer/accounts";
    }

    @GetMapping("/transactions")
    public String transactions(@RequestParam(required = false) String accountNumber,
                               Authentication auth, Model model) {
        try {
            Customer c = getCustomer(auth);
            List<Account> accounts = accountService.getByCustomer(c);
            model.addAttribute("accounts", accounts);
            model.addAttribute("customer", c);
            if (!accounts.isEmpty()) {
                Account selected = accountNumber != null
                        ? accounts.stream().filter(a -> a.getAccountNumber().equals(accountNumber))
                                  .findFirst().orElse(accounts.get(0))
                        : accounts.get(0);
                model.addAttribute("selectedAccount", selected);
                model.addAttribute("transactions", transactionService.getByAccount(selected));
            } else {
                model.addAttribute("transactions", new ArrayList<>());
            }
        } catch (Exception e) {
            model.addAttribute("accounts", new ArrayList<>());
            model.addAttribute("transactions", new ArrayList<>());
            model.addAttribute("error", "Could not load transactions: " + e.getMessage());
        }
        return "customer/transactions";
    }

    @GetMapping("/transfer")
    public String transferForm(Authentication auth, Model model) {
        try {
            Customer c = getCustomer(auth);
            model.addAttribute("accounts", accountService.getByCustomer(c));
            model.addAttribute("beneficiaries", beneficiaryService.getByCustomer(c));
        } catch (Exception e) {
            model.addAttribute("accounts", new ArrayList<>());
            model.addAttribute("beneficiaries", new ArrayList<>());
        }
        model.addAttribute("transferDto", new TransferDto());
        return "customer/transfer";
    }

    @PostMapping("/transfer")
    public String doTransfer(@Valid @ModelAttribute("transferDto") TransferDto dto,
                             BindingResult result, Authentication auth, RedirectAttributes ra, Model model) {
        Customer c = getCustomer(auth);
        if (result.hasErrors()) {
            model.addAttribute("accounts", accountService.getByCustomer(c));
            model.addAttribute("beneficiaries", beneficiaryService.getByCustomer(c));
            model.addAttribute("transferDto", dto);
            return "customer/transfer";
        }
        try {
            // Verify fromAccount ownership to prevent IDOR
            List<Account> customerAccounts = accountService.getByCustomer(c);
            boolean ownsAccount = customerAccounts.stream()
                .anyMatch(acc -> acc.getAccountNumber().equals(dto.getFromAccount()));
            if (!ownsAccount) {
                throw new RuntimeException("Access Denied: You do not own the source account");
            }

            transactionService.transfer(dto);
            ra.addFlashAttribute("success", "Transfer successful! Transaction ID: " +
                "TXN" + System.currentTimeMillis());
            return "redirect:/customer/transfer";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/customer/transfer";
        }
    }

    @GetMapping("/loans")
    public String loans(Authentication auth, Model model) {
        try {
            Customer c = getCustomer(auth);
            model.addAttribute("loans", loanService.getByCustomer(c));
        } catch (Exception e) {
            model.addAttribute("loans", new ArrayList<>());
            model.addAttribute("error", "Could not load loans: " + e.getMessage());
        }
        model.addAttribute("loanDto", new LoanApplicationDto());
        return "customer/loans";
    }

    @PostMapping("/loans/apply")
    public String applyLoan(@Valid @ModelAttribute("loanDto") LoanApplicationDto dto,
                            BindingResult result, Authentication auth, RedirectAttributes ra, Model model) {
        if (result.hasErrors()) {
            Customer c = getCustomer(auth);
            model.addAttribute("loans", loanService.getByCustomer(c));
            model.addAttribute("loanDto", dto);
            return "customer/loans";
        }
        try {
            loanService.applyLoan(getCustomer(auth), dto);
            ra.addFlashAttribute("success", "Loan application submitted successfully!");
            return "redirect:/customer/loans";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to apply: " + e.getMessage());
            return "redirect:/customer/loans";
        }
    }

    @GetMapping("/cards")
    public String cards(Authentication auth, Model model) {
        try {
            Customer c = getCustomer(auth);
            List<Account> accounts = accountService.getByCustomer(c);
            model.addAttribute("accounts", accounts);
            model.addAttribute("customer", c);
            if (!accounts.isEmpty()) {
                model.addAttribute("cards", cardService.getByAccount(accounts.get(0)));
            } else {
                model.addAttribute("cards", new ArrayList<>());
            }
        } catch (Exception e) {
            model.addAttribute("accounts", new ArrayList<>());
            model.addAttribute("cards", new ArrayList<>());
            model.addAttribute("error", "Could not load cards: " + e.getMessage());
        }
        return "customer/cards";
    }

    @PostMapping("/cards/request")
    public String requestCard(@RequestParam Long accountId, @RequestParam String cardType,
                              RedirectAttributes ra) {
        try {
            Account account = accountService.getAllAccounts().stream()
                .filter(a -> a.getId().equals(accountId)).findFirst()
                .orElseThrow(() -> new RuntimeException("Account not found"));
            cardService.requestCard(account, Card.CardType.valueOf(cardType));
            ra.addFlashAttribute("success", "Card request submitted!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to request card: " + e.getMessage());
        }
        return "redirect:/customer/cards";
    }

    @GetMapping("/beneficiaries")
    public String beneficiaries(Authentication auth, Model model) {
        try {
            Customer c = getCustomer(auth);
            model.addAttribute("beneficiaries", beneficiaryService.getByCustomer(c));
        } catch (Exception e) {
            model.addAttribute("beneficiaries", new ArrayList<>());
            model.addAttribute("error", "Could not load beneficiaries: " + e.getMessage());
        }
        return "customer/beneficiaries";
    }

    @PostMapping("/beneficiaries/add")
    public String addBeneficiary(@RequestParam String name, @RequestParam String accountNo,
                                 @RequestParam String ifsc, @RequestParam String bank,
                                 @RequestParam String nick, Authentication auth, RedirectAttributes ra) {
        try {
            beneficiaryService.add(getCustomer(auth), name, accountNo, ifsc, bank, nick);
            ra.addFlashAttribute("success", "Beneficiary added!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to add beneficiary: " + e.getMessage());
        }
        return "redirect:/customer/beneficiaries";
    }

    @PostMapping("/beneficiaries/{id}/delete")
    public String deleteBeneficiary(@PathVariable Long id, RedirectAttributes ra) {
        try {
            beneficiaryService.delete(id);
            ra.addFlashAttribute("success", "Beneficiary removed");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to delete: " + e.getMessage());
        }
        return "redirect:/customer/beneficiaries";
    }

    // ─── Export: PDF Statement ────────────────────────────────────
    @GetMapping("/transactions/export/pdf")
    public void exportPdf(@RequestParam(required = false) String accountNumber,
                          Authentication auth, HttpServletResponse response) {
        try {
            Customer c = getCustomer(auth);
            List<Account> accounts = accountService.getByCustomer(c);
            if (accounts.isEmpty()) { response.sendError(404, "No accounts found"); return; }

            Account account = accountNumber != null
                    ? accounts.stream().filter(a -> a.getAccountNumber().equals(accountNumber))
                              .findFirst().orElse(accounts.get(0))
                    : accounts.get(0);

            List<Transaction> transactions = transactionService.getByAccount(account);
            byte[] pdf = exportService.generatePdf(c, account, transactions);

            String filename = "Statement_" + account.getAccountNumber() + "_"
                    + LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMMyyyy")) + ".pdf";
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            response.setContentLength(pdf.length);
            response.getOutputStream().write(pdf);
            response.getOutputStream().flush();
        } catch (Exception e) {
            try { response.sendError(500, "Failed to generate PDF: " + e.getMessage()); } catch (IOException ignored) {}
        }
    }

    // ─── Export: CSV Statement ────────────────────────────────────
    @GetMapping("/transactions/export/csv")
    public void exportCsv(@RequestParam(required = false) String accountNumber,
                          Authentication auth, HttpServletResponse response) {
        try {
            Customer c = getCustomer(auth);
            List<Account> accounts = accountService.getByCustomer(c);
            if (accounts.isEmpty()) { response.sendError(404, "No accounts found"); return; }

            Account account = accountNumber != null
                    ? accounts.stream().filter(a -> a.getAccountNumber().equals(accountNumber))
                              .findFirst().orElse(accounts.get(0))
                    : accounts.get(0);

            List<Transaction> transactions = transactionService.getByAccount(account);

            String filename = "Statement_" + account.getAccountNumber() + "_"
                    + LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMMyyyy")) + ".csv";
            response.setContentType("text/csv");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            exportService.writeCsv(c, account, transactions, response.getWriter());
        } catch (Exception e) {
            try { response.sendError(500, "Failed to generate CSV: " + e.getMessage()); } catch (IOException ignored) {}
        }
    }

    @GetMapping("/transactions/export/excel")
    public void exportExcel(@RequestParam(required = false) String accountNumber,
                            Authentication auth, HttpServletResponse response) {
        try {
            Customer c = getCustomer(auth);
            List<Account> accounts = accountService.getByCustomer(c);
            if (accounts.isEmpty()) { response.sendError(404, "No accounts found"); return; }

            Account account = accountNumber != null
                    ? accounts.stream().filter(a -> a.getAccountNumber().equals(accountNumber))
                              .findFirst().orElse(accounts.get(0))
                    : accounts.get(0);

            List<Transaction> transactions = transactionService.getByAccount(account);
            byte[] xlsx = exportService.generateExcel(c, account, transactions);

            String filename = "Statement_" + account.getAccountNumber() + "_"
                    + LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMMyyyy")) + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            response.setContentLength(xlsx.length);
            response.getOutputStream().write(xlsx);
            response.getOutputStream().flush();
        } catch (Exception e) {
            try { response.sendError(500, "Failed to generate Excel: " + e.getMessage()); } catch (IOException ignored) {}
        }
    }

    @GetMapping("/profile")
    public String profile(Authentication auth, Model model) {
        try {
            model.addAttribute("customer", getCustomer(auth));
        } catch (Exception e) {
            model.addAttribute("error", "Could not load profile: " + e.getMessage());
        }
        return "customer/profile";
    }
}
