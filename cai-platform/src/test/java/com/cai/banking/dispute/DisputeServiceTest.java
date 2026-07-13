package com.cai.banking.dispute;

import com.cai.banking.domain.TransactionDispute;
import com.cai.banking.domain.TransactionDispute.Status;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Parity tests ported from TransactionDisputeServiceTest (Apex). */
class DisputeServiceTest {

    private final UUID caseId = UUID.randomUUID();
    private final DisputeService service = new DisputeService(() -> caseId);

    @Test
    void escalatesLargeDispute() {
        TransactionDispute dispute = service.fileDispute(
            UUID.randomUUID(), "4242", new BigDecimal("750"), "GOODS_13.1", false);

        assertEquals(Status.ESCALATED, dispute.getStatus());
        assertEquals(caseId, dispute.getEscalationCaseId());
        assertEquals(LocalDate.now().plusDays(2), dispute.getSlaDueDate());
    }

    @Test
    void standardSlaForSmallDispute() {
        TransactionDispute dispute = service.fileDispute(
            UUID.randomUUID(), "4242", new BigDecimal("120"), "GOODS_13.1", false);

        assertEquals(Status.OPEN, dispute.getStatus());
        assertNull(dispute.getEscalationCaseId());
        assertTrue(dispute.getSlaDueDate().isAfter(LocalDate.now().plusDays(9)));
    }

    @Test
    void cardPresentFraudEscalatesRegardlessOfAmount() {
        assertTrue(DisputeService.requiresUrgentEscalation(
            new BigDecimal("100"), "FRAUD_10.4", true));
        assertFalse(DisputeService.requiresUrgentEscalation(
            new BigDecimal("100"), "FRAUD_10.4", false));
    }

    @Test
    void rejectsNonPositiveAmount() {
        var ex = assertThrows(DisputeService.InvalidDisputeException.class,
            () -> service.fileDispute(UUID.randomUUID(), "4242", BigDecimal.ZERO, "X", false));
        assertTrue(ex.getMessage().contains("positive"));
    }

    @Test
    void businessDaysSkipWeekends() {
        LocalDate due = DisputeService.addBusinessDays(LocalDate.of(2026, 7, 10), 10); // a Friday
        assertEquals(LocalDate.of(2026, 7, 24), due);
        assertNotEquals(DayOfWeek.SATURDAY, due.getDayOfWeek());
    }

    @Test
    void nullReasonCodeGetsStandardSlaWithoutCrashing() {
        TransactionDispute dispute = service.fileDispute(
            UUID.randomUUID(), "4242", new BigDecimal("100"), null, true);

        assertEquals(Status.OPEN, dispute.getStatus());
        assertNull(dispute.getEscalationCaseId());
    }
}
