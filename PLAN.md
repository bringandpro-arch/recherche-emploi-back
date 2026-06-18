# PLAN — Logiciel de recherche d'offres d'emploi

> Conception fonctionnelle + technique. Document de référence rédigé en **Phase 1 (Mode Plan)**.
> Toute évolution majeure se reflète ici ; l'avancement réel est suivi dans [`ETAT_AVANCEMENT.md`](./ETAT_AVANCEMENT.md).

---

## 0. Résumé exécutif

Application qui agrège des **offres d'emploi tech (CDI + freelance)** depuis des sources légales
(APIs officielles + RSS), les **normalise**, les **dédoublonne**, les **score** par rapport à un
**profil de compétences** saisi dans une app **Ionic**, puis **notifie via Telegram** les offres
pertinentes non encore vues. Scan **périodique automatique**. L'IA est isolée derrière une interface
et utilisée uniquement là où elle apporte une vraie valeur (scoring sémantique, extraction structurée,
détection CDI→freelance, génération de pitch).

Cible initiale : **un seul utilisateur** (architecte freelance senior Cloud/DevOps/Observabilité/IA,
région Lyon), cherchant **missions freelance ET CDI**. Conçu proprement pour extension multi-utilisateurs.

---

## 1. Conception fonctionnelle

### 1.1 Personas

| Persona | Description | Besoins clés |
|---|---|---|
| **P1 — Le commanditaire (Alex)** | Architecte freelance senior, Cloud/DevOps/Observabilité/IA, Lyon. Cherche freelance (TJM) et CDI (cadre). | Recevoir vite des offres pertinentes, filtrer freelance/CDI, ne pas se noyer dans le bruit, ne pas revoir 2× la même offre. |
| **P2 — Profil tech générique (futur)** | Dev / data / ops cherchant un poste. | Mêmes besoins, paramétrage de profil différent. Justifie une conception générique. |
| **P3 — Administrateur (= Alex au départ)** | Gère les sources, les clés API, les seuils de scoring. | Configurer sans toucher au code ; observer les scans (logs lisibles en français). |

### 1.2 Parcours utilisateur (MVP)

1. **Onboarding profil** → l'utilisateur saisit compétences, types de contrat (freelance/CDI), localisations
   (Paris, Lyon, full remote…), % remote souhaité, TJM cible et/ou salaire cible, mots-clés à exclure.
2. **Scan automatique** (cron) → le backend interroge les sources, normalise, dédoublonne, score.
3. **Notification** → les offres au-dessus du seuil de pertinence partent sur Telegram (lien + résumé + score).
4. **Consultation** → l'utilisateur ouvre l'app Ionic, voit la liste triée par score, filtre (freelance/CDI,
   localisation, remote), ouvre le détail, clique vers l'offre source.
5. **Feedback (post-MVP)** → l'utilisateur marque « intéressé / pas intéressé », ce qui affine le scoring.

### 1.3 Liste des features (numérotées, priorisées)

> Ordre = ordre de réalisation en Phase 2. **MVP** = Definition of Done du brief. **V1.1+** = ensuite.

| # | Feature | Priorité | Description |
|---|---|---|---|
| **F1** | Modèle de données + socle backend | MVP | Entités, migrations, structure Spring Boot, config, Docker. |
| **F2** | Profil de compétences (API + persistance) | MVP | CRUD du profil (compétences, contrat, localisation, remote, TJM/salaire, exclusions). Mono-profil au départ. |
| **F3** | Interface `JobSource` + connecteurs | MVP | Contrat commun + connecteurs : France Travail, Adzuna, Remotive (≥ 2–3 réels). RSS générique en option. |
| **F4** | Normalisation des offres | MVP | Mapping vers un modèle commun (contrat, lieu, remote %, stack, salaire/TJM). |
| **F5** | Déduplication inter-sources | MVP | Hash + matching flou (titre + entreprise + localisation) ; historique « déjà vu ». |
| **F6** | Scoring / matching | MVP | Score de pertinence règles + couche IA (sémantique) derrière interface. Score de confiance ≠ probabilité. |
| **F7** | Scan périodique automatique | MVP | Scheduler (cron) : fetch → normalise → dédoublonne → score → notifie. |
| **F8** | Notification Telegram | MVP | Bot BotFather ; message lien + résumé + score ; pas de doublon de notif. |
| **F9** | Filtrage freelance/CDI + localisation/remote | MVP | Filtres appliqués au scan et exposés à l'API/front. |
| **F10** | Front Ionic (profil + liste + détail) | MVP | Saisie profil, liste triée par score, filtres, écran détail. |
| **F11** | IA — extraction structurée des offres | V1.1 | LLM extrait stack/TJM/remote des descriptions peu structurées. |
| **F12** | IA — détection CDI → mission freelance | V1.1 | Heuristiques + LLM pour repérer les CDI « ouvrables » en freelance. |
| **F13** | IA — génération de pitch / lettre | V1.2 | Pitch court personnalisé par offre (à la demande). |
| **F14** | Feedback utilisateur → ré-apprentissage du scoring | V1.2 | Pouce haut/bas, ajuste les poids. |
| **F15** | Sources additionnelles (Jooble, The Muse, Free-Work, RSS) | V1.1 | Élargissement du catalogue de connecteurs. |
| **F16** | Authentification + multi-utilisateur | V2 | Comptes, isolation des profils. Conçu mais non implémenté au MVP. |

