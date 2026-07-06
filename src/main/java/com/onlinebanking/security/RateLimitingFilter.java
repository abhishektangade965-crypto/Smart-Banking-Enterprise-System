package com.onlinebanking.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitingFilter implements Filter {
    private final ConcurrentHashMap<String, RequestCounter> limitMap = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS_PER_MINUTE = 100; // Limit to 100 requests per minute per IP

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            String ip = httpRequest.getRemoteAddr();
            long currentTime = System.currentTimeMillis();

            RequestCounter counter = limitMap.compute(ip, (k, v) -> {
                if (v == null || (currentTime - v.windowStart) > 60000) {
                    return new RequestCounter(currentTime, 1);
                } else {
                    v.count.incrementAndGet();
                    return v;
                }
            });

            if (counter.count.get() > MAX_REQUESTS_PER_MINUTE) {
                httpResponse.setStatus(429); // Too Many Requests
                httpResponse.setContentType("text/plain");
                httpResponse.getWriter().write("Too Many Requests. Rate limit exceeded.");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private static class RequestCounter {
        final long windowStart;
        final AtomicInteger count;

        RequestCounter(long windowStart, int initialCount) {
            this.windowStart = windowStart;
            this.count = new AtomicInteger(initialCount);
        }
    }
}
