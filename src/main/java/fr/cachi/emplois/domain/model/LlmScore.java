package fr.cachi.emplois.domain.model;

import java.util.List;

/**
 * Résultat d'évaluation sémantique par le LLM (couche IA du scoring).
 *
 * @param score                pertinence 0..100 (aide à la décision, NON probabiliste)
 * @param confidence           libellé de confiance ("élevé"/"moyen"/"faible")
 * @param reasons              justifications courtes
 * @param freelanceConvertible l'offre (souvent CDI) semble ouvrable en mission freelance
 */
public record LlmScore(
        int score,
        String confidence,
        List<String> reasons,
        boolean freelanceConvertible
) {
    public LlmScore {
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }
}
