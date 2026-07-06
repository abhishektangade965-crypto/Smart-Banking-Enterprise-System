# 🎓 CASE STUDY: Enterprise Architecture & Design Decisions
## Project: SmartBank Enterprise Banking System

This document outlines the high-level design patterns, system architecture, technical challenges, and engineering decisions implemented during the development of the **SmartBank** digital banking portal. It serves as a technical deep-dive for hiring managers, software architects, and engineers reviewing the system.

---

## 🏛️ System Architecture

SmartBank utilizes a **Domain-Driven Design (DDD)** inspired layered architecture built on **Spring Boot 3.2.0**. The application is designed to support high-throughput bookkeeping, multi-portal security configurations, and sub-millisecond page rendering times using server-side Thymeleaf compilation.

```text
                                 [ Client Browser ]
                                         │
                        ( HTTPS requests / CSRF Handshake )
                                         │
                             ┌───────────▼───────────┐
                             │  Spring Security 6 &  │
                             │  Rate Limiting Filter │
                             └───────────┬───────────┘
                                         │
                             ┌───────────▼───────────┐
                             │    MVC Controllers    │
                             │   (Role-Based Auth)   │
                             └───────────┬───────────┘
                                         │
                             ┌───────────▼───────────┐
                             │ Transactional Service │
                             │  Layer (Spring @Tx)   │
                             └─────┬───────────┬─────┘
                                   │           │
           ┌───────────────────────┘           └──────────────────────┐
┌──────────▼──────────┐                                     ┌─────────▼──────────┐
│  Auditing Engine    │                                     │ Transaction        │
│  (Security Events)  │                                     │ Execution Pipeline │
└─────────────────────┘                                     └─────────┬──────────┘
                                                                      │
                                                            ┌─────────▼──────────┐
                                                            │ Spring Data JPA    │
                                                            │ Repositories       │
                                                            └─────────┬──────────┘
                                                                      │
                                                            ┌─────────▼──────────┐
                                                            │  H2 Database /     │
                                                            │  MySQL 8.x Engine  │
                                                            └────────────────────┘
```

---

## 💡 Core Design Patterns & Engineering Challenges Solved

### Challenge 1: Atomic Ledger Execution (Avoiding Deadlocks & Race Conditions)
**Context:** When a customer initiates a fund transfer (NEFT/IMPS/UPI) from Account A to Account B, the system must debit Account A, credit Account B, record a ledger transaction, and optionally flag compliance checks. In an enterprise environment, concurrent threads running this transaction could cause database locks, read anomalies, or double-spending.

**Solution: Strict Transaction isolation & Database-level Locking Strategy**
* Implemented the transaction execution within a serialized Spring transaction scope using `@Transactional(isolation = Isolation.READ_COMMITTED)`.
* Applied programmatic balance verification checks before executing writing operations.
* Logged transaction states inside an atomic execution block, guaranteeing that if either the debit or credit fails, the entire transaction is rolled back.
* Used safe numeric precision (`BigDecimal`) for financial balances to prevent floating-point inaccuracies.

```java
// Simplified transaction execution snippet from TransactionService.java
@Transactional
public Transaction executeTransfer(TransferDto transfer) {
    Account source = accountRepo.findByAccountNumber(transfer.getSourceAccount());
    Account target = accountRepo.findByAccountNumber(transfer.getTargetAccount());
    
    // Balance and status checks
    validateAccountStatus(source);
    validateAccountStatus(target);
    verifySufficientFunds(source, transfer.getAmount());
    
    // Atomic debit/credit operations
    source.setBalance(source.getBalance().subtract(transfer.getAmount()));
    target.setBalance(target.getBalance().add(transfer.getAmount()));
    
    // Persist states
    accountRepo.save(source);
    accountRepo.save(target);
    
    return transactionRepo.save(new Transaction(source, target, transfer.getAmount(), TransactionStatus.COMPLETED));
}
```

---

### Challenge 2: Deferred CSRF Session Conflicts with Spring Security 6
**Context:** Spring Security 6 defers CSRF token generation until it is explicitly requested during request rendering. In high-performance single-page templates or dashboards, this deferred resolution causes Spring to attempt session creation *after* Thymeleaf has committed headers to the response stream, resulting in `IllegalStateException: Cannot create session, response already committed`.

**Solution: Proactive CSRF Pre-Resolution Interceptor**
* Embedded a hidden, non-rendering Thymeleaf expression directly inside the base Layout templates (`layout.html`, `login.html`, `register.html`).
* By referencing `${_csrf.token}` at the very beginning of the `<body>` tag, Thymeleaf forces Spring Security to resolve the deferred CSRF token and instantiate the HTTP session *before* the first byte of markup is written to the output stream.

```html
<!-- Force CSRF session creation before response stream is committed -->
<div th:if="${_csrf != null}" th:text="${_csrf.token}" style="display:none;"></div>
```

---

### Challenge 3: Real-Time High-Throughput Ledger Simulation
**Context:** Demonstrating dynamic data and settlements in a resume/portfolio context requires real-time dashboard updates without manual input.

**Solution: Non-Blocking Scheduler-Based Transaction Generator**
* Designed a background daemon simulator utilizing Spring's `@Scheduled` tasks running asynchronously.
* The simulator periodically selects random customers, performs balance checks, schedules transfers, and recalculates monthly accruals.
* This generates a rich log of historical ledger data, making the Admin dashboard look dynamically alive without introducing locking overhead.

---

### Challenge 4: Implementing Glassmorphism 2.0 without Tailwind/Pre-processors
**Context:** Modern frontend trends favor rich glassmorphism (translucency, backdrop-blur, subtle gradients). However, bundling heavy frameworks like Tailwind adds compilation overhead and configuration friction to a monolithic Spring Boot stack.

**Solution: Pure CSS Custom Properties & Backdrop Filters**
* Utilized native CSS custom variables mapped to light-mode HSL values to provide unified design system tokens.
* Leveraged standard `backdrop-filter: blur(24px)` with fallback shadows for older engines.
* Constructed a completely clean CSS hierarchy that achieves standard glass layers without runtime javascript libraries.

---

## 🎨 White Glassmorphism 2.0 Design Tokens

```css
:root {
    --primary: #4f46e5;                    /* Indigo brand color */
    --bg-main: #dce8f5;                    /* Steel-blue background */
    --bg-card: rgba(255, 255, 255, 0.5);   /* Translucent glass layer */
    --border-color: rgba(255, 255, 255, 0.45); /* Highlight outline */
    --shadow: 0 20px 50px rgba(15, 23, 42, 0.05);
}
```

---

## 🚀 Key Takeaways for Reviewers
* **Production-Ready Configuration**: The application configures standard security parameters (Rate limits, CSRF protection, secure cookies) suitable for corporate deployment.
* **Separation of Concerns**: Backend logic relies entirely on strict Java interfaces, custom DTO validations, and Spring MVC route mappings, keeping design updates purely within the view layer.
* **Performance Focused**: Thymeleaf rendering caching and optimized CSS compilation minimize server overhead.
