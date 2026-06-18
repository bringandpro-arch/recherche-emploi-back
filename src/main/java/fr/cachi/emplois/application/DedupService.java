package fr.cachi.emplois.application;

import fr.cachi.emplois.domain.model.Offer;
import fr.cachi.emplois.domain.port.SeenOfferRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Déduplication (F6) : déduplication intra-lot par {@code dedupKey} et filtrage des offres
 * déjà notifiées à un utilisateur (historique {@link SeenOfferRepository}).
 */
public class DedupService {

    private final SeenOfferRepository seenRepository;

    public DedupService(SeenOfferRepository seenRepository) {
        this.seenRepository = seenRepository;
    }

    /** Déduplique un lot d'offres : conserve la première occurrence de chaque {@code dedupKey}. */
    public List<Offer> dedupeBatch(List<Offer> offers) {
        Map<String, Offer> byKey = new LinkedHashMap<>();
        for (Offer o : offers) {
            byKey.putIfAbsent(o.dedupKey(), o);
        }
        return new ArrayList<>(byKey.values());
    }

    /** Retourne les offres non encore vues par l'utilisateur (sans modifier l'historique). */
    public List<Offer> selectUnseen(String userId, List<Offer> offers) {
        List<Offer> result = new ArrayList<>();
        for (Offer o : offers) {
            if (!seenRepository.isSeen(userId, o.dedupKey())) {
                result.add(o);
            }
        }
        return result;
    }

    /** Marque les offres comme vues pour l'utilisateur (après notification). */
    public void markSeen(String userId, List<Offer> offers) {
        Instant now = Instant.now();
        for (Offer o : offers) {
            seenRepository.markSeen(userId, o.dedupKey(), now);
        }
    }
}
