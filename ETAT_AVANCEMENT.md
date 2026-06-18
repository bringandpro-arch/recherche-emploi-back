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
| F7 | Scoring (règles + IA Bedrock via `LlmProvider`) | ✅ | `ScoringService` (règles : skills/contrat/localisation/remote/rému/fraîcheur + pénalité mots exclus, pré-filtre IA) ; port `LlmProvider` + `BedrockLlmProvider`/`NoopLlmProvider` + `Prompts` (gabarits resources) ; tests verts |
| F8 | Scan périodique (EventBridge Scheduler → Lambda) | ✅ | `ScanService` orchestre fetch→normalise→dédup→score→persiste→notifie→historise ; `ScanHandler` câblé (`Wiring`) ; `ScanRun` persisté ; test e2e vert |
| F9 | Notification Telegram | ✅ | port `Notifier` + `TelegramNotifier` (sendMessage HTML, lien+résumé+score, anti-doublon via `SeenOffers`) |
| F10 | Filtrage freelance/CDI + localisation/remote | ✅ | `GET /offers` + `OffersQueryService`/`OffersFilter` (contrat, remoteMin, location, minScore) ; tri par score ; tests verts |
| F11 | Front Ionic + Amplify + domaine (login Cognito, profil, liste, détail) | ✅ | App Ionic 8 + Angular 20 standalone + Tailwind ; auth Cognito (Amplify) + guard ; pages login/profil/offres(filtres)/détail ; `ApiService` JWT ; `amplify.yml` ; build `www/` OK |

## Phase 2 — V1.1+ (après MVP)

| # | Feature | Statut | Notes |
|---|---|---|---|
| F12 | IA — extraction structurée des offres | ✅ | `ExtractedFields` + `LlmProvider.extract()` + prompt `extraction-user` + parsing Bedrock ; `OfferEnricher` comble les champs manquants (contrat, télétravail, stack, rému, ville) sur les offres **nouvelles** uniquement (coût maîtrisé), sans écraser l'existant ; câblé dans `ScanService`/`Wiring` |
| F13 | IA — détection CDI → mission freelance | ✅ | Heuristique déterministe `FreelanceConvertibility` (régie/ESN/mission/consultant…) **+** signal IA `freelance_convertible` ; union des deux dans `ScoringService`, raison explicite ; fonctionne même IA désactivée |
| F14 | IA — génération de pitch / lettre (Sonnet) | ⬜ | |
| F15 | Feedback utilisateur → ré-apprentissage scoring | ⬜ | |
| F16 | Sources additionnelles | ✅ | Connecteurs **The Muse** (JSON, clé optionnelle), **RemoteOK** (JSON, sans clé), **RSS générique** (`RSS_FEEDS`, parsing XML protégé XXE — couvre Free-Work & flux divers) ; enregistrés dans `JobSources` ; `.env.example` complété |
| F17 | App mobile (Capacitor Android/iOS) | ⬜ | |

## Journal

- **2026-06-18** — Phase 1 terminée et fichiers versionnés/poussés (back `ba75857`, front `57f2d52`).
  Arbitrages tranchés avec le commanditaire → bascule de la conception vers **AWS serverless + Cognito +
  Bedrock + Amplify + multi-utilisateur**. PLAN/ETAT révisés. Démarrage Phase 2 sur F1.
- **2026-06-18** — **MVP F1→F11 terminé.** Backend : socle SAM, auth Cognito, profil, 3 connecteurs,
  normalisation, dédup/historique, scoring (règles + Bedrock), scan orchestré, Telegram, filtres — **25 tests verts**.
  Front : app Ionic/Angular/Tailwind (login Cognito, profil, offres filtrables, détail) — build OK.
  Tout poussé sur les deux repos.
- **2026-06-18** — **V1.1 : F12, F13, F16 terminées (backend).** Extraction structurée IA (F12) avec
  enrichissement des offres nouvelles ; détection CDI→freelance (F13) heuristique + IA ; 3 nouvelles
  sources (F16 : The Muse, RemoteOK, RSS générique). **38 tests verts** (vs 25). Reste V1.1 : F14, F15, F17.
- **2026-06-18** — **Chargeur de secrets SSM** (`Config`) : les connecteurs (France Travail, Adzuna…)
  et Telegram résolvent désormais leurs secrets via variable d'env puis SSM Parameter Store
  (`/recherche-emploi/{stage}/*`, SecureString). Comble l'écart « secrets non câblés » repéré avant
  déploiement. **42 tests verts**.

### Reste à faire avant mise en production (hors périmètre code)
- Créer les secrets dans SSM (SecureString `/recherche-emploi/{stage}/*` : France Travail, Adzuna,
  token Telegram…) — chargés au runtime par `Config` (env d'abord, puis SSM). Commandes `aws ssm
  put-parameter` documentées dans `.env.example`. Renseigner aussi les sorties Cognito
  (`userPoolId`/`userPoolClientId`) dans les environnements front, après `sam deploy`.
- `sam deploy` (backend) + brancher Amplify Hosting sur le repo front + domaines `emplois.cachi.fr` /
  `api.emplois.cachi.fr`. Nécessite les credentials AWS du commanditaire.
- Activer Claude sur Bedrock (accès modèle) dans la région choisie.

## Suite (V1.1+) : F14 pitch · F15 feedback · F17 mobile
