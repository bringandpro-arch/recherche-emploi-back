package fr.cachi.emplois.domain.model;

import java.time.Instant;

/**
 * Offre scorée pour un utilisateur (résultat du scan : offre normalisée + score).
 *
 * @param userId   utilisateur destinataire
 * @param offer    offre normalisée
 * @param result   score (règles + IA)
 * @param scoredAt date du scoring
 */
public record ScoredOffer(
        String userId,
        Offer offer,
        ScoreResult result,
        Instant scoredAt
) {
}
