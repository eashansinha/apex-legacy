# Migration Plan — Salesforce (Apex) → CAI Platform

## 1. Goal

Move retail-banking client operations off Salesforce onto the bank's custom
platform (**CAI**), eliminating Apex/SOQL lock-in while preserving every
business rule currently enforced in the org.

Target stack: **Java 17 + Spring Boot 3** services under `cai-platform/`,
JPA/Postgres for persistence, REST APIs replacing Apex controllers, and
Spring events replacing record triggers.

## 2. Source Inventory (what the Apex org does today)

| Apex artifact | Responsibility | CAI target |
|---|---|---|
| `LoanApplicationService.qualify` | GDS ≤ 39% / TDS ≤ 44% qualification with +2% stress test; persists decision transactionally (`Database.Savepoint`) | `LoanQualificationService` (`@Transactional`) |
| `LoanApplicationTrigger` + `LoanApplicationTriggerHandler` | Status-transition state machine; stamps `Submitted_Date__c` | `LoanApplicationStateMachine` enforced in the service layer (no trigger equivalent needed) |
| `ClientOnboardingService.onboard` | Dedupe by SIN hash + DOB before creating client; enqueues KYC | `ClientOnboardingService` + unique DB constraint on `(sin_hash, date_of_birth)` |
| `KYCVerificationService` (`@future` callout) | Async identity-provider verification | `KycVerificationService` using `@Async` + `RestClient`; retry via Spring Retry |
| `ClientSelector` (SOQL) | Client lookups, stale-KYC scans | Spring Data JPA repository queries |
| `TransactionDisputeService` | Dispute intake; >$500 or (fraud reason code **and** card present) → 48h SLA Case, else 10 business days | `DisputeService` + `EscalationService` |
| Custom objects `Loan_Application__c`, `Transaction_Dispute__c` | Data model | JPA entities `LoanApplication`, `TransactionDispute` (Flyway migrations) |

## 3. Business Rules That MUST Survive (verification checklist)

1. Stress test: qualification rate = offered rate **+ 2%**.
2. GDS ≤ 39%, TDS ≤ 44% — decline reasons must state which ratio failed.
3. KYC gate: block qualification unless KYC `Verified` within 365 days.
4. Status transitions: `Draft→Submitted|Cancelled`, `Submitted→Approved|Declined|Cancelled`, `Approved→Funded|Cancelled`; `Declined/Funded/Cancelled` terminal.
5. Dispute escalation: amount > $500 **or** (fraud reason code **and** card present) → 48-hour SLA; otherwise 10 **business** days.
6. Onboarding dedupe by SIN hash + DOB; new clients start `Pending` KYC.

Each rule maps 1:1 to a JUnit test in the CAI services (parity with the
existing Apex tests `LoanApplicationServiceTest` / `TransactionDisputeServiceTest`).

## 4. Salesforce-isms and their CAI replacements

| Salesforce concept | CAI replacement |
|---|---|
| SOQL | Spring Data JPA / JPQL |
| DML + `Database.Savepoint` | `@Transactional` with rollback |
| `@future(callout=true)` | `@Async` service method + `RestClient` |
| Triggers (`before update`) | Service-layer validation (single write path) |
| `addError()` | Domain exceptions → HTTP 422 |
| Person Accounts | `Client` entity |
| Named Credential `Identity_Provider` | Spring config + secret manager |

## 5. Migration Slices (each is an independent PR with tests)

1. **Slice 0 — this plan** (docs only).
2. **Slice 1 — CAI scaffold + loan qualification**: Maven module, domain
   model, `LoanQualificationService`, state machine, JUnit parity tests.
3. **Slice 2 — disputes + onboarding/KYC**: `DisputeService`,
   `ClientOnboardingService`, `KycVerificationService`, parity tests.
4. **Slice 3 (post-demo) — data migration**: Bulk API export → Flyway-seeded
   Postgres, dual-run verification, Salesforce read-only cutover.

## 6. Review & Test Gates

- Every slice PR: code review + `mvn test` green in CI.
- Rule-parity table (§3) checked off in each PR description.
- No slice merges without its Apex-equivalent test ported.
