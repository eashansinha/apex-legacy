package com.cai.banking.dispute;

import com.cai.banking.domain.TransactionDispute;
import com.cai.banking.domain.TransactionDispute.Status;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Ported from TransactionDisputeService (Apex).
 * Rules preserved:
 *   - amount > $500 OR (fraud reason code AND card present) -> escalate, 48h SLA
 *   - otherwise 10 business days SLA
 */
public class DisputeService {

    public static class InvalidDisputeException extends RuntimeException {
        public InvalidDisputeException(String message) { super(message); }
    }

    /** Replaces Case creation in Apex escalate(); returns the escalation case id. */
    public interface EscalationCaseFactory extends Supplier<UUID> {}

    private static final BigDecimal ESCALATION_AMOUNT = new BigDecimal("500");
    private static final int URGENT_SLA_HOURS = 48;
    private static final int STANDARD_SLA_BUSINESS_DAYS = 10;
    private static final Set<String> FRAUD_CODES = Set.of("FRAUD_10.4", "FRAUD_10.1");

    private final EscalationCaseFactory escalationCaseFactory;

    public DisputeService(EscalationCaseFactory escalationCaseFactory) {
        this.escalationCaseFactory = escalationCaseFactory;
    }

    public TransactionDispute fileDispute(UUID clientId, String cardLast4, BigDecimal amount,
                                          String reasonCode, boolean cardPresent) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidDisputeException("Dispute amount must be positive");
        }

        TransactionDispute dispute =
            new TransactionDispute(clientId, cardLast4, amount, reasonCode, cardPresent);

        if (requiresUrgentEscalation(amount, reasonCode, cardPresent)) {
            dispute.setEscalationCaseId(escalationCaseFactory.get());
            dispute.setStatus(Status.ESCALATED);
            dispute.setSlaDueDate(LocalDate.now().plusDays(URGENT_SLA_HOURS / 24));
        } else {
            dispute.setSlaDueDate(addBusinessDays(LocalDate.now(), STANDARD_SLA_BUSINESS_DAYS));
        }
        return dispute;
    }

    static boolean requiresUrgentEscalation(BigDecimal amount, String reasonCode, boolean cardPresent) {
        return amount.compareTo(ESCALATION_AMOUNT) > 0
            || (reasonCode != null && FRAUD_CODES.contains(reasonCode) && cardPresent);
    }

    static LocalDate addBusinessDays(LocalDate start, int businessDays) {
        LocalDate result = start;
        int added = 0;
        while (added < businessDays) {
            result = result.plusDays(1);
            DayOfWeek day = result.getDayOfWeek();
            if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
                added++;
            }
        }
        return result;
    }
}