---

## 2. Conception technique

### 2.1 Vue d'ensemble

```
┌────────────────────────┐        ┌──────────────────────────────────────────────┐
│  Front Ionic (Angular) │  HTTPS │            Backend Spring Boot                 │
│  Capacitor + Tailwind  │ <────> │  REST API  ──  Service Profil                  │
│  - écran Profil        │        │            ──  Service Scan (scheduler @cron)  │
│  - liste offres + score│        │            ──  Ingestion (connecteurs)         │
│  - filtres / détail    │        │            ──  Normalisation / Dédup           │
└────────────────────────┘        │            ──  Scoring (règles + LlmProvider)  │
                                   │            ──  Notifier Telegram               │
                                   └──────┬───────────────┬─────────────┬──────────┘
                                          │               │             │
                                   ┌──────▼─────┐  ┌───────▼──────┐ ┌────▼─────────┐
                                   │ PostgreSQL │  │ Sources web  │ │ LLM provider │
                                   │ (Docker)   │  │ (API/RSS)    │ │ (interface)  │
                                   └────────────┘  └──────────────┘ └──────────────┘
```

### 2.2 Choix de stack (argumenté)

**Backend : Spring Boot 3 (Java 21) sur VPS Hetzner existant — recommandé.**

- *Pourquoi pas AWS serverless* : pour un usage **mono-utilisateur** avec scans périodiques et un bot
  Telegram, le serverless ajoute de la complexité (Lambda cold start, packaging, IAM, DynamoDB single-table)
  sans bénéfice réel à ce volume. Le **VPS Hetzner existe déjà** → coût marginal ≈ nul, déploiement Docker simple,
  scheduler natif (`@Scheduled`), état relationnel facile (Postgres). Chiffrage comparatif au §7.
- Java 21 + Maven (présents sur le poste). Architecture hexagonale légère : `domain` / `application` /
  `infrastructure` (connecteurs, persistance, LLM, Telegram = adapters remplaçables).

**Frontend : Ionic + Capacitor + Angular + Tailwind** (imposé). Web + mobile (Android via Capacitor) depuis
une base unique. Tailwind pour le style ; composants Ionic pour l'UX mobile.

**Base de données : PostgreSQL** (Docker). Relationnel adapté au modèle (profil, offres, historique, scores).
Migrations via **Flyway**.

**IA : interface `LlmProvider`** (port hexagonal). Implémentation par défaut **Anthropic Claude**
(modèle configurable). Provider remplaçable (OpenAI, Mistral, local…) sans toucher au domaine.
Le score de confiance IA est explicitement présenté comme **indicatif, non probabiliste**.

**Scheduling : Spring `@Scheduled`** (cron configurable). Pas d'EventBridge nécessaire sur VPS.

**Déploiement : Docker Compose** sur le VPS (services : `app`, `postgres`). Reverse-proxy
(Caddy/Traefik) pour TLS si exposition publique du front/API.

### 2.3 Modèle de données (entités principales)

