package com.onlinebanking.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDeniedException(AccessDeniedException e, RedirectAttributes ra) {
        log.warn("Access denied exception: {}", e.getMessage());
        ra.addFlashAttribute("error", "Access Denied: You do not have permission to perform this action.");
        return "redirect:/dashboard";
    }

    @ExceptionHandler(RuntimeException.class)
    public String handleRuntimeException(RuntimeException e, RedirectAttributes ra) {
        log.error("Runtime exception caught: ", e);
        ra.addFlashAttribute("error", e.getMessage() != null ? e.getMessage() : "A database or system error occurred.");
        return "redirect:/dashboard";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneralException(Exception e, RedirectAttributes ra) {
        log.error("Unhandled exception caught: ", e);
        ra.addFlashAttribute("error", "An unexpected system error occurred. Please contact support.");
        return "redirect:/dashboard";
    }
}
