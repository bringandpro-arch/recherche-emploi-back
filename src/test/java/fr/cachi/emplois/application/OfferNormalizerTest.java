package fr.cachi.emplois.application;

import fr.cachi.emplois.domain.model.ContractType;
import fr.cachi.emplois.domain.model.Offer;
import fr.cachi.emplois.domain.model.RawJob;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfferNormalizerTest {

    private final OfferNormalizer normalizer = new OfferNormalizer();

    @Test
    void normalise_un_cdi_lyonnais_avec_salaire_annuel() {
        RawJob raw = new RawJob("france-travail", "FT-1", "Ingénieur DevOps", "Société X",
                "69 - Lyon", "https://x/1", "Kubernetes Terraform AWS observabilité",
                Instant.now(), "Contrat à durée indéterminée", "Annuel de 50000 à 60000 Euros",
                null, List.of());

        Offer o = normalizer.normalize(raw);

        assertEquals(ContractType.CDI, o.contractType());
        assertEquals("Lyon", o.city());
        assertEquals("France", o.country());
        assertEquals(50000, o.salaryMin());
        assertEquals(60000, o.salaryMax());
        assertNull(o.tjmMin());
        assertTrue(o.stack().contains("kubernetes"));
        assertTrue(o.stack().contains("terraform"));
        assertEquals("EUR", o.currency());
    }

    @Test
    void normalise_un_freelance_remote_avec_tjm() {
        RawJob raw = new RawJob("remotive", "R-1", "Architecte Cloud Freelance", "Acme",
                "Full remote", "https://x/2", "AWS GCP mission longue",
                Instant.now(), "Freelance", "TJM 600-750 / jour", "remote", List.of("aws"));

        Offer o = normalizer.normalize(raw);

        assertEquals(ContractType.FREELANCE, o.contractType());
        assertEquals(100, o.remotePercent());
        assertEquals(600, o.tjmMin());
        assertEquals(750, o.tjmMax());
        assertNull(o.salaryMin());
    }

    @Test
    void dedup_key_stable_par_titre_entreprise_ville() {
        RawJob a = new RawJob("adzuna", "1", "Architecte Cloud", "BigCorp", "Lyon, Rhône",
                "u", "d", Instant.now(), "permanent", null, null, List.of());
        RawJob b = new RawJob("france-travail", "2", "architecte  cloud", "BIGCORP", "69 - Lyon",
                "u2", "d2", Instant.now(), "CDI", null, null, List.of());

        assertEquals(normalizer.normalize(a).dedupKey(), normalizer.normalize(b).dedupKey());
    }
}
