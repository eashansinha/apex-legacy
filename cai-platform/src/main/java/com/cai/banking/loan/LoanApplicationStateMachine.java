package com.cai.banking.loan;

import com.cai.banking.domain.LoanApplication;
import com.cai.banking.domain.LoanApplication.Status;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Ported from LoanApplicationTrigger + LoanApplicationTriggerHandler (Apex).
 * Enforced in the service layer — CAI has a single write path, so no trigger
 * mechanism is needed.
 */
public final class LoanApplicationStateMachine {

    public static class IllegalTransitionException extends RuntimeException {
        public IllegalTransitionException(Status from, Status to) {
            super("Illegal status transition: " + from + " -> " + to);
        }
    }

    private static final Map<Status, Set<Status>> ALLOWED = Map.of(
        Status.DRAFT,     Set.of(Status.SUBMITTED, Status.CANCELLED),
        Status.SUBMITTED, Set.of(Status.APPROVED, Status.DECLINED, Status.CANCELLED),
        Status.APPROVED,  Set.of(Status.FUNDED, Status.CANCELLED),
        Status.DECLINED,  Set.of(),
        Status.FUNDED,    Set.of(),
        Status.CANCELLED, Set.of()
    );

    private LoanApplicationStateMachine() {}

    public static void transition(LoanApplication app, Status to) {
        Status from = app.getStatus();
        if (!ALLOWED.get(from).contains(to)) {
            throw new IllegalTransitionException(from, to);
        }
        if (from == Status.DRAFT && to == Status.SUBMITTED) {
            app.setSubmittedDate(Instant.now());
        }
        app.setStatus(to);
    }
}
