package com.cai.banking.onboarding;

import com.cai.banking.domain.Client;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Ported from ClientOnboardingService (Apex).
 * Dedupe by SIN hash + date of birth; new clients start with Pending KYC and
 * are queued for asynchronous verification (replaces the @future callout).
 */
public class ClientOnboardingService {

    public static class InvalidOnboardingRequestException extends RuntimeException {
        public InvalidOnboardingRequestException(String message) { super(message); }
    }

    public record OnboardingRequest(
        String firstName, String lastName, String sinHash, LocalDate dateOfBirth,
        String email, String phone, String province) {}

    /** Replaces the ClientSelector SOQL lookup; backed by JPA in production. */
    public interface ClientRepository {
        Optional<Client> findBySinHashAndDateOfBirth(String sinHash, LocalDate dateOfBirth);
        Client save(Client client);
    }

    /** Replaces the @future(callout=true) KYC verification enqueue. */
    public interface KycVerificationQueue {
        void enqueue(Client client);
    }

    private final ClientRepository repository;
    private final KycVerificationQueue kycQueue;

    public ClientOnboardingService(ClientRepository repository, KycVerificationQueue kycQueue) {
        this.repository = repository;
        this.kycQueue = kycQueue;
    }

    public Client onboard(OnboardingRequest req) {
        if (req.sinHash() == null || req.sinHash().isBlank() || req.dateOfBirth() == null) {
            throw new InvalidOnboardingRequestException(
                "SIN hash and date of birth are required for dedupe");
        }

        Optional<Client> existing =
            repository.findBySinHashAndDateOfBirth(req.sinHash(), req.dateOfBirth());
        if (existing.isPresent()) {
            return existing.get();
        }

        Client client = new Client(req.firstName(), req.lastName(), req.sinHash(), req.dateOfBirth());
        client.setEmail(req.email());
        client.setPhone(req.phone());
        client.setProvince(req.province());
        client = repository.save(client);

        kycQueue.enqueue(client);
        return client;
    }
}
