package fr.cachi.emplois.domain.port;

import fr.cachi.emplois.domain.model.ScoredOffer;

import java.util.List;

/** Port de persistance des offres scorées (pour la consultation / les filtres). */
public interface ScoredOfferRepository {

    void save(ScoredOffer scored);

    /** Offres scorées d'un utilisateur (les plus récentes/pertinentes en premier côté service). */
    List<ScoredOffer> listByUser(String userId);
}
