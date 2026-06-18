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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoringServiceTest {

    private final Profile profile = new Profile(
            "u1", "Archi Cloud",
            List.of(ContractType.FREELANCE, ContractType.CDI),
            List.of("Lyon"), 50, 600, null,
            List.of("AWS", "Terraform", "Kubernetes"),
            List.of(), List.of("php"),
            60, null, true, Instant.now(), Instant.now());

    private Offer offer(String title, String desc, ContractType contract, Integer remote,
                        Integer tjmMax, String city) {
        return new Offer("remotive", "1", title, "Acme", city, city, "France", remote,
                contract, null, null, null, tjmMax, "EUR",
                List.of("aws", "terraform"), "url", desc, Instant.now(), Instant.now(), "k");
    }

    @Test
    void score_de_regles_eleve_pour_offre_pertinente_sans_ia() {
        ScoringService service = new ScoringService(new DisabledLlm());
        Offer o = offer("Architecte Cloud AWS", "Mission AWS Terraform Kubernetes",
                ContractType.FREELANCE, 100, 750, "Lyon");

        ScoreResult r = service.score(profile, o);

        assertTrue(r.score() >= 75, "score attendu élevé, obtenu " + r.score());
        assertNull(r.llmScore());
        assertEquals("élevé", r.confidenceLabel());
    }

    @Test
    void mot_cle_exclu_effondre_le_score() {
        ScoringService service = new ScoringService(new DisabledLlm());
        Offer o = offer("Dev PHP", "Stack PHP Symfony", ContractType.CDI, 0, null, "Lyon");

        ScoreResult r = service.score(profile, o);

        assertEquals(5, r.ruleScore());
        assertTrue(r.reasons().stream().anyMatch(s -> s.contains("php")));
    }

    @Test
    void ia_combinee_quand_disponible_et_au_dessus_du_prefiltre() {
        ScoringService service = new ScoringService(new FixedLlm(40, true));
        Offer o = offer("Architecte Cloud AWS", "AWS Terraform Kubernetes",
                ContractType.FREELANCE, 100, 750, "Lyon");

        ScoreResult r = service.score(profile, o);

        // final = 0.6*rule + 0.4*40, donc inférieur au pur rule -> l'IA a pondéré
        assertEquals(40, r.llmScore());
        assertTrue(r.freelanceConvertible());
        assertTrue(r.score() < r.ruleScore());
    }

    @Test
    void ia_non_appelee_sous_le_prefiltre() {
        ScoringService service = new ScoringService(new ThrowingLlm());
        Offer o = offer("Dev PHP", "PHP", ContractType.CDI, 0, null, "Brest");
        // mot-clé exclu -> ruleScore 5 < prefiltre -> l'IA (qui lèverait) n'est pas appelée
        ScoreResult r = service.score(profile, o);
        assertNull(r.llmScore());
    }

    // ─── doubles de test ───
    static class DisabledLlm implements LlmProvider {
        public boolean available() { return false; }
        public LlmScore score(Profile p, Offer o) { return null; }
    }

    static class FixedLlm implements LlmProvider {
        private final int score;
        private final boolean freelance;
        FixedLlm(int score, boolean freelance) { this.score = score; this.freelance = freelance; }
        public boolean available() { return true; }
        public LlmScore score(Profile p, Offer o) {
            return new LlmScore(score, "moyen", List.of("avis IA"), freelance);
        }
    }

    static class ThrowingLlm implements LlmProvider {
        public boolean available() { return true; }
        public LlmScore score(Profile p, Offer o) { throw new IllegalStateException("ne doit pas être appelé"); }
    }
}
