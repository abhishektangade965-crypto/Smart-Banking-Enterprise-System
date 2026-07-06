package com.onlinebanking.config;

import com.onlinebanking.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final CustomUserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authProvider() {
        var provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSP, FrameOptions and HSTS headers setup
            .headers(headers -> headers
                .frameOptions(frame -> frame.disable()) // Enabled for H2 console frame compatibility
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.jsdelivr.net https://cdnjs.cloudflare.com; style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://cdnjs.cloudflare.com https://fonts.googleapis.com; font-src 'self' https://cdnjs.cloudflare.com https://fonts.gstatic.com; img-src 'self' data:; connect-src 'self';"))
            )
            // Enforce CSRF; ignore API and Swagger/H2 console paths
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/h2-console/**", "/swagger-ui/**", "/v3/api-docs/**"))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/features", "/services", "/about", "/contact", "/faq", "/login", "/register",
                                 "/auth/**", "/css/**", "/js/**", "/images/**",
                                 "/webjars/**", "/error", "/h2-console/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/employee/**").hasAnyRole("ADMIN", "EMPLOYEE")
                .requestMatchers("/customer/**").hasAnyRole("ADMIN", "CUSTOMER")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/auth/login")
                .loginProcessingUrl("/auth/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/auth/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/auth/logout")
                .logoutSuccessUrl("/auth/login?logout=true")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID", "remember-me")
                .permitAll()
            )
            .rememberMe(rem -> rem.key("smartbank-remember-me").tokenValiditySeconds(604800))
            // Session Hijacking Protection & Concurrent Sessions Setup
            .sessionManagement(session -> session
                .sessionFixation(fixation -> fixation.newSession())
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false) // Replaces old session on new login instead of locking out
            );
        return http.build();
    }
}
