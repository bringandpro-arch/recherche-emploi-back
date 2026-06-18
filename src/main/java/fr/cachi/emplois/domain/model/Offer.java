package fr.cachi.emplois.domain.model;

import java.time.Instant;
import java.util.List;

/**
 * Offre normalisée (modèle commun, issu de la normalisation d'un {@link RawJob} — F5).
 *
 * @param source           code de la source
 * @param sourceExternalId identifiant chez la source
 * @param title            intitulé
 * @param company          entreprise
 * @param locationRaw      localisation d'origine (texte libre)
 * @param city             ville normalisée (nullable)
 * @param country          pays (nullable)
 * @param remotePercent    télétravail estimé 0..100 (nullable si indéterminé)
 * @param contractType     type de contrat normalisé
 * @param salaryMin        salaire annuel min en euros (CDI, nullable)
 * @param salaryMax        salaire annuel max en euros (CDI, nullable)
 * @param tjmMin           TJM min en euros (freelance, nullable)
 * @param tjmMax           TJM max en euros (freelance, nullable)
 * @param currency         devise (ex. "EUR", nullable)
 * @param stack            technologies détectées
 * @param url              lien d'origine
 * @param descriptionRaw   description brute
 * @param publishedAt      date de publication (nullable)
 * @param fetchedAt        date de récupération
 * @param dedupKey         clé de déduplication (titre|entreprise|ville normalisés)
 */
public record Offer(
        String source,
        String sourceExternalId,
        String title,
        String company,
        String locationRaw,
        String city,
        String country,
        Integer remotePercent,
        ContractType contractType,
        Integer salaryMin,
        Integer salaryMax,
        Integer tjmMin,
        Integer tjmMax,
        String currency,
        List<String> stack,
        String url,
        String descriptionRaw,
        Instant publishedAt,
        Instant fetchedAt,
        String dedupKey
) {
    public Offer {
        stack = stack == null ? List.of() : List.copyOf(stack);
    }
}
