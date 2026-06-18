package fr.cachi.emplois.domain.model;

import java.time.Instant;

/**
 * Résumé d'une exécution de scan (observabilité).
 *
 * @param scanId         identifiant du scan
 * @param startedAt      début
 * @param endedAt        fin
 * @param status         "OK" / "ERREUR"
 * @param sourcesQueried nombre de sources interrogées
 * @param fetched        offres brutes récupérées
 * @param newCount       offres nouvelles (non vues)
 * @param notified       notifications envoyées
 * @param errorSummary   éventuel résumé d'erreurs
 */
public record ScanRun(
        String scanId,
        Instant startedAt,
        Instant endedAt,
        String status,
        int sourcesQueried,
        int fetched,
        int newCount,
        int notified,
        String errorSummary
) {
}
