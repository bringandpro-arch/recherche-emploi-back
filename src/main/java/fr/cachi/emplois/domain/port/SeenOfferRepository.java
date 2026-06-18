package fr.cachi.emplois.domain.port;

import java.time.Instant;

/** Port de l'historique « offres déjà vues » par utilisateur (anti-re-notification). */
public interface SeenOfferRepository {

    boolean isSeen(String userId, String dedupKey);

    void markSeen(String userId, String dedupKey, Instant when);
}
