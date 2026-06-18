package fr.cachi.emplois.infrastructure.config;

import fr.cachi.emplois.application.DedupService;
import fr.cachi.emplois.application.OfferEnricher;
import fr.cachi.emplois.application.OfferNormalizer;
import fr.cachi.emplois.application.ProfileService;
import fr.cachi.emplois.application.ScanService;
import fr.cachi.emplois.application.ScoringService;
import fr.cachi.emplois.domain.port.LlmProvider;
import fr.cachi.emplois.domain.port.ProfileRepository;
import fr.cachi.emplois.domain.port.ScoredOfferRepository;
import fr.cachi.emplois.infrastructure.llm.LlmProviders;
import fr.cachi.emplois.infrastructure.persistence.DynamoOfferRepository;
import fr.cachi.emplois.infrastructure.persistence.DynamoProfileRepository;
import fr.cachi.emplois.infrastructure.persistence.DynamoScanRunRepository;
import fr.cachi.emplois.infrastructure.persistence.DynamoScoredOfferRepository;
import fr.cachi.emplois.infrastructure.persistence.DynamoSeenOfferRepository;
import fr.cachi.emplois.infrastructure.source.JobSources;
import fr.cachi.emplois.infrastructure.telegram.TelegramNotifier;

/**
 * Câblage des composants pour les handlers Lambda (composition root côté infrastructure).
 * Construit à la demande ; réutilisable entre invocations chaudes.
 */
public final class Wiring {

    private Wiring() {
    }

    public static ProfileRepository profileRepository() {
        return new DynamoProfileRepository();
    }

    public static ProfileService profileService() {
        return new ProfileService(profileRepository());
    }

    public static ScoredOfferRepository scoredOfferRepository() {
        return new DynamoScoredOfferRepository();
    }

    public static ScanService scanService() {
        LlmProvider llm = LlmProviders.fromEnv();
        return new ScanService(
                JobSources.all(),
                new OfferNormalizer(),
                new OfferEnricher(llm),
                new DedupService(new DynamoSeenOfferRepository()),
                new ScoringService(llm),
                profileRepository(),
                new DynamoOfferRepository(),
                scoredOfferRepository(),
                new TelegramNotifier(),
                new DynamoScanRunRepository());
    }
}
