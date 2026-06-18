# CLAUDE.md — recherche-emploi-back

Backend de l'app de recherche d'offres d'emploi. Voir [`../PLAN.md`](../PLAN.md) pour la conception
globale et [`../ETAT_AVANCEMENT.md`](../ETAT_AVANCEMENT.md) pour l'avancement.

## Contexte

API + moteur de scan : agrège des offres (APIs officielles + RSS), normalise, dédoublonne, score par
rapport à un profil, notifie via Telegram. Mono-utilisateur au départ, conçu pour extension.

## Stack

- **Java 21**, **Spring Boot 3**, **Maven**.
- **PostgreSQL** + **Flyway** (migrations).
- **Docker / Docker Compose** (déploiement VPS Hetzner).
- LLM derrière l'interface `LlmProvider` (impl. par défaut : Anthropic Claude, modèle configurable).
- Telegram Bot API (HTTP).
- Scheduler : `@Scheduled` (cron configurable via `scan.cron`).

## Architecture (hexagonale légère)

```
src/main/java/.../
  domain/          # entités métier, value objects, interfaces (ports) : JobSource, LlmProvider, Notifier
  application/     # services : ProfileService, ScanService, ScoringService, DedupService
  infrastructure/
    web/           # contrôleurs REST
    persistence/   # entités JPA + repositories
    source/        # connecteurs JobSource (france-travail, adzuna, remotive, rss, ...)
    llm/           # AnthropicLlmProvider, NoopLlmProvider
    telegram/      # TelegramNotifier
    config/        # configuration Spring
```

## Commandes

```bash
mvn clean verify           # build + tests
mvn spring-boot:run        # lancement local (profil dev)
docker compose up -d       # postgres + app (déploiement)
mvn -q -DskipTests package # jar
```

## Conventions

- Commits **Conventional Commits** (`feat:`, `fix:`, `chore:`, `docs:`, `refactor:`, `test:`), atomiques,
  push après chaque feature terminée.
- Logs et messages de statut **en français**, lisibles.
- **Secrets via variables d'environnement** (`.env`, voir `.env.example`). Jamais en dur, jamais commités.
- Connecteurs : implémenter `JobSource` ; activation par configuration. **Pas de scraper naïf** des sites
  à CGU restrictives (LinkedIn/Indeed/WTTJ/Malt). Module scraping = optionnel, désactivé, basse fréquence.
- LLM : tout appel passe par `LlmProvider`. Le score de confiance IA est **indicatif, non probabiliste**.

## Do / Don't

- ✅ Ajouter une source = nouveau connecteur `JobSource` + config, sans toucher au domaine.
- ✅ Normaliser tous les champs (contrat, lieu, remote %, stack, salaire/TJM) vers le modèle commun.
- ✅ Dédoublonner avant de notifier ; enregistrer l'historique `SeenOffer`.
- ❌ Ne pas committer de secret. ❌ Ne pas appeler un LLM hors de `LlmProvider`.
- ❌ Ne pas scraper de source à CGU restrictives.
