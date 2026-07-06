package com.onlinebanking.controller;

import com.onlinebanking.dto.RegistrationDto;
import com.onlinebanking.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String logout,
                        jakarta.servlet.http.HttpServletRequest request, Model model) {
        if (error != null) {
            String errorMessage = "Invalid username or password. Please try again.";
            Object exception = request.getSession().getAttribute("SPRING_SECURITY_LAST_EXCEPTION");
            if (exception instanceof org.springframework.security.authentication.LockedException) {
                errorMessage = "This account has been locked out due to 5 failed attempts. Please contact support.";
            }
            model.addAttribute("error", errorMessage);
        }
        if (logout != null) model.addAttribute("message", "Logged out successfully.");
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("registration", new RegistrationDto());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registration") RegistrationDto dto,
                           BindingResult result, RedirectAttributes ra, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("registration", dto);
            return "auth/register";
        }
        try {
            authService.registerCustomer(dto);
            ra.addFlashAttribute("success", "Registration successful! Please login.");
            return "redirect:/auth/login";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("registration", dto);
            return "auth/register";
        }
    }

    @GetMapping("/forgot-password")
    public String forgotPassword() { return "auth/forgot-password"; }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String email, Model model) {
        try {
            String token = authService.generatePasswordResetToken(email);
            // Simulate sending email by printing reset link to server log
            System.out.println("=================================================");
            System.out.println("PASSWORD RESET LINK: http://localhost:8080/auth/reset-password?token=" + token);
            System.out.println("=================================================");
            model.addAttribute("success", "A password reset link has been printed to the system console logs.");
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }
        return "auth/forgot-password";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam String token, Model model) {
        model.addAttribute("token", token);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam String token,
                                       @RequestParam String password,
                                       @RequestParam String confirmPassword,
                                       RedirectAttributes ra, Model model) {
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match");
            model.addAttribute("token", token);
            return "auth/reset-password";
        }
        try {
            authService.resetPassword(token, password);
            ra.addFlashAttribute("success", "Your password has been reset successfully. Please login.");
            return "redirect:/auth/login";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("token", token);
            return "auth/reset-password";
        }
    }
}
