package fr.cachi.emplois.domain.port;

import fr.cachi.emplois.domain.model.ExtractedFields;
import fr.cachi.emplois.domain.model.LlmScore;
import fr.cachi.emplois.domain.model.Offer;
import fr.cachi.emplois.domain.model.Profile;

/**
 * Port LLM (provider remplaçable : Bedrock, autre…). Isole tout appel au modèle.
 * Le score retourné est une aide à la décision, <b>non probabiliste</b>.
 */
public interface LlmProvider {

    /** La couche IA est-elle disponible (configurée) ? */
    boolean available();

    /**
     * Évalue la pertinence sémantique d'une offre pour un profil.
     * Peut lever une exception (réseau/quotas) : l'appelant gère le repli sur le score de règles.
     */
    LlmScore score(Profile profile, Offer offer);

    /**
     * Extraction structurée (F12) : déduit les champs manquants (stack, TJM, télétravail…)
     * depuis la description libre de l'offre. Peut lever une exception (réseau/quotas) :
     * l'appelant gère le repli (offre laissée en l'état). Par défaut : aucune extraction.
     */
    default ExtractedFields extract(Offer offer) {
        return ExtractedFields.empty();
    }
}
