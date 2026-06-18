package fr.cachi.emplois.domain.port;

import fr.cachi.emplois.domain.model.RawJob;
import fr.cachi.emplois.domain.model.SearchCriteria;

import java.util.List;

/**
 * Port d'ingestion : une source d'offres d'emploi (API officielle, RSS…).
 * Un connecteur par source ; activation par configuration.
 */
public interface JobSource {

    /** Code stable de la source ("france-travail", "adzuna", "remotive"…). */
    String code();

    /** La source est-elle activée (configuration / clés présentes) ? */
    boolean enabled();

    /**
     * Récupère les offres correspondant aux critères. En cas d'erreur réseau/source,
     * l'implémentation logge et renvoie une liste vide (le scan continue avec les autres sources).
     */
    List<RawJob> fetch(SearchCriteria criteria);
}
