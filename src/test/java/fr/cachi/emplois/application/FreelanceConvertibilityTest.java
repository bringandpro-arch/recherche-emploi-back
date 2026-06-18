package fr.cachi.emplois.application;

import fr.cachi.emplois.domain.model.ContractType;
import fr.cachi.emplois.domain.model.LlmScore;
import fr.cachi.emplois.domain.model.Offer;
import fr.cachi.emplois.domain.model.Profile;
import fr.cachi.emplois.domain.model.ScoreResult;
import fr.cachi.emplois.domain.port.LlmProvider;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FreelanceConvertibilityTest {

    private static Offer offer(String desc, ContractType contract) {
        return new Offer("ft", "1", "Ingénieur DevOps", "Acme", "Lyon", "Lyon", "France", 50,
                contract, 55000, 65000, null, null, "EUR", List.of("aws"), "url",
                desc, Instant.now(), Instant.now(), "k");
    }

    @Test
    void cdi_en_regie_esn_est_convertible() {
        assertTrue(FreelanceConvertibility.looksConvertible(
                offer("Poste en régie chez un grand compte, ESN reconnue", ContractType.CDI)));
        assertTrue(FreelanceConvertibility.looksConvertible(
                offer("Mission de renfort longue durée pour un consultant", ContractType.CDI)));
    }

    @Test
    void cdi_produit_classique_non_convertible() {
        assertFalse(FreelanceConvertibility.looksConvertible(
                offer("Rejoignez notre équipe produit en interne", ContractType.CDI)));
    }

    @Test
    void offre_deja_freelance_non_convertible() {
        assertFalse(FreelanceConvertibility.looksConvertible(
                offer("Mission en régie", ContractType.FREELANCE)));
    }

    @Test
    void scoring_marque_la_convertibilite_meme_sans_ia() {
        Profile profile = new Profile("u1", "Archi", List.of(ContractType.FREELANCE),
                List.of("Lyon"), 0, 500, null, List.of("AWS"), List.of(), List.of(),
                60, null, true, Instant.now(), Instant.now());
        ScoringService service = new ScoringService(new DisabledLlm());

        ScoreResult r = service.score(profile, offer("Mission en régie, ESN, AWS", ContractType.CDI));

        assertTrue(r.freelanceConvertible());
        assertTrue(r.reasons().stream().anyMatch(s -> s.contains("freelance")));
    }

    static class DisabledLlm implements LlmProvider {
        public boolean available() { return false; }
        public LlmScore score(Profile p, Offer o) { return null; }
    }
}
