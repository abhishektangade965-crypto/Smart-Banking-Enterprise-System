package com.onlinebanking.service;

import com.onlinebanking.entity.AuditLog;
import com.onlinebanking.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository auditLogRepo;

    public void log(String username, String action, String details) {
        String ipAddress = "0.0.0.0";
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String xfHeader = request.getHeader("X-Forwarded-For");
            if (xfHeader == null) {
                ipAddress = request.getRemoteAddr();
            } else {
                ipAddress = xfHeader.split(",")[0];
            }
        }
        
        AuditLog logEntry = AuditLog.builder()
                .username(username != null ? username : "SYSTEM")
                .action(action)
                .details(details)
                .ipAddress(ipAddress)
                .build();
        auditLogRepo.save(logEntry);
    }
}
