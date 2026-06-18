package fr.cachi.emplois.domain.model;

import java.time.Instant;
import java.util.List;

/**
 * Profil de recherche d'un utilisateur (compétences, contrat, localisation, rémunération cible…).
 *
 * @param userId          identifiant utilisateur (sub Cognito)
 * @param label           libellé du profil
 * @param contractTypes   types de contrat recherchés (FREELANCE / CDI…)
 * @param locations       localisations souhaitées (ex. "Lyon", "Paris", "Full remote")
 * @param remoteMin       télétravail minimum souhaité en %
 * @param targetTjmMin    TJM cible minimum (freelance), en euros
 * @param targetSalaryMin salaire annuel cible minimum (CDI), en euros
 * @param skills          compétences recherchées
 * @param keywords        mots-clés favorisés
 * @param excludedKeywords mots-clés rédhibitoires
 * @param notifyThreshold score minimum (0..100) pour déclencher une notification
 * @param telegramChatId  chat id Telegram de l'utilisateur (destination des notifications)
 * @param active          profil actif (scanné) ou non
 * @param createdAt       date de création
 * @param updatedAt       date de dernière mise à jour
 */
public record Profile(
        String userId,
        String label,
        List<ContractType> contractTypes,
        List<String> locations,
        Integer remoteMin,
        Integer targetTjmMin,
        Integer targetSalaryMin,
        List<String> skills,
        List<String> keywords,
        List<String> excludedKeywords,
        Integer notifyThreshold,
        String telegramChatId,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    /** Valeurs par défaut raisonnables pour un nouveau profil. */
    public static Profile empty(String userId) {
        Instant now = Instant.now();
        return new Profile(
                userId, "Mon profil",
                List.of(ContractType.FREELANCE, ContractType.CDI),
                List.of(), 0, null, null,
                List.of(), List.of(), List.of(),
                60, null, true, now, now);
    }
}
