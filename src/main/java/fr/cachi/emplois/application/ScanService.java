package fr.cachi.emplois.application;

import fr.cachi.emplois.domain.model.Offer;
import fr.cachi.emplois.domain.model.Profile;
import fr.cachi.emplois.domain.model.RawJob;
import fr.cachi.emplois.domain.model.ScanRun;
import fr.cachi.emplois.domain.model.ScoreResult;
import fr.cachi.emplois.domain.model.ScoredOffer;
import fr.cachi.emplois.domain.model.SearchCriteria;
import fr.cachi.emplois.domain.port.JobSource;
import fr.cachi.emplois.domain.port.Notifier;
import fr.cachi.emplois.domain.port.OfferRepository;
import fr.cachi.emplois.domain.port.ProfileRepository;
import fr.cachi.emplois.domain.port.ScanRunRepository;
import fr.cachi.emplois.domain.port.ScoredOfferRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Orchestration du scan (F8) : pour chaque profil actif,
 * fetch (sources) → normalise → dédoublonne → score → persiste → notifie (F9) → historise.
 */
public class ScanService {

    private final List<JobSource> sources;
    private final OfferNormalizer normalizer;
    private final DedupService dedup;
    private final ScoringService scoring;
    private final ProfileRepository profiles;
    private final OfferRepository offers;
    private final ScoredOfferRepository scoredOffers;
    private final Notifier notifier;
    private final ScanRunRepository scanRuns;

    public ScanService(List<JobSource> sources, OfferNormalizer normalizer, DedupService dedup,
                       ScoringService scoring, ProfileRepository profiles, OfferRepository offers,
                       ScoredOfferRepository scoredOffers, Notifier notifier, ScanRunRepository scanRuns) {
        this.sources = sources;
        this.normalizer = normalizer;
        this.dedup = dedup;
        this.scoring = scoring;
        this.profiles = profiles;
        this.offers = offers;
        this.scoredOffers = scoredOffers;
        this.notifier = notifier;
        this.scanRuns = scanRuns;
    }

    /** Exécute un scan complet sur tous les profils actifs. */
    public ScanRun run() {
        String scanId = UUID.randomUUID().toString();
        Instant start = Instant.now();
        int fetched = 0;
        int newCount = 0;
        int notified = 0;
        List<String> errors = new ArrayList<>();

        List<JobSource> active = sources.stream().filter(JobSource::enabled).toList();
        log("Scan " + scanId + " : " + active.size() + " source(s) active(s)");

        for (Profile profile : profiles.findAllActive()) {
            try {
                SearchCriteria criteria = criteriaFor(profile);
                List<RawJob> raw = fetchAll(active, criteria, errors);
                fetched += raw.size();

                List<Offer> normalized = raw.stream().map(normalizer::normalize).toList();
                List<Offer> deduped = dedup.dedupeBatch(normalized);
                deduped.forEach(offers::upsert);

                List<Offer> unseen = dedup.selectUnseen(profile.userId(), deduped);
                newCount += unseen.size();

                List<ScoredOffer> scored = new ArrayList<>();
                for (Offer o : unseen) {
                    ScoreResult result = scoring.score(profile, o);
                    ScoredOffer so = new ScoredOffer(profile.userId(), o, result, Instant.now());
                    scoredOffers.save(so);
                    scored.add(so);
                }

                int threshold = profile.notifyThreshold() == null ? 60 : profile.notifyThreshold();
                List<ScoredOffer> toNotify = scored.stream()
                        .filter(s -> s.result().score() >= threshold)
                        .sorted(Comparator.comparingInt((ScoredOffer s) -> s.result().score()).reversed())
                        .toList();
                for (ScoredOffer s : toNotify) {
                    if (notifier.notify(profile, s)) {
                        notified++;
                    }
                }

                // Marque toutes les offres traitées comme vues (évite re-scoring/re-notification).
                dedup.markSeen(profile.userId(), unseen);
                log("Profil " + profile.userId() + " : " + unseen.size() + " nouvelle(s), "
                        + toNotify.size() + " notifiable(s)");
            } catch (Exception e) {
                errors.add("profil " + profile.userId() + " : " + e.getMessage());
                log("Erreur sur le profil " + profile.userId() + " : " + e);
            }
        }

        ScanRun run = new ScanRun(scanId, start, Instant.now(),
                errors.isEmpty() ? "OK" : "ERREUR", active.size(), fetched, newCount, notified,
                errors.isEmpty() ? null : String.join(" | ", errors));
        if (scanRuns != null) {
            try {
                scanRuns.save(run);
            } catch (Exception e) {
                log("Échec d'enregistrement du ScanRun : " + e.getMessage());
            }
        }
        log("Scan " + scanId + " terminé : fetched=" + fetched + ", new=" + newCount + ", notified=" + notified);
        return run;
    }

    /** Construit les critères de recherche à partir du profil (compétences + 1ʳᵉ localisation). */
    static SearchCriteria criteriaFor(Profile p) {
        String query = (p.skills() == null || p.skills().isEmpty())
                ? (p.label() == null ? "" : p.label())
                : String.join(" ", p.skills());
        String location = (p.locations() == null || p.locations().isEmpty()) ? null : p.locations().get(0);
        return SearchCriteria.of(query, location);
    }

    private List<RawJob> fetchAll(List<JobSource> active, SearchCriteria criteria, List<String> errors) {
        List<RawJob> all = new ArrayList<>();
        for (JobSource source : active) {
            try {
                all.addAll(source.fetch(criteria));
            } catch (Exception e) {
                errors.add("source " + source.code() + " : " + e.getMessage());
            }
        }
        return all;
    }

    private static void log(String msg) {
        System.out.println("[scan] " + msg);
    }
}
