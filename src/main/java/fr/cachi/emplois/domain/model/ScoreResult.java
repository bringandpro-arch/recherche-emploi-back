package fr.cachi.emplois.domain.model;

import java.util.List;

/**
 * Score final d'une offre pour un profil = combinaison règles + IA.
 *
 * <p>Le {@code confidenceLabel} est une aide à la lecture, explicitement <b>non statistique</b>.</p>
 *
 * @param score                score final 0..100
 * @param ruleScore            composante déterministe (règles)
 * @param llmScore             composante IA (nullable si IA non sollicitée)
 * @param reasons              justifications agrégées
 * @param confidenceLabel      "élevé" / "moyen" / "faible"
 * @param freelanceConvertible CDI potentiellement ouvrable en freelance
 */
public record ScoreResult(
        int score,
        int ruleScore,
        Integer llmScore,
        List<String> reasons,
        String confidenceLabel,
        boolean freelanceConvertible
) {
    public ScoreResult {
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }

    public static String labelFor(int score) {
        if (score >= 75) {
            return "élevé";
        }
        if (score >= 50) {
            return "moyen";
        }
        return "faible";
    }
}
