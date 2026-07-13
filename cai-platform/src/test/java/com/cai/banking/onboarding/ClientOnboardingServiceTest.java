package com.cai.banking.onboarding;

import com.cai.banking.domain.Client;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Parity tests for ClientOnboardingService dedupe + KYC enqueue (Apex parity). */
class ClientOnboardingServiceTest {

    static class InMemoryClientRepository implements ClientOnboardingService.ClientRepository {
        final Map<String, Client> byKey = new HashMap<>();

        @Override
        public Optional<Client> findBySinHashAndDateOfBirth(String sinHash, LocalDate dob) {
            return Optional.ofNullable(byKey.get(sinHash + "|" + dob));
        }

        @Override
        public Client save(Client client) {
            byKey.put(client.getSinHash() + "|" + client.getDateOfBirth(), client);
            return client;
        }
    }

    private final InMemoryClientRepository repo = new InMemoryClientRepository();
    private final List<Client> enqueued = new ArrayList<>();
    private final ClientOnboardingService service =
        new ClientOnboardingService(repo, enqueued::add);

    private ClientOnboardingService.OnboardingRequest request() {
        return new ClientOnboardingService.OnboardingRequest(
            "Ada", "Lovelace", "abc123hash", LocalDate.of(1985, 12, 10),
            "ada@example.com", "555-0100", "ON");
    }

    @Test
    void createsNewClientWithPendingKycAndEnqueuesVerification() {
        Client client = service.onboard(request());

        assertEquals(Client.KycStatus.PENDING, client.getKycStatus());
        assertEquals(1, enqueued.size());
        assertSame(client, enqueued.get(0));
    }

    @Test
    void dedupesBySinHashAndDob() {
        Client first = service.onboard(request());
        Client second = service.onboard(request());

        assertSame(first, second);
        assertEquals(1, enqueued.size(), "KYC only enqueued for the new client");
    }

    @Test
    void rejectsMissingSinHash() {
        var badRequest = new ClientOnboardingService.OnboardingRequest(
            "No", "Sin", " ", LocalDate.of(1985, 12, 10), null, null, null);
        assertThrows(ClientOnboardingService.InvalidOnboardingRequestException.class,
            () -> service.onboard(badRequest));
    }
}
