package fr.cachi.emplois.application;

import fr.cachi.emplois.domain.model.ContractType;

/**
 * Filtres de consultation des offres scorées (F10).
 *
 * @param contract  type de contrat (FREELANCE / CDI…), null = tous
 * @param remoteMin télétravail minimum (%), null = indifférent
 * @param location  filtre sur la ville (sous-chaîne), null = toutes
 * @param minScore  score minimum, null = aucun seuil
 */
public record OffersFilter(
        ContractType contract,
        Integer remoteMin,
        String location,
        Integer minScore
) {
    public static OffersFilter none() {
        return new OffersFilter(null, null, null, null);
    }
}