```
Profile
  id, label, contractTypes[FREELANCE|CDI], locations[], remoteMin(%), 
  targetTjmMin, targetSalaryMin, skills[], keywords[], excludedKeywords[], 
  notifyThreshold, active, createdAt, updatedAt

Skill            (id, profileId, name, weight)            -- pondération par compétence
JobSourceConfig  (id, code, enabled, rateLimitPerMin, lastRunAt, config(jsonb))

JobOffer
  id, source(code), sourceExternalId, title, company, locationRaw, city, country,
  remotePercent, contractType(FREELANCE|CDI|CDD|UNKNOWN), 
  salaryMin, salaryMax, tjmMin, tjmMax, currency, stack[], url, descriptionRaw,
  descriptionExtracted(jsonb, IA), publishedAt, fetchedAt, fingerprint(hash), dedupKey

ScoredOffer
  id, jobOfferId, profileId, score(0..100), ruleScore, llmScore, 
  reasons[], confidenceLabel, scoredAt

SeenOffer        (id, profileId, dedupKey, firstSeenAt)   -- historique anti-doublon
NotificationLog  (id, profileId, jobOfferId, channel(TELEGRAM), sentAt, status, payloadHash)
ScanRun          (id, startedAt, endedAt, status, sourcesQueried, fetched, new, notified, errorSummary)
```

Index clés : `JobOffer(dedupKey)`, `SeenOffer(profileId, dedupKey)`, `JobOffer(source, sourceExternalId)`.

### 2.4 Sources de données — voir §5 pour le détail. Ingestion **pluggable** :

```java
interface JobSource {
    String code();                       // "france-travail", "adzuna", ...
    boolean enabled();
    List<RawJob> fetch(SearchCriteria c); // critères dérivés du profil
}
```

Un connecteur par source. Module **scraping** isolé, **désactivé par défaut**, basse fréquence, clairement
signalé, uniquement pour sources sans API/RSS.

### 2.5 Normalisation, déduplication, historique

- **Normalisation (F4)** : mapping des champs hétérogènes → modèle commun (contrat, ville/pays, remote %,
  stack détectée par mots-clés + IA optionnelle, salaire/TJM avec devise).
- **Déduplication (F5)** : `dedupKey` = normalisation(titre) + normalisation(entreprise) + ville ; matching
  **flou** (Jaro-Winkler / token-set) pour rapprocher les quasi-doublons inter-sources.
- **Historique (F5)** : `SeenOffer` empêche de re-notifier une offre déjà signalée (même via une autre source).

### 2.6 Scoring / matching (F6)

Score final `0..100` = combinaison **pondérée** :
1. **Couche règles** (déterministe, rapide, gratuite) : recouvrement compétences/stack, type de contrat,
   localisation/remote, TJM/salaire vs cible, fraîcheur, mots-clés exclus.
2. **Couche IA** (sémantique, derrière `LlmProvider`) : pertinence sémantique profil↔offre + courte
   justification. Activable/désactivable et plafonnée en coût (cf. §7).

Le `confidenceLabel` (ex. « élevé / moyen / faible ») est **indicatif** et explicitement **non statistique**.

### 2.7 Sécurité

- Secrets via **variables d'environnement** / `.env` (jamais en dur, jamais commités). `.env.example` fourni.
- `.gitignore` strict (`.env`, secrets, build, `node_modules`, `target`).
- CORS restreint à l'origine du front. Pas d'auth au MVP (mono-utilisateur, API non exposée publiquement
  par défaut) ; emplacement prévu pour JWT en V2 (F16).
- Respect des CGU des sources : **pas** de scraper naïf LinkedIn/Indeed/WTTJ/Malt. Scraping optionnel
  uniquement, désactivé, respectueux.

### 2.8 Scheduling & déploiement

- `@Scheduled(cron = "${scan.cron}")` — fréquence configurable (ex. toutes les 2–4 h).
- Déploiement **Docker Compose** sur VPS Hetzner. `app` (jar) + `postgres`. Build front Ionic servi en
  statique (ou hébergé séparément). Logs **en français**, lisibles.

### 2.9 Conventions

- **Commits** : Conventional Commits (`feat:`, `fix:`, `chore:`, `docs:`…), atomiques, push après chaque feature.
- **Windows / fins de ligne** : `.gitattributes` pour normaliser LF côté scripts ; chemins relatifs côté code.
- Messages de log et de statut **en français**.
- Détails par repo dans les `CLAUDE.md` respectifs.

---

## 3. Découpage Phase 2 (réalisation)

Réalisation **feature par feature** dans l'ordre F1 → F10 (MVP), puis F11+. Après chaque feature :
mise à jour de [`ETAT_AVANCEMENT.md`](./ETAT_AVANCEMENT.md), commit conventionnel, push sur les deux repos.

---

