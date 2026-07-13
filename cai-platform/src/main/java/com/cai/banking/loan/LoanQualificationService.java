package com.cai.banking.loan;

import com.cai.banking.domain.Client;
import com.cai.banking.domain.LoanApplication;
import com.cai.banking.domain.LoanApplication.Status;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Ported from LoanApplicationService (Apex).
 * Rules preserved:
 *   - qualification rate = offered rate + 2% (stress test)
 *   - GDS <= 39%, TDS <= 44%
 *   - KYC must be Verified within the last 365 days
 */
public class LoanQualificationService {

    public static class KycNotCurrentException extends RuntimeException {
        public KycNotCurrentException(Object clientId) {
            super("KYC not current for client " + clientId + "; application blocked");
        }
    }

    public record QualificationResult(
        boolean qualified,
        BigDecimal gdsRatio,
        BigDecimal tdsRatio,
        BigDecimal stressTestedRate,
        String declineReason) {}

    private static final BigDecimal MAX_GDS = new BigDecimal("0.39");
    private static final BigDecimal MAX_TDS = new BigDecimal("0.44");
    private static final BigDecimal STRESS_TEST_BUFFER = new BigDecimal("0.02");
    private static final MathContext MC = MathContext.DECIMAL64;

    public QualificationResult qualify(LoanApplication app, Client client) {
        if (!client.isKycCurrent(LocalDate.now())) {
            throw new KycNotCurrentException(client.getId());
        }

        BigDecimal stressRate = app.getOfferedRate().add(STRESS_TEST_BUFFER);
        BigDecimal payment = monthlyPayment(app.getAmount(), stressRate, app.getAmortizationMonths());
        BigDecimal monthlyIncome = app.getAnnualIncome().divide(BigDecimal.valueOf(12), MC);

        BigDecimal gds = payment.add(app.getMonthlyHousingCosts()).divide(monthlyIncome, MC);
        BigDecimal tds = payment.add(app.getMonthlyHousingCosts())
            .add(app.getMonthlyDebtPayments()).divide(monthlyIncome, MC);

        boolean qualified;
        String declineReason = null;
        if (gds.compareTo(MAX_GDS) > 0) {
            qualified = false;
            declineReason = "GDS ratio " + gds.setScale(4, RoundingMode.HALF_UP) + " exceeds 39%";
        } else if (tds.compareTo(MAX_TDS) > 0) {
            qualified = false;
            declineReason = "TDS ratio " + tds.setScale(4, RoundingMode.HALF_UP) + " exceeds 44%";
        } else {
            qualified = true;
        }

        LoanApplicationStateMachine.transition(
            app, qualified ? Status.APPROVED : Status.DECLINED);
        app.setGdsRatio(gds);
        app.setTdsRatio(tds);
        app.setDeclineReason(declineReason);

        return new QualificationResult(qualified, gds, tds, stressRate, declineReason);
    }

    /** Standard amortized monthly payment; parity with Apex monthlyPayment(). */
    static BigDecimal monthlyPayment(BigDecimal principal, BigDecimal annualRate, int months) {
        BigDecimal r = annualRate.divide(BigDecimal.valueOf(12), MC);
        BigDecimal factor = BigDecimal.ONE.add(r).pow(months, MC);
        return principal.multiply(r.multiply(factor, MC), MC)
            .divide(factor.subtract(BigDecimal.ONE), MC);
    }
}
