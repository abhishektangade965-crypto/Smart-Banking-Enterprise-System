package com.onlinebanking.security;

import com.onlinebanking.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticationEvents {
    private final LoginAttemptService loginAttemptService;
    private final AuditService auditService;

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent success) {
        Object principal = success.getAuthentication().getPrincipal();
        if (principal instanceof User user) {
            loginAttemptService.loginSucceeded(user.getUsername());
            auditService.log(user.getUsername(), "LOGIN_SUCCESS", "Successfully logged into the system");
        } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
            loginAttemptService.loginSucceeded(userDetails.getUsername());
            auditService.log(userDetails.getUsername(), "LOGIN_SUCCESS", "Successfully logged into the system");
        } else if (principal instanceof String username) {
            loginAttemptService.loginSucceeded(username);
            auditService.log(username, "LOGIN_SUCCESS", "Successfully logged into the system");
        }
    }

    @EventListener
    public void onFailure(AuthenticationFailureBadCredentialsEvent failure) {
        String username = failure.getAuthentication().getName();
        loginAttemptService.loginFailed(username);
        auditService.log(username, "LOGIN_FAILURE", "Failed login attempt (invalid credentials)");
    }
}
