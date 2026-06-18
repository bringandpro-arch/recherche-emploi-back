# État d'avancement

> Suivi continu **fait / en cours / à faire**. Mis à jour après chaque feature (voir [`PLAN.md`](./PLAN.md)).
> Légende : ✅ fait · 🟡 en cours · ⬜ à faire

_Dernière mise à jour : 2026-06-18_

## Phase 1 — Mode Plan

| Élément | Statut |
|---|---|
| Conception fonctionnelle (personas, parcours, features F1–F17) | ✅ |
| Conception technique (AWS serverless, modèle de données, sécurité, scheduling) | ✅ |
| Catalogue des sources (§5) | ✅ |
| `PLAN.md` (révisé post-arbitrages) | ✅ |
| `ETAT_AVANCEMENT.md` | ✅ |
| `CLAUDE.md` (back + front) | ✅ |
| `.claude/settings.json` (back + front) | ✅ |
| `.env.example` + `.gitignore` + `.gitattributes` (back + front) | ✅ |
| `prompts/` (templates LLM) | ✅ |
| Bloc unique d'arbitrages payants posé + **tranché** | ✅ |

### Arbitrages validés (2026-06-18)

- **Hébergement** : AWS serverless (Lambda + API Gateway + EventBridge + DynamoDB), IaC SAM. _Motif : coût ~0–2 €/mois + entraînement certifs AWS._
- **IA** : Amazon Bedrock / Claude Haiku (scoring + extraction), Sonnet pour le pitch. Budget plafonné ~3–8 €/mois.
- **Sources** : tiers gratuits uniquement.
- **Auth** : Amazon Cognito (multi-utilisateur dès le MVP).
- **Front** : AWS Amplify Hosting ; domaines `emplois.cachi.fr` (web) / `api.emplois.cachi.fr` (API) ; HTTPS ACM. _(à confirmer : cachi.fr vs cachis.fr)_
- **Mobile** : Capacitor (V1.1).
- **Budget total estimé** : ~5–10 €/mois (moins la 1ʳᵉ année).

## Phase 2 — Mode Développeur (MVP)

| # | Feature | Statut | Notes |
|---|---|---|---|
| F1 | Socle serverless + IaC (SAM, DynamoDB, API GW, Cognito, CloudWatch) | ✅ | pom.xml, template.yaml (6 tables, Cognito, API HTTP, scan planifié), handlers API+scan, /health, tests verts |
| F2 | Authentification Cognito (sign-up/login, JWT authorizer) | ✅ | User pool + authorizer JWT (template.yaml) + `AuthContext` extrait le `sub` ; routes protégées (401 sans JWT) |
| F3 | Profil de compétences (API + DynamoDB, scoping userId) | ✅ | `Profile`/`ProfileService`/`ProfileRepository` + impl. DynamoDB ; routes `GET`/`PUT /profile` scopées userId ; tests roundtrip verts |
| F4 | Interface `JobSource` + connecteurs (France Travail, Adzuna, Remotive) | ✅ | Interface `JobSource` + `RawJob`/`SearchCriteria` ; 3 connecteurs (OAuth2 FT, clés Adzuna, Remotive sans clé) ; registre `JobSources` (actifs selon config) ; parsing testé hors-ligne (4 tests) |
| F5 | Normalisation des offres | ✅ | `OfferNormalizer` : contrat (labels FR), remote %, ville (villes FR connues), salaire/TJM, stack (dictionnaire techs), `dedupKey` ; modèle `Offer` ; tests verts |
| F6 | Déduplication + historique | ✅ | `DedupService` (dédup intra-lot + `selectUnseen`/`markSeen` par userId) ; ports `OfferRepository`/`SeenOfferRepository` + impl. DynamoDB ; tests verts |
| F7 | Scoring (règles + IA Bedrock via `LlmProvider`) | ⬜ | |
| F8 | Scan périodique (EventBridge Scheduler → Lambda) | ⬜ | |
| F9 | Notification Telegram | ⬜ | |
| F10 | Filtrage freelance/CDI + localisation/remote | ⬜ | |
| F11 | Front Ionic + Amplify + domaine (login Cognito, profil, liste, détail) | ⬜ | |

## Phase 2 — V1.1+ (après MVP)

| # | Feature | Statut |
|---|---|---|
| F12 | IA — extraction structurée des offres | ⬜ |
| F13 | IA — détection CDI → mission freelance | ⬜ |
| F14 | IA — génération de pitch / lettre (Sonnet) | ⬜ |
| F15 | Feedback utilisateur → ré-apprentissage scoring | ⬜ |
| F16 | Sources additionnelles (Jooble, The Muse, Free-Work, RSS) | ⬜ |
| F17 | App mobile (Capacitor Android/iOS) | ⬜ |

## Journal

- **2026-06-18** — Phase 1 terminée et fichiers versionnés/poussés (back `06982f4`, front `2878103`).
  Arbitrages tranchés avec le commanditaire → bascule de la conception vers **AWS serverless + Cognito +
  Bedrock + Amplify + multi-utilisateur**. PLAN/ETAT révisés. Démarrage Phase 2 sur F1.
