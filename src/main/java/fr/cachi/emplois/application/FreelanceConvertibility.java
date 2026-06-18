package fr.cachi.emplois.application;

import fr.cachi.emplois.application.support.Text;
import fr.cachi.emplois.domain.model.ContractType;
import fr.cachi.emplois.domain.model.Offer;

import java.util.List;

/**
 * Détection « CDI ouvrable en mission freelance » (F13) — couche déterministe.
 *
 * <p>Repère les offres (typiquement CDI ou contrat indéterminé) dont le contexte suggère
 * qu'une prestation en freelance/régie serait envisageable : vocabulaire ESN/conseil, régie,
 * mission, renfort, mention explicite d'indépendants. Sert de complément gratuit au signal IA
 * ({@code freelance_convertible}) : le scoring (F7) retient l'<b>union</b> des deux signaux.</p>
 *
 * <p>Le résultat est <b>indicatif</b> : il oriente, il ne garantit rien.</p>
 */
public final class FreelanceConvertibility {

    /** Indices forts d'un fonctionnement en prestation/régie. */
    private static final List<String> SIGNALS = List.of(
            "regie", "mission", "prestation", "prestataire", "consultant", "consulting",
            "esn", "ssii", "societe de conseil", "cabinet de conseil", "renfort",
            "freelance", "independant", "portage", "intercontrat", "tjm", "au forfait");

    private FreelanceConvertibility() {
    }

    /**
     * Vrai si l'offre, sans être déjà une mission freelance, présente des signaux d'ouverture
     * à une prestation. Une offre déjà {@link ContractType#FREELANCE} renvoie {@code false}
     * (rien à « convertir »).
     */
    public static boolean looksConvertible(Offer offer) {
        if (offer == null || offer.contractType() == ContractType.FREELANCE) {
            return false;
        }
        String haystack = Text.deaccentLower(
                (offer.title() == null ? "" : offer.title()) + " "
                        + (offer.company() == null ? "" : offer.company()) + " "
                        + (offer.descriptionRaw() == null ? "" : offer.descriptionRaw()));
        for (String signal : SIGNALS) {
            if (haystack.contains(signal)) {
                return true;
            }
        }
        return false;
    }
}
