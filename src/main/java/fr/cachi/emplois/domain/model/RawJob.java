package fr.cachi.emplois.domain.model;

import java.time.Instant;
import java.util.List;

/**
 * Offre « brute » telle que renvoyée par une source, avant normalisation (F5).
 *
 * @param source      code de la source ("france-travail", "adzuna", "remotive"…)
 * @param externalId  identifiant de l'offre chez la source
 * @param title       intitulé
 * @param company     entreprise
 * @param locationRaw localisation telle quelle (texte libre)
 * @param url         lien vers l'offre d'origine
 * @param description description (texte libre)
 * @param publishedAt date de publication (peut être null si non fournie)
 * @param contractRaw type de contrat tel quel (texte libre, nullable)
 * @param salaryRaw   rémunération telle quelle (texte libre, nullable)
 * @param remoteRaw   indication de télétravail telle quelle (texte libre, nullable)
 * @param tags        tags/technos éventuels fournis par la source
 */
public record RawJob(
        String source,
        String externalId,
        String title,
        String company,
        String locationRaw,
        String url,
        String description,
        Instant publishedAt,
        String contractRaw,
        String salaryRaw,
        String remoteRaw,
        List<String> tags
) {
    public RawJob {
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}
