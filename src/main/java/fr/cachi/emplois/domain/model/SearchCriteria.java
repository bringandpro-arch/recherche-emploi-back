package fr.cachi.emplois.domain.model;

/**
 * Critères de recherche transmis aux sources. Dérivés des profils actifs au moment du scan
 * (union des compétences/localisations), volontairement larges : le tri fin se fait au scoring (F7).
 *
 * @param query      mots-clés (ex. "devops cloud architecte")
 * @param location   localisation cible (ex. "Lyon"), nullable
 * @param remoteOnly ne demander que du télétravail
 * @param limit      nombre maximum de résultats par source
 */
public record SearchCriteria(
        String query,
        String location,
        boolean remoteOnly,
        int limit
) {
    public static SearchCriteria of(String query, String location) {
        return new SearchCriteria(query, location, false, 50);
    }
}
