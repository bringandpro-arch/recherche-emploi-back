package fr.cachi.emplois.application;

import fr.cachi.emplois.domain.model.ContractType;
import fr.cachi.emplois.domain.model.ExtractedFields;
import fr.cachi.emplois.domain.model.Offer;
import fr.cachi.emplois.domain.port.LlmProvider;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Enrichissement par extraction IA (F12) : complète les champs d'une {@link Offer} normalisée
 * que les heuristiques de la normalisation (F5) n'ont pas su renseigner (contrat, télétravail,
 * stack, rémunération, ville).
 *
 * <p><b>Maîtrise du coût</b> : l'IA n'est sollicitée que si l'offre présente des champs manquants
 * exploitables ({@link #needsEnrichment}) et si le provider est disponible. On ne remplace jamais
 * une valeur déjà déduite par les règles (l'IA ne fait que combler les trous).</p>
 */
public class OfferEnricher {

    private final LlmProvider llm;

    public OfferEnricher(LlmProvider llm) {
        this.llm = llm;
    }

    /** Renvoie l'offre enrichie, ou l'offre d'origine si l'IA est indisponible / inutile / en échec. */
    public Offer enrich(Offer offer) {
        if (llm == null || !llm.available() || !needsEnrichment(offer)) {
            return offer;
        }
        try {
            ExtractedFields ex = llm.extract(offer);
            if (ex == null || ex.isEmpty()) {
                return offer;
            }
            return merge(offer, ex);
        } catch (Exception e) {
            System.out.println("[enrich] extraction IA indisponible (" + offer.dedupKey() + ") : "
                    + e.getMessage());
            return offer;
        }
    }

    /** Vrai s'il manque au moins un champ clé qu'une extraction pourrait combler. */
    static boolean needsEnrichment(Offer o) {
        boolean noCompensation = o.tjmMin() == null && o.tjmMax() == null
                && o.salaryMin() == null && o.salaryMax() == null;
        return o.contractType() == null || o.contractType() == ContractType.UNKNOWN
                || o.remotePercent() == null
                || o.stack().isEmpty()
                || o.city() == null
                || noCompensation;
    }

    /** Fusionne les champs extraits dans l'offre : ne remplit que les valeurs manquantes. */
    static Offer merge(Offer o, ExtractedFields ex) {
        ContractType contract = o.contractType();
        if ((contract == null || contract == ContractType.UNKNOWN)
                && ex.contractType() != null && ex.contractType() != ContractType.UNKNOWN) {
            contract = ex.contractType();
        }

        Integer remote = o.remotePercent() == null ? ex.remotePercent() : o.remotePercent();

        // Union de la stack (préserve l'ordre, sans doublon, en minuscules comme la normalisation).
        Set<String> stack = new LinkedHashSet<>(o.stack());
        for (String t : ex.stack()) {
            if (t != null && !t.isBlank()) {
                stack.add(t.toLowerCase());
            }
        }

        boolean isFreelance = contract == ContractType.FREELANCE;
        Integer tjmMin = o.tjmMin() == null && isFreelance ? ex.tjmMin() : o.tjmMin();
        Integer tjmMax = o.tjmMax() == null && isFreelance ? ex.tjmMax() : o.tjmMax();
        Integer salaryMin = o.salaryMin() == null && !isFreelance ? ex.salaryMin() : o.salaryMin();
        Integer salaryMax = o.salaryMax() == null && !isFreelance ? ex.salaryMax() : o.salaryMax();

        String currency = o.currency();
        if (currency == null && (tjmMin != null || tjmMax != null || salaryMin != null || salaryMax != null)) {
            currency = ex.currency() != null ? ex.currency() : "EUR";
        }

        String city = o.city() == null ? ex.location() : o.city();
        String country = o.country();
        if (country == null && city != null) {
            country = "France";
        }

        return new Offer(
                o.source(), o.sourceExternalId(), o.title(), o.company(), o.locationRaw(),
                city, country, remote, contract,
                salaryMin, salaryMax, tjmMin, tjmMax, currency,
                new ArrayList<>(stack), o.url(), o.descriptionRaw(),
                o.publishedAt(), o.fetchedAt(), o.dedupKey());
    }
}
