package com.cai.banking.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Replaces the Salesforce custom object Transaction_Dispute__c. */
public class TransactionDispute {

    public enum Status { OPEN, ESCALATED, RESOLVED, REJECTED }

    private final UUID id;
    private final UUID clientId;
    private final String cardLast4;
    private final BigDecimal amount;
    private final String reasonCode;
    private final boolean cardPresent;
    private Status status = Status.OPEN;
    private final Instant filedDate;
    private LocalDate slaDueDate;
    private UUID escalationCaseId;

    public TransactionDispute(UUID clientId, String cardLast4, BigDecimal amount,
                              String reasonCode, boolean cardPresent) {
        this.id = UUID.randomUUID();
        this.clientId = clientId;
        this.cardLast4 = cardLast4;
        this.amount = amount;
        this.reasonCode = reasonCode;
        this.cardPresent = cardPresent;
        this.filedDate = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getClientId() { return clientId; }
    public String getCardLast4() { return cardLast4; }
    public BigDecimal getAmount() { return amount; }
    public String getReasonCode() { return reasonCode; }
    public boolean isCardPresent() { return cardPresent; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Instant getFiledDate() { return filedDate; }
    public LocalDate getSlaDueDate() { return slaDueDate; }
    public void setSlaDueDate(LocalDate slaDueDate) { this.slaDueDate = slaDueDate; }
    public UUID getEscalationCaseId() { return escalationCaseId; }
    public void setEscalationCaseId(UUID escalationCaseId) { this.escalationCaseId = escalationCaseId; }
}
