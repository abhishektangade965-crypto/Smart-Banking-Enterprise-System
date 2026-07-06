package com.onlinebanking.controller;

import com.onlinebanking.entity.*;
import com.onlinebanking.service.*;
import com.onlinebanking.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;
    private final CustomerService  customerService;
    private final AccountService   accountService;
    private final TransactionRepository txnRepo;
    private final com.onlinebanking.repository.CustomerRepository customerRepo;

    private boolean isLoggedIn() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated()
            && !(auth.getPrincipal() instanceof String s && s.equals("anonymousUser"));
    }

    @GetMapping("/")
    public String root(Model model, jakarta.servlet.http.HttpServletRequest request) {
        request.getSession(true);
        return root(model);
    }

    public String root(Model model) {
        if (isLoggedIn()) {
            model.addAttribute("isLoggedIn", true);
        }
        model.addAttribute("totalTransacted", txnRepo.sumAllTransactionAmounts());
        model.addAttribute("totalCustomers", customerRepo.count());
        return "landing";
    }

    @GetMapping("/features")
    public String features(Model model, jakarta.servlet.http.HttpServletRequest request) {
        request.getSession(true);
        model.addAttribute("activeSection", "features");
        return root(model);
    }

    @GetMapping("/services")
    public String services(Model model, jakarta.servlet.http.HttpServletRequest request) {
        request.getSession(true);
        model.addAttribute("activeSection", "services");
        return root(model);
    }

    @GetMapping("/about")
    public String about(Model model, jakarta.servlet.http.HttpServletRequest request) {
        request.getSession(true);
        model.addAttribute("activeSection", "about");
        return root(model);
    }

    @GetMapping("/contact")
    public String contact(Model model, jakarta.servlet.http.HttpServletRequest request) {
        request.getSession(true);
        model.addAttribute("activeSection", "contact");
        return root(model);
    }

    @GetMapping("/faq")
    public String faq(Model model, jakarta.servlet.http.HttpServletRequest request) {
        request.getSession(true);
        model.addAttribute("activeSection", "faq");
        return root(model);
    }

    @GetMapping("/login")
    public String publicLogin() {
        return isLoggedIn() ? "redirect:/dashboard" : "redirect:/auth/login";
    }

    @GetMapping("/register")
    public String publicRegister() {
        return isLoggedIn() ? "redirect:/dashboard" : "redirect:/auth/register";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        if (!isLoggedIn()) return "redirect:/auth/login";

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin    = auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        boolean isEmployee = auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));
        boolean isCustomer = auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_CUSTOMER"));

        if (isAdmin) {
            try {
                model.addAttribute("stats", dashboardService.getStats());
                // Chart data
                Map<String,Object> chart = dashboardService.getTransactionChart();
                model.addAttribute("txnChartLabels", chart.get("labels"));
                model.addAttribute("txnChartData",   chart.get("data"));
                // Loan doughnut
                model.addAttribute("loanStats", dashboardService.getLoanStats());
                // Recent transactions
                model.addAttribute("recentTransactions", txnRepo.findTop10ByOrderByTransactionDateDesc());
                // Pending KYC for stat card
                model.addAttribute("pendingKyc", dashboardService.getPendingKycCount());
            } catch (Exception e) {
                model.addAttribute("stats", null);
            }
            return "admin/dashboard";
        } else if (isEmployee) {
            try {
                model.addAttribute("pendingLoans", 0);
                model.addAttribute("pendingKyc", 0);
            } catch (Exception ignored) {}
            return "employee/dashboard";
        } else if (isCustomer) {
            try {
                Customer customer = customerService.getByUsername(auth.getName());
                model.addAttribute("customer", customer);
                model.addAttribute("accounts", accountService.getByCustomer(customer));
            } catch (Exception e) {
                model.addAttribute("accounts", new ArrayList<>());
            }
            return "customer/dashboard";
        }
        return "redirect:/auth/login";
    }

    @org.springframework.web.bind.annotation.PostMapping("/contact")
    public String submitContact(RedirectAttributes ra) {
        ra.addFlashAttribute("success", "Thank you for reaching out! Our team will contact you shortly.");
        return "redirect:/contact";
    }

    /** JSON endpoint polled by the notification bell every 30 seconds. */
    @GetMapping("/admin/notifications/count")
    @ResponseBody
    public ResponseEntity<Map<String,Long>> notificationCount() {
        Map<String,Long> result = new HashMap<>();
        try {
            result.put("pendingLoans", dashboardService.getStats().getPendingLoans());
            result.put("pendingKyc",   dashboardService.getPendingKycCount());
        } catch (Exception e) {
            result.put("pendingLoans", 0L);
            result.put("pendingKyc",   0L);
        }
        return ResponseEntity.ok(result);
    }
}