## 4. Hypothèses & valeurs par défaut retenues (modifiables via §7)

- Backend **Spring Boot sur Hetzner** (recommandé) — construit par défaut sur cette base.
- LLM par défaut **Anthropic Claude**, modèle économique pour le scoring de masse, derrière interface.
- Sources de départ : **tiers gratuits** (France Travail, Adzuna dev, Remotive). Élargissement ensuite.
- Pas de nom de domaine au MVP (accès direct / IP / sous-domaine du VPS).

---

## 5. Catalogue des sources de données

> Vérifier les conditions d'accès **à jour** au moment de l'intégration. Statuts ci-dessous = état de
> conception ; chaque connecteur documente sa vérification réelle dans le code.

| Source | Accès | Gratuité | Couverture | Limites / notes |
|---|---|---|---|---|
| **France Travail** (ex Pôle Emploi) — API « Offres d'emploi v2 » | **API officielle** (OAuth2 client credentials) | Gratuit (inscription + appli sur le portail développeur) | Large CDI/CDD/alternance FR | Quotas d'appel ; scope à demander ; **source prioritaire MVP**. |
| **Adzuna** — API agrégateur | **API officielle** (`app_id` + `app_key`) | Free tier dev (quota requêtes/mois) | Agrégateur multi-pays (FR inclus) | Champs salaire estimés ; pagination ; **MVP**. |
| **Remotive** — remote tech | **API publique JSON** | Gratuit, sans clé (usage raisonnable) | Remote tech | Anglophone surtout ; **MVP** (full remote). |
| **RemoteOK** — remote tech | **API/JSON publique** | Gratuit (attribution demandée) | Remote tech | Respecter rate limit/CGU ; V1.1. |
| **The Muse** — jobs/tech | **API officielle** (clé gratuite) | Free tier | US/intl, filtrable | V1.1. |
| **Jooble** — agrégateur | **API** (clé sur demande) | Free/dev | Agrégateur FR inclus | Clé par e-mail ; V1.1. |
| **APEC** — emploi cadre | **Pas d'API publique officielle** | — | Cadres FR (cœur de cible) | RSS/scraping fragile → **module optionnel désactivé**, basse fréquence, signalé. |
| **Free-Work** — freelance + CDI tech | **RSS / à vérifier** | À vérifier | Freelance + CDI tech FR | Vérifier API/RSS & CGU ; V1.1. |
| **Flux RSS divers** (job boards) | **RSS** via connecteur générique | Gratuit | Variable | Connecteur RSS générique paramétrable ; V1.1. |
| LinkedIn / Indeed / WTTJ / Malt | ❌ **Non** | — | — | Contraire aux CGU, fragile, risqué → **exclus** (pas de scraper naïf). |

**Architecture d'ingestion** : interface `JobSource` commune, un connecteur par source, activation par
configuration (`JobSourceConfig`). Module scraping isolé et désactivé par défaut.

---

## 6. Fichiers créés (versionnés)

- `PLAN.md` (ce fichier) — racine.
- `ETAT_AVANCEMENT.md` — racine, suivi continu.
- `recherche-emploi-back/CLAUDE.md`, `recherche-emploi-front/CLAUDE.md`.
- `.claude/settings.json` (permissions) dans chaque repo.
- `.env.example` + `.gitignore` + `.gitattributes` dans chaque repo.
- `recherche-emploi-back/prompts/` — templates de prompts LLM (system / user).

---

## 7. Arbitrages payants — à valider (bloc unique)

Voir la question groupée posée au commanditaire à l'issue de la Phase 1. Synthèse :

1. **Backend / hébergement** — *Spring Boot sur Hetzner existant (coût marginal ≈ 0 €/mois)* **[recommandé]**
   vs *AWS serverless* (free tier puis ~quelques €/mois à l'usage, mais complexité accrue).
2. **API LLM (features IA)** — provider + modèle + budget. Repère de coût Anthropic Claude (au 2026-06) :
   Haiku 4.5 ≈ 1 $/MTok entrée, 5 $/MTok sortie ; Opus 4.8 ≈ 5 $/25 $. Scoring/extraction = petites
   requêtes ⇒ budget estimé **quelques € à ~15 €/mois** selon volume de scans. Plafond configurable.
3. **APIs job boards payantes** — démarrage sur tiers **gratuits** ; passage payant seulement si insuffisant.
4. **Nom de domaine** — optionnel ; non requis au MVP.
