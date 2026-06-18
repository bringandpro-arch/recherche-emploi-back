package fr.cachi.emplois.domain.model;

import java.util.List;

/**
 * Champs structurés extraits par l'IA (F12) à partir d'une description libre d'offre.
 *
 * <p>Sert à <b>enrichir</b> une {@link Offer} normalisée lorsque les heuristiques de la
 * normalisation (F5) n'ont pas su renseigner un champ. Tous les champs sont nullables :
 * un {@code null} signifie « l'IA n'a rien pu déduire » (on ne fabrique pas d'information).</p>
 *
 * @param contractType type de contrat déduit (nullable / {@link ContractType#UNKNOWN} si indéterminé)
 * @param remotePercent télétravail 0..100 (nullable)
 * @param stack technologies/outils détectés (jamais null, éventuellement vide)
 * @param tjmMin TJM min en euros (freelance, nullable)
 * @param tjmMax TJM max en euros (freelance, nullable)
 * @param salaryMin salaire annuel min en euros (CDI, nullable)
 * @param salaryMax salaire annuel max en euros (CDI, nullable)
 * @param currency devise (ex. "EUR", nullable)
 * @param location ville/pays déduit (nullable)
 */
public record ExtractedFields(
        ContractType contractType,
        Integer remotePercent,
        List<String> stack,
        Integer tjmMin,
        Integer tjmMax,
        Integer salaryMin,
        Integer salaryMax,
        String currency,
        String location
) {
    public ExtractedFields {
        stack = stack == null ? List.of() : List.copyOf(stack);
    }

    /** Résultat vide (aucune extraction). */
    public static ExtractedFields empty() {
        return new ExtractedFields(null, null, List.of(), null, null, null, null, null, null);
    }

    /** Aucune information exploitable. */
    public boolean isEmpty() {
        return contractType == null && remotePercent == null && stack.isEmpty()
                && tjmMin == null && tjmMax == null && salaryMin == null && salaryMax == null
                && currency == null && (location == null || location.isBlank());
    }
}
