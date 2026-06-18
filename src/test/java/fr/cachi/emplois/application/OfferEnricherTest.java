package fr.cachi.emplois.application;

import fr.cachi.emplois.domain.model.ContractType;
import fr.cachi.emplois.domain.model.ExtractedFields;
import fr.cachi.emplois.domain.model.LlmScore;
import fr.cachi.emplois.domain.model.Offer;
import fr.cachi.emplois.domain.model.Profile;
import fr.cachi.emplois.domain.port.LlmProvider;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfferEnricherTest {

    private static Offer offer(ContractType contract, Integer remote, List<String> stack,
                               Integer tjmMin, Integer salaryMin, String city) {
        return new Offer("remotive", "1", "DevOps", "Acme", "Europe", city, null, remote, contract,
                salaryMin, null, tjmMin, null, null, stack, "https://x/1",
                "Mission longue Terraform AWS", Instant.now(), Instant.now(), "k");
    }

    @Test
    void enrichit_les_champs_manquants_via_extraction() {
        LlmProvider llm = new FakeLlm(new ExtractedFields(
                ContractType.FREELANCE, 100, List.of("terraform", "aws"),
                600, 750, null, null, "EUR", "Lyon"));

        Offer in = offer(ContractType.UNKNOWN, null, List.of(), null, null, null);
        Offer out = new OfferEnricher(llm).enrich(in);

        assertEquals(ContractType.FREELANCE, out.contractType());
        assertEquals(100, out.remotePercent());
        assertTrue(out.stack().contains("terraform"));
        assertEquals(600, out.tjmMin());
        assertEquals("EUR", out.currency());
        assertEquals("Lyon", out.city());
        assertEquals("France", out.country());
    }

    @Test
    void ne_remplace_pas_les_valeurs_deja_presentes() {
        LlmProvider llm = new FakeLlm(new ExtractedFields(
                ContractType.FREELANCE, 0, List.of("python"), 800, null, null, null, "USD", "Paris"));

        // Télétravail manquant => l'enrichissement se déclenche, mais ne doit pas écraser le reste.
        Offer in = offer(ContractType.CDI, null, List.of("java"), null, 55000, "Lyon");
        Offer out = new OfferEnricher(llm).enrich(in);

        assertEquals(ContractType.CDI, out.contractType(), "contrat déjà connu : non remplacé");
        assertEquals(0, out.remotePercent(), "télétravail manquant : comblé par l'IA");
        assertEquals("Lyon", out.city(), "ville déjà connue : non remplacée");
        assertEquals(55000, out.salaryMin(), "salaire déjà connu : non remplacé");
        assertTrue(out.stack().contains("java") && out.stack().contains("python"), "stack fusionnée");
    }

    @Test
    void ne_sollicite_pas_l_ia_si_aucun_champ_manquant() {
        Offer complet = offer(ContractType.CDI, 50, List.of("java"), null, 55000, "Lyon");
        assertFalse(OfferEnricher.needsEnrichment(complet));

        FakeLlm llm = new FakeLlm(ExtractedFields.empty());
        Offer out = new OfferEnricher(llm).enrich(complet);
        assertSame(complet, out, "offre inchangée");
        assertEquals(0, llm.calls, "aucun appel IA");
    }

    @Test
    void ia_indisponible_laisse_l_offre_en_l_etat() {
        Offer in = offer(ContractType.UNKNOWN, null, List.of(), null, null, null);
        Offer out = new OfferEnricher(new DisabledLlm()).enrich(in);
        assertSame(in, out);
    }

    // ─── doubles ───
    static class FakeLlm implements LlmProvider {
        private final ExtractedFields fields;
        int calls = 0;
        FakeLlm(ExtractedFields fields) { this.fields = fields; }
        public boolean available() { return true; }
        public LlmScore score(Profile p, Offer o) { return null; }
        public ExtractedFields extract(Offer o) { calls++; return fields; }
    }

    static class DisabledLlm implements LlmProvider {
        public boolean available() { return false; }
        public LlmScore score(Profile p, Offer o) { return null; }
    }
}
