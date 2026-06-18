package fr.cachi.emplois.application;

import fr.cachi.emplois.domain.model.ContractType;
import fr.cachi.emplois.domain.model.Offer;
import fr.cachi.emplois.domain.port.SeenOfferRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DedupServiceTest {

    private final InMemorySeen seen = new InMemorySeen();
    private final DedupService service = new DedupService(seen);

    @Test
    void deduplique_intra_lot_par_cle() {
        List<Offer> deduped = service.dedupeBatch(List.of(
                offer("k1", "remotive"), offer("k1", "adzuna"), offer("k2", "remotive")));
        assertEquals(2, deduped.size());
    }

    @Test
    void selectUnseen_filtre_les_offres_deja_vues_puis_markSeen() {
        List<Offer> offers = List.of(offer("k1", "x"), offer("k2", "x"));
        service.markSeen("user-1", List.of(offer("k1", "x")));

        List<Offer> unseen = service.selectUnseen("user-1", offers);
        assertEquals(1, unseen.size());
        assertEquals("k2", unseen.get(0).dedupKey());

        // un autre utilisateur n'est pas impacté
        assertEquals(2, service.selectUnseen("user-2", offers).size());
    }

    private static Offer offer(String dedupKey, String source) {
        return new Offer(source, "id", "Titre", "Co", "Lyon", "Lyon", "France", 100,
                ContractType.CDI, 50000, 60000, null, null, "EUR", List.of(), "url", "desc",
                Instant.now(), Instant.now(), dedupKey);
    }

    static class InMemorySeen implements SeenOfferRepository {
        private final Set<String> store = new HashSet<>();
        public boolean isSeen(String userId, String dedupKey) { return store.contains(userId + "|" + dedupKey); }
        public void markSeen(String userId, String dedupKey, Instant when) { store.add(userId + "|" + dedupKey); }
    }
}
