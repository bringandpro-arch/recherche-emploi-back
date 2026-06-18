package fr.cachi.emplois.application;

import fr.cachi.emplois.application.support.Text;
import fr.cachi.emplois.domain.model.ContractType;
import fr.cachi.emplois.domain.model.LlmScore;
import fr.cachi.emplois.domain.model.Offer;
import fr.cachi.emplois.domain.model.Profile;
import fr.cachi.emplois.domain.model.ScoreResult;
import fr.cachi.emplois.domain.port.LlmProvider;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Scoring (F7) : score déterministe (règles) + couche IA optionnelle (LLM).
 *
 * <p>Les règles servent aussi de <b>pré-filtre</b> : l'IA n'est appelée que pour les offres
 * dépassant {@code prefilterThreshold} (maîtrise du coût). Score de confiance NON probabiliste.</p>
 */
public class ScoringService {

    /** En deçà de ce score de règles, on n'appelle pas l'IA (économie de tokens). */
    private static final int PREFILTER_THRESHOLD = 40;

    private final LlmProvider llm;

    public ScoringService(LlmProvider llm) {
        this.llm = llm;
    }

    public ScoreResult score(Profile profile, Offer offer) {
        List<String> reasons = new ArrayList<>();
        int rule = ruleScore(profile, offer, reasons);

        Integer llmScore = null;
        boolean freelanceConvertible = false;

        if (llm != null && llm.available() && rule >= PREFILTER_THRESHOLD) {
            try {
                LlmScore ai = llm.score(profile, offer);
                llmScore = clamp(ai.score());
                freelanceConvertible = ai.freelanceConvertible();
                if (ai.reasons() != null) {
                    reasons.addAll(ai.reasons());
                }
            } catch (Exception e) {
                reasons.add("IA indisponible (repli sur le score de règles)");
            }
        }

        int finalScore = llmScore == null ? rule : (int) Math.round(0.6 * rule + 0.4 * llmScore);
        finalScore = clamp(finalScore);
        return new ScoreResult(finalScore, rule, llmScore, reasons,
                ScoreResult.labelFor(finalScore), freelanceConvertible);
    }

    /** Score déterministe 0..100. Renseigne {@code reasons}. */
    int ruleScore(Profile profile, Offer offer, List<String> reasons) {
        String haystack = Text.deaccentLower(
                (offer.title() == null ? "" : offer.title()) + " "
                        + (offer.descriptionRaw() == null ? "" : offer.descriptionRaw()) + " "
                        + String.join(" ", offer.stack()));

        // Mots-clés rédhibitoires → pénalité forte.
        for (String ex : nullToEmpty(profile.excludedKeywords())) {
            if (!ex.isBlank() && haystack.contains(Text.deaccentLower(ex))) {
                reasons.add("Contient un mot-clé exclu : " + ex);
                return 5;
            }
        }

        int score = 0;

        // 1) Recouvrement compétences/stack (0..40)
        List<String> skills = nullToEmpty(profile.skills());
        if (!skills.isEmpty()) {
            long matched = skills.stream()
                    .filter(s -> haystack.contains(Text.deaccentLower(s)))
                    .count();
            int skillPoints = (int) Math.round(40.0 * matched / skills.size());
            score += skillPoints;
            reasons.add(matched + "/" + skills.size() + " compétences présentes");
        } else {
            score += 20;
        }

        // 2) Type de contrat (0..15)
        List<ContractType> wanted = nullToEmpty(profile.contractTypes());
        if (wanted.isEmpty() || wanted.contains(offer.contractType())) {
            score += 15;
            reasons.add("Type de contrat correspondant (" + offer.contractType() + ")");
        } else if (offer.contractType() == ContractType.UNKNOWN) {
            score += 7;
        }

        // 3) Localisation / télétravail (0..20)
        score += locationScore(profile, offer, reasons);

        // 4) Rémunération vs cible (0..15)
        score += compensationScore(profile, offer, reasons);

        // 5) Fraîcheur (0..10)
        score += freshnessScore(offer);

        return clamp(score);
    }

    private int locationScore(Profile profile, Offer offer, List<String> reasons) {
        Integer remoteMin = profile.remoteMin();
        if (remoteMin != null && offer.remotePercent() != null && offer.remotePercent() >= remoteMin) {
            reasons.add("Télétravail suffisant (" + offer.remotePercent() + "%)");
            return 20;
        }
        List<String> locations = nullToEmpty(profile.locations());
        String city = Text.deaccentLower(offer.city());
        boolean cityMatch = locations.stream()
                .anyMatch(l -> !l.isBlank() && city.contains(Text.deaccentLower(l)));
        if (cityMatch) {
            reasons.add("Localisation correspondante (" + offer.city() + ")");
            return 18;
        }
        boolean wantsRemote = locations.stream().anyMatch(l -> Text.deaccentLower(l).contains("remote")
                || Text.deaccentLower(l).contains("teletravail"));
        if (wantsRemote && offer.remotePercent() != null && offer.remotePercent() >= 80) {
            return 18;
        }
        return 6;
    }

    private int compensationScore(Profile profile, Offer offer, List<String> reasons) {
        Integer tjmTarget = profile.targetTjmMin();
        if (tjmTarget != null && offer.tjmMax() != null) {
            if (offer.tjmMax() >= tjmTarget) {
                reasons.add("TJM atteint (max " + offer.tjmMax() + "€)");
                return 15;
            }
            return 5;
        }
        Integer salaryTarget = profile.targetSalaryMin();
        if (salaryTarget != null && offer.salaryMax() != null) {
            if (offer.salaryMax() >= salaryTarget) {
                reasons.add("Salaire atteint (max " + offer.salaryMax() + "€)");
                return 15;
            }
            return 5;
        }
        return 7; // rémunération non renseignée → neutre
    }

    private int freshnessScore(Offer offer) {
        if (offer.publishedAt() == null) {
            return 5;
        }
        long days = Duration.between(offer.publishedAt(), Instant.now()).toDays();
        if (days <= 7) {
            return 10;
        }
        if (days <= 30) {
            return 5;
        }
        return 2;
    }

    private static <T> List<T> nullToEmpty(List<T> l) {
        return l == null ? List.of() : l;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(100, v));
    }
}
