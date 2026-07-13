package com.cai.banking.loan;

import com.cai.banking.domain.LoanApplication;
import com.cai.banking.domain.LoanApplication.Status;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Parity tests ported from LoanApplicationTriggerHandler (Apex). */
class LoanApplicationStateMachineTest {

    private LoanApplication draft() {
        return new LoanApplication(UUID.randomUUID(), new BigDecimal("100000"),
            new BigDecimal("0.05"), 300, new BigDecimal("90000"),
            new BigDecimal("400"), new BigDecimal("200"));
    }

    @Test
    void stampsSubmittedDateOnSubmission() {
        LoanApplication app = draft();
        assertNull(app.getSubmittedDate());
        LoanApplicationStateMachine.transition(app, Status.SUBMITTED);
        assertNotNull(app.getSubmittedDate());
        assertEquals(Status.SUBMITTED, app.getStatus());
    }

    @Test
    void allowsFullHappyPath() {
        LoanApplication app = draft();
        LoanApplicationStateMachine.transition(app, Status.SUBMITTED);
        LoanApplicationStateMachine.transition(app, Status.APPROVED);
        LoanApplicationStateMachine.transition(app, Status.FUNDED);
        assertEquals(Status.FUNDED, app.getStatus());
    }

    @Test
    void rejectsDraftToApproved() {
        LoanApplication app = draft();
        assertThrows(LoanApplicationStateMachine.IllegalTransitionException.class,
            () -> LoanApplicationStateMachine.transition(app, Status.APPROVED));
    }

    @Test
    void declinedIsTerminal() {
        LoanApplication app = draft();
        LoanApplicationStateMachine.transition(app, Status.SUBMITTED);
        LoanApplicationStateMachine.transition(app, Status.DECLINED);
        assertThrows(LoanApplicationStateMachine.IllegalTransitionException.class,
            () -> LoanApplicationStateMachine.transition(app, Status.SUBMITTED));
    }
}
