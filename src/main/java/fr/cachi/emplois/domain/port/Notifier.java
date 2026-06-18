package fr.cachi.emplois.domain.port;

import fr.cachi.emplois.domain.model.Profile;
import fr.cachi.emplois.domain.model.ScoredOffer;

/** Port de notification (Telegram…). */
public interface Notifier {

    boolean enabled();

    /** Notifie une offre pertinente à l'utilisateur. Renvoie true si l'envoi a réussi. */
    boolean notify(Profile profile, ScoredOffer scored);
}
