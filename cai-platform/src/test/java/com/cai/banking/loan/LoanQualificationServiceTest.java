package com.cai.banking.loan;

import com.cai.banking.domain.Client;
import com.cai.banking.domain.LoanApplication;
import com.cai.banking.domain.LoanApplication.Status;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Parity tests ported from LoanApplicationServiceTest (Apex). */
class LoanQualificationServiceTest {

    private final LoanQualificationService service = new LoanQualificationService();

    private Client verifiedClient() {
        Client c = new Client("Test", "Client", "hash", LocalDate.of(1990, 1, 1));
        c.setKycStatus(Client.KycStatus.VERIFIED);
        c.setKycVerifiedDate(LocalDate.now().minusDays(30));
        return c;
    }

    private LoanApplication app(UUID clientId, String amount, String income) {
        LoanApplication a = new LoanApplication(
            clientId, new BigDecimal(amount), new BigDecimal("0.0549"), 300,
            new BigDecimal(income), new BigDecimal("450"), new BigDecimal("300"));
        LoanApplicationStateMachine.transition(a, Status.SUBMITTED);
        return a;
    }

    @Test
    void qualifiesHealthyApplication() {
        Client client = verifiedClient();
        LoanApplication application = app(client.getId(), "300000", "160000");

        var result = service.qualify(application, client);

        assertTrue(result.qualified(), "Healthy application should qualify");
        assertEquals(0, new BigDecimal("0.0749").compareTo(result.stressTestedRate()),
            "Rate stress-tested at +2%");
        assertEquals(Status.APPROVED, application.getStatus());
    }

    @Test
    void declinesWhenGdsTooHigh() {
        Client client = verifiedClient();
        LoanApplication application = app(client.getId(), "900000", "60000");

        var result = service.qualify(application, client);

        assertFalse(result.qualified());
        assertTrue(result.declineReason().contains("GDS"));
        assertEquals(Status.DECLINED, application.getStatus());
    }

    @Test
    void declinesWhenTdsTooHighButGdsOk() {
        Client client = verifiedClient();
        LoanApplication application = new LoanApplication(
            client.getId(), new BigDecimal("300000"), new BigDecimal("0.0549"), 300,
            new BigDecimal("100000"), new BigDecimal("450"), new BigDecimal("1800"));
        LoanApplicationStateMachine.transition(application, Status.SUBMITTED);

        var result = service.qualify(application, client);

        assertFalse(result.qualified());
        assertTrue(result.declineReason().contains("TDS"));
    }

    @Test
    void blocksStaleKyc() {
        Client client = new Client("Stale", "Kyc", "hash", LocalDate.of(1990, 1, 1));
        client.setKycStatus(Client.KycStatus.VERIFIED);
        client.setKycVerifiedDate(LocalDate.now().minusDays(400));
        LoanApplication application = app(client.getId(), "300000", "160000");

        var ex = assertThrows(LoanQualificationService.KycNotCurrentException.class,
            () -> service.qualify(application, client));
        assertTrue(ex.getMessage().contains("KYC not current"));
    }

    @Test
    void monthlyPaymentMatchesAmortizationFormula() {
        // $300k at 7.49% over 300 months ≈ $2,215.02/month
        BigDecimal payment = LoanQualificationService.monthlyPayment(
            new BigDecimal("300000"), new BigDecimal("0.0749"), 300);
        assertEquals(2215.02, payment.doubleValue(), 0.5);
    }
}
