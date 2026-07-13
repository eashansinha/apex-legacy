package com.cai.banking.domain;

import java.time.LocalDate;
import java.util.UUID;

/** Replaces the Salesforce person Account. */
public class Client {
    public enum KycStatus { PENDING, VERIFIED, FAILED, ERROR }

    private final UUID id;
    private String firstName;
    private String lastName;
    private String sinHash;
    private LocalDate dateOfBirth;
    private String email;
    private String phone;
    private String province;
    private KycStatus kycStatus = KycStatus.PENDING;
    private LocalDate kycVerifiedDate;

    public Client(String firstName, String lastName, String sinHash, LocalDate dateOfBirth) {
        this.id = UUID.randomUUID();
        this.firstName = firstName;
        this.lastName = lastName;
        this.sinHash = sinHash;
        this.dateOfBirth = dateOfBirth;
    }

    public UUID getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getSinHash() { return sinHash; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
    public KycStatus getKycStatus() { return kycStatus; }
    public void setKycStatus(KycStatus kycStatus) { this.kycStatus = kycStatus; }
    public LocalDate getKycVerifiedDate() { return kycVerifiedDate; }
    public void setKycVerifiedDate(LocalDate kycVerifiedDate) { this.kycVerifiedDate = kycVerifiedDate; }

    /** KYC gate ported from LoanApplicationService.assertKycCurrent (Apex). */
    public boolean isKycCurrent(LocalDate asOf) {
        return kycStatus == KycStatus.VERIFIED
            && kycVerifiedDate != null
            && !kycVerifiedDate.isBefore(asOf.minusDays(365));
    }
}
