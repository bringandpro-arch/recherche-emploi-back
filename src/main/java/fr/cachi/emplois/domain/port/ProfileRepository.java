package fr.cachi.emplois.domain.port;

import fr.cachi.emplois.domain.model.Profile;

import java.util.List;
import java.util.Optional;

/** Port de persistance des profils (implémenté côté infrastructure). */
public interface ProfileRepository {

    Optional<Profile> findByUserId(String userId);

    Profile save(Profile profile);

    /** Tous les profils actifs (utilisé par le scan). */
    List<Profile> findAllActive();
}
