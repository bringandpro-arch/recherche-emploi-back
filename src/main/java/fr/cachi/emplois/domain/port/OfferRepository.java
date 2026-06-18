package fr.cachi.emplois.domain.port;

import fr.cachi.emplois.domain.model.Offer;

import java.util.Optional;

/** Port de persistance du pool global d'offres normalisées. */
public interface OfferRepository {

    /** Insère ou met à jour une offre (clé = dedupKey). */
    void upsert(Offer offer);

    Optional<Offer> findByDedupKey(String dedupKey);
}
