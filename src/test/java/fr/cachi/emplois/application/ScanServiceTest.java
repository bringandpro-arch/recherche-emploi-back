package fr.cachi.emplois.application;

import fr.cachi.emplois.domain.model.ContractType;
import fr.cachi.emplois.domain.model.LlmScore;
import fr.cachi.emplois.domain.model.Offer;
import fr.cachi.emplois.domain.model.Profile;
import fr.cachi.emplois.domain.model.RawJob;
import fr.cachi.emplois.domain.model.ScanRun;
import fr.cachi.emplois.domain.model.ScoredOffer;
import fr.cachi.emplois.domain.model.SearchCriteria;
import fr.cachi.emplois.domain.port.JobSource;
import fr.cachi.emplois.domain.port.LlmProvider;
import fr.cachi.emplois.domain.port.Notifier;
import fr.cachi.emplois.domain.port.OfferRepository;
import fr.cachi.emplois.domain.port.ProfileRepository;
import fr.cachi.emplois.domain.port.ScoredOfferRepository;
import fr.cachi.emplois.domain.port.SeenOfferRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScanServiceTest {

    private final Profile profile = new Profile(
            "u1", "Archi", List.of(ContractType.CDI), List.of("Lyon"), 0, null, null,
            List.of("AWS", "Terraform"), List.of(), List.of(), 60, "chat-1", true,
            Instant.now(), Instant.now());

    @Test
    void scan_complet_notifie_les_offres_pertinentes_et_marque_vu() {
        StubSource source = new StubSource(List.of(
                raw("Architecte Cloud AWS", "AWS Terraform Kubernetes", "CDI", "Lyon"),
                raw("Commercial", "Vente B2B", null, "Paris")));
        InMemoryScored scored = new InMemoryScored();
        CountingNotifier notifier = new CountingNotifier();

        ScanService service = new ScanService(
                List.of(source), new OfferNormalizer(),
                new DedupService(new InMemorySeen()),
                new ScoringService(new DisabledLlm()),
                new InMemoryProfiles(profile),
                new InMemoryOffers(), scored, notifier, null);

        ScanRun run = service.run();

        assertEquals("OK", run.status());
        assertEquals(2, run.fetched());
        assertEquals(2, run.newCount());
        assertEquals(1, run.notified(), "seule l'offre pertinente doit être notifiée");
        assertEquals(2, scored.saved.size());
        assertTrue(notifier.notified.stream().anyMatch(s -> s.offer().title().contains("Architecte")));
    }

    @Test
    void second_scan_ne_renotifie_pas_les_offres_deja_vues() {
        StubSource source = new StubSource(List.of(
                raw("Architecte Cloud AWS", "AWS Terraform", "CDI", "Lyon")));
        InMemorySeen seen = new InMemorySeen();
        CountingNotifier notifier = new CountingNotifier();
        ScanService service = new ScanService(
                List.of(source), new OfferNormalizer(), new DedupService(seen),
                new ScoringService(new DisabledLlm()), new InMemoryProfiles(profile),
                new InMemoryOffers(), new InMemoryScored(), notifier, null);

        service.run();
        ScanRun second = service.run();

        assertEquals(0, second.newCount());
        assertEquals(0, second.notified());
    }

    private static RawJob raw(String title, String desc, String contract, String location) {
        return new RawJob("stub", title, title, "Acme", location, "https://x/" + title.hashCode(),
                desc, Instant.now(), contract, null, null, List.of());
    }

    // ─── doubles ───
    static class StubSource implements JobSource {
        private final List<RawJob> jobs;
        StubSource(List<RawJob> jobs) { this.jobs = jobs; }
        public String code() { return "stub"; }
        public boolean enabled() { return true; }
        public List<RawJob> fetch(SearchCriteria c) { return jobs; }
    }

    static class DisabledLlm implements LlmProvider {
        public boolean available() { return false; }
        public LlmScore score(Profile p, Offer o) { return null; }
    }

    static class CountingNotifier implements Notifier {
        final List<ScoredOffer> notified = new ArrayList<>();
        public boolean enabled() { return true; }
        public boolean notify(Profile p, ScoredOffer s) { notified.add(s); return true; }
    }

    static class InMemoryProfiles implements ProfileRepository {
        private final Profile profile;
        InMemoryProfiles(Profile profile) { this.profile = profile; }
        public Optional<Profile> findByUserId(String userId) { return Optional.of(profile); }
        public Profile save(Profile p) { return p; }
        public List<Profile> findAllActive() { return List.of(profile); }
    }

    static class InMemoryOffers implements OfferRepository {
        public void upsert(Offer offer) { }
        public Optional<Offer> findByDedupKey(String dedupKey) { return Optional.empty(); }
    }

    static class InMemoryScored implements ScoredOfferRepository {
        final List<ScoredOffer> saved = new ArrayList<>();
        public void save(ScoredOffer s) { saved.add(s); }
        public List<ScoredOffer> listByUser(String userId) { return saved; }
    }

    static class InMemorySeen implements SeenOfferRepository {
        private final Set<String> store = new HashSet<>();
        public boolean isSeen(String userId, String dedupKey) { return store.contains(userId + "|" + dedupKey); }
        public void markSeen(String userId, String dedupKey, Instant when) { store.add(userId + "|" + dedupKey); }
    }
}
