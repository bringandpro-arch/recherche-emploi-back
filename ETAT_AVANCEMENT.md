# État d'avancement

> Suivi continu **fait / en cours / à faire**. Mis à jour après chaque feature (voir [`PLAN.md`](./PLAN.md)).
> Légende : ✅ fait · 🟡 en cours · ⬜ à faire

_Dernière mise à jour : 2026-06-18_

## Phase 1 — Mode Plan

| Élément | Statut |
|---|---|
| Conception fonctionnelle (personas, parcours, features F1–F16) | ✅ |
| Conception technique (architecture, stack, modèle de données, sécurité, scheduling) | ✅ |
| Catalogue des sources (§5) | ✅ |
| `PLAN.md` | ✅ |
| `ETAT_AVANCEMENT.md` | ✅ |
| `CLAUDE.md` (back + front) | ✅ |
| `.claude/settings.json` (back + front) | ✅ |
| `.env.example` + `.gitignore` + `.gitattributes` (back + front) | ✅ |
| `prompts/` (templates LLM) | ✅ |
| Bloc unique d'arbitrages payants posé au commanditaire | ✅ |

## Phase 2 — Mode Développeur (MVP)

| # | Feature | Statut | Notes |
|---|---|---|---|
| F1 | Modèle de données + socle backend | ⬜ | Spring Boot 3 / Java 21 / Postgres / Flyway / Docker |
| F2 | Profil de compétences (API + persistance) | ⬜ | |
| F3 | Interface `JobSource` + connecteurs (≥ 2–3 réels) | ⬜ | France Travail, Adzuna, Remotive |
| F4 | Normalisation des offres | ⬜ | |
| F5 | Déduplication inter-sources + historique | ⬜ | |
| F6 | Scoring / matching (règles + IA) | ⬜ | `LlmProvider` |
| F7 | Scan périodique automatique | ⬜ | `@Scheduled` |
| F8 | Notification Telegram | ⬜ | |
| F9 | Filtrage freelance/CDI + localisation/remote | ⬜ | |
| F10 | Front Ionic (profil + liste + détail) | ⬜ | Ionic + Angular + Tailwind |

## Phase 2 — V1.1+ (après MVP)

| # | Feature | Statut |
|---|---|---|
| F11 | IA — extraction structurée des offres | ⬜ |
| F12 | IA — détection CDI → mission freelance | ⬜ |
| F13 | IA — génération de pitch / lettre | ⬜ |
| F14 | Feedback utilisateur → ré-apprentissage scoring | ⬜ |
| F15 | Sources additionnelles (Jooble, The Muse, Free-Work, RSS) | ⬜ |
| F16 | Authentification + multi-utilisateur | ⬜ |

## Journal

- **2026-06-18** — Phase 1 terminée : conception fonctionnelle + technique, catalogue sources, fichiers
  versionnés (PLAN, ETAT, CLAUDE x2, settings, env/gitignore, prompts). Bloc d'arbitrages payants posé.
  Démarrage Phase 2 sur les valeurs par défaut recommandées (Spring Boot/Hetzner, LLM Anthropic, sources gratuites).
