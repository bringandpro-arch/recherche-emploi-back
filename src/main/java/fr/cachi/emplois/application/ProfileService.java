package fr.cachi.emplois.application;

import fr.cachi.emplois.domain.model.ContractType;
import fr.cachi.emplois.domain.model.Profile;
import fr.cachi.emplois.domain.port.ProfileRepository;

import java.time.Instant;
import java.util.List;

/** Cas d'usage autour du profil de recherche. */
public class ProfileService {

    private final ProfileRepository repository;

    public ProfileService(ProfileRepository repository) {
        this.repository = repository;
    }

    /** Récupère le profil de l'utilisateur, ou un profil par défaut s'il n'existe pas encore. */
    public Profile getOrDefault(String userId) {
        return repository.findByUserId(userId).orElseGet(() -> Profile.empty(userId));
    }

    /**
     * Crée ou met à jour le profil de l'utilisateur. {@code userId} fait foi (sécurité) :
     * le profil est toujours scopé à l'utilisateur authentifié.
     */
    public Profile save(String userId, Profile input) {
        Instant now = Instant.now();
        Instant createdAt = repository.findByUserId(userId)
                .map(Profile::createdAt)
                .orElse(now);

        Profile toSave = new Profile(
                userId,
                orDefault(input.label(), "Mon profil"),
                input.contractTypes() == null || input.contractTypes().isEmpty()
                        ? List.of(ContractType.FREELANCE, ContractType.CDI) : input.contractTypes(),
                input.locations() == null ? List.of() : input.locations(),
                input.remoteMin() == null ? 0 : input.remoteMin(),
                input.targetTjmMin(),
                input.targetSalaryMin(),
                input.skills() == null ? List.of() : input.skills(),
                input.keywords() == null ? List.of() : input.keywords(),
                input.excludedKeywords() == null ? List.of() : input.excludedKeywords(),
                input.notifyThreshold() == null ? 60 : clamp(input.notifyThreshold()),
                input.telegramChatId(),
                input.active(),
                createdAt,
                now);
        return repository.save(toSave);
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }
}
