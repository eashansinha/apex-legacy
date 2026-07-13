package com.cai.banking.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Replaces the Salesforce custom object Loan_Application__c. */
public class LoanApplication {

    public enum Status { DRAFT, SUBMITTED, APPROVED, DECLINED, FUNDED, CANCELLED }

    private final UUID id;
    private final UUID clientId;
    private BigDecimal amount;
    private BigDecimal offeredRate;
    private int amortizationMonths;
    private BigDecimal annualIncome;
    private BigDecimal monthlyHousingCosts;
    private BigDecimal monthlyDebtPayments;
    private Status status = Status.DRAFT;
    private Instant submittedDate;
    private BigDecimal gdsRatio;
    private BigDecimal tdsRatio;
    private String declineReason;

    public LoanApplication(UUID clientId, BigDecimal amount, BigDecimal offeredRate,
                           int amortizationMonths, BigDecimal annualIncome,
                           BigDecimal monthlyHousingCosts, BigDecimal monthlyDebtPayments) {
        this.id = UUID.randomUUID();
        this.clientId = clientId;
        this.amount = amount;
        this.offeredRate = offeredRate;
        this.amortizationMonths = amortizationMonths;
        this.annualIncome = annualIncome;
        this.monthlyHousingCosts = monthlyHousingCosts;
        this.monthlyDebtPayments = monthlyDebtPayments;
    }

    public UUID getId() { return id; }
    public UUID getClientId() { return clientId; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getOfferedRate() { return offeredRate; }
    public int getAmortizationMonths() { return amortizationMonths; }
    public BigDecimal getAnnualIncome() { return annualIncome; }
    public BigDecimal getMonthlyHousingCosts() { return monthlyHousingCosts; }
    public BigDecimal getMonthlyDebtPayments() { return monthlyDebtPayments; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Instant getSubmittedDate() { return submittedDate; }
    public void setSubmittedDate(Instant submittedDate) { this.submittedDate = submittedDate; }
    public BigDecimal getGdsRatio() { return gdsRatio; }
    public void setGdsRatio(BigDecimal gdsRatio) { this.gdsRatio = gdsRatio; }
    public BigDecimal getTdsRatio() { return tdsRatio; }
    public void setTdsRatio(BigDecimal tdsRatio) { this.tdsRatio = tdsRatio; }
    public String getDeclineReason() { return declineReason; }
    public void setDeclineReason(String declineReason) { this.declineReason = declineReason; }
}
