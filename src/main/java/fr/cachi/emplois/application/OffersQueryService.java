package fr.cachi.emplois.application;

import fr.cachi.emplois.application.support.Text;
import fr.cachi.emplois.domain.model.Offer;
import fr.cachi.emplois.domain.model.ScoredOffer;
import fr.cachi.emplois.domain.port.ScoredOfferRepository;

import java.util.List;

/** Consultation filtrée des offres scorées d'un utilisateur (F10). */
public class OffersQueryService {

    private final ScoredOfferRepository repository;

    public OffersQueryService(ScoredOfferRepository repository) {
        this.repository = repository;
    }

    /** Offres de l'utilisateur, filtrées, triées par score décroissant. */
    public List<ScoredOffer> list(String userId, OffersFilter filter) {
        OffersFilter f = filter == null ? OffersFilter.none() : filter;
        return repository.listByUser(userId).stream()
                .filter(s -> matches(s, f))
                .toList();
    }

    static boolean matches(ScoredOffer s, OffersFilter f) {
        Offer o = s.offer();
        if (f.contract() != null && o.contractType() != f.contract()) {
            return false;
        }
        if (f.remoteMin() != null && (o.remotePercent() == null || o.remotePercent() < f.remoteMin())) {
            return false;
        }
        if (f.location() != null && !f.location().isBlank()) {
            String city = Text.deaccentLower(o.city());
            if (!city.contains(Text.deaccentLower(f.location()))) {
                return false;
            }
        }
        if (f.minScore() != null && s.result().score() < f.minScore()) {
            return false;
        }
        return true;
    }
}
