# PLAN — Logiciel de recherche d'offres d'emploi

> Conception fonctionnelle + technique. Document de référence rédigé en **Phase 1 (Mode Plan)**,
> mis à jour après les arbitrages du commanditaire (architecture **AWS serverless**).
> Avancement réel suivi dans [`ETAT_AVANCEMENT.md`](./ETAT_AVANCEMENT.md).

---

## 0. Résumé exécutif

Application qui agrège des **offres d'emploi tech (CDI + freelance)** depuis des sources légales
(APIs officielles + RSS), les **normalise**, les **dédoublonne**, les **score** par rapport à un
**profil de compétences** saisi dans une app **Ionic**, puis **notifie via Telegram** les offres
pertinentes non encore vues. Scan **périodique automatique**. L'IA est isolée derrière une interface
et utilisée uniquement là où elle apporte une vraie valeur (scoring sémantique, extraction structurée,
détection CDI→freelance, génération de pitch).

Cible : architecte freelance senior (Cloud/DevOps/Observabilité/IA, Lyon) cherchant **freelance ET CDI**.
**Multi-utilisateur dès le MVP** (auth Cognito) — le commanditaire est le premier utilisateur.

**Décisions d'architecture (arbitrages validés)** :
- **Backend : AWS serverless** (Lambda + API Gateway + EventBridge Scheduler + DynamoDB), IaC **AWS SAM**.
  Choisi pour le coût quasi nul à cette échelle **et** la valeur d'entraînement (certifs AWS DVA/SAA).
- **Auth : Amazon Cognito** (user pool, login web + mobile).
- **IA : Amazon Bedrock** (Claude Haiku pour le scoring/extraction de masse), derrière l'interface `LlmProvider`.
- **Front : Ionic + Angular + Capacitor + Tailwind**, hébergé sur **AWS Amplify Hosting**.
- **Sources : tiers gratuits uniquement** (France Travail, Adzuna dev, Remotive…).
- **Domaine** : web `emplois.cachi.fr`, API `api.emplois.cachi.fr` (HTTPS via ACM).

**Budget estimé : ~5–10 €/mois** (infra ~0–2 € + Bedrock ~3–8 €, plafonné), moins la 1ʳᵉ année.

---

## 1. Conception fonctionnelle

### 1.1 Personas

| Persona | Description | Besoins clés |
|---|---|---|
| **P1 — Le commanditaire (Alex)** | Architecte freelance senior, Cloud/DevOps/Observabilité/IA, Lyon. Freelance (TJM) + CDI (cadre). | Recevoir vite des offres pertinentes, filtrer freelance/CDI, éviter le bruit et les doublons. |
| **P2 — Profil tech générique** | Dev / data / ops cherchant un poste. | Mêmes besoins, profil différent → conception générique + multi-utilisateur (Cognito). |
| **P3 — Administrateur (= Alex)** | Gère sources, clés, seuils. | Configurer sans toucher au code ; observer les scans (logs FR). |

### 1.2 Parcours utilisateur (MVP)

1. **Inscription / connexion** (Cognito) sur `emplois.cachi.fr`.
2. **Onboarding profil** → compétences, contrat (freelance/CDI), localisations (Paris, Lyon, full remote…),
   % remote, TJM/salaire cible, mots-clés exclus.
3. **Scan automatique** (EventBridge → Lambda) → fetch sources, normalise, dédoublonne, score.
4. **Notification Telegram** → offres au-dessus du seuil (lien + résumé + score).
5. **Consultation** → liste triée par score, filtres (freelance/CDI, localisation, remote), détail + lien source.
6. **Feedback (post-MVP)** → pouce haut/bas affine le scoring.

### 1.3 Liste des features (numérotées, priorisées)

> Ordre = ordre de réalisation Phase 2. **MVP** = Definition of Done du brief (+ auth Cognito demandée).

| # | Feature | Priorité | Description |
|---|---|---|---|
| **F1** | Socle serverless + IaC | MVP | SAM template, DynamoDB, API Gateway, EventBridge, Cognito user pool, CloudWatch, build/deploy. |
| **F2** | Authentification Cognito | MVP | Sign-up/login, JWT authorizer sur l'API, scoping `userId`. |
| **F3** | Profil de compétences (API + DynamoDB) | MVP | CRUD profil par utilisateur (compétences, contrat, localisation, remote, TJM/salaire, exclusions). |
| **F4** | Interface `JobSource` + connecteurs | MVP | Contrat commun + connecteurs réels : France Travail, Adzuna, Remotive (≥ 2–3). |
| **F5** | Normalisation des offres | MVP | Mapping vers modèle commun (contrat, lieu, remote %, stack, salaire/TJM, devise). |
| **F6** | Déduplication + historique | MVP | Hash + matching flou (titre+entreprise+lieu) ; `SeenOffer` anti-re-notification. |
| **F7** | Scoring / matching | MVP | Règles déterministes + couche IA (Bedrock) derrière `LlmProvider`. Confiance **indicative, non probabiliste**. |
| **F8** | Scan périodique automatique | MVP | EventBridge Scheduler → Lambda : fetch → normalise → dédoublonne → score → notifie. |
| **F9** | Notification Telegram | MVP | Bot BotFather ; message lien + résumé + score ; pas de doublon. |
| **F10** | Filtrage freelance/CDI + localisation/remote | MVP | Filtres au scan et exposés à l'API/front. |
| **F11** | Front Ionic + Amplify + domaine | MVP | Login Cognito, profil, liste triée, filtres, détail. Amplify Hosting, `emplois.cachi.fr`, HTTPS. |
| **F12** | IA — extraction structurée | V1.1 | LLM extrait stack/TJM/remote des descriptions libres. |
| **F13** | IA — détection CDI → freelance | V1.1 | Heuristiques + LLM pour repérer les CDI « ouvrables » en mission. |
| **F14** | IA — génération de pitch / lettre | V1.2 | Pitch court personnalisé par offre, à la demande (Sonnet). |
| **F15** | Feedback utilisateur → ré-apprentissage scoring | V1.2 | Pouce haut/bas ajuste les poids. |
| **F16** | Sources additionnelles (Jooble, The Muse, Free-Work, RSS) | V1.1 | Élargissement du catalogue. |
| **F17** | App mobile (Capacitor Android/iOS) | V1.1 | Build natif de l'app Ionic (web déjà mobile-ready au MVP). |

---

## 2. Conception technique

### 2.1 Vue d'ensemble (AWS serverless)

```
        emplois.cachi.fr (Amplify Hosting + CloudFront + ACM)
   ┌─────────────────────────────┐
   │  Front Ionic / Angular      │   login Cognito (libs Amplify)
   │  Capacitor + Tailwind       │
   └──────────────┬──────────────┘
                  │ HTTPS  (api.emplois.cachi.fr)
        ┌─────────▼──────────────────────────────────────────────┐
        │  API Gateway (HTTP API) + Cognito JWT Authorizer        │
        └─────────┬───────────────────────────────────────────────┘
                  │
        ┌─────────▼─────────┐        ┌──────────────────────────────┐
        │  Lambda  (Java 21)│        │  EventBridge Scheduler (cron) │
        │  - API handlers   │        └───────────────┬──────────────┘
        │  - Scan handler   │ <──────────────────────┘ déclenche le scan
        └───┬───────┬───────┘
            │       │            ┌──────────────┐   ┌──────────────────┐
   ┌────────▼──┐ ┌──▼─────────┐  │ Sources web  │   │ Bedrock (Claude) │
   │ DynamoDB  │ │ SSM Params │  │ (API/RSS)    │   │ via LlmProvider  │
   │ (tables)  │ │ + secrets  │  └──────────────┘   └──────────────────┘
   └───────────┘ └────────────┘            │
                                   ┌────────▼────────┐
                                   │ Telegram Bot API│
                                   └─────────────────┘
```

### 2.2 Choix de stack (argumenté)

- **Compute : AWS Lambda (Java 21)**, handlers simples (aws-lambda-java-core/-events) + AWS SDK v2.
  Pas de VPC (DynamoDB/Bedrock/Telegram sont sur Internet → accès sortant par défaut, **pas de NAT Gateway**).
- **API : API Gateway HTTP API** + **autorizer JWT Cognito**. Moins cher/plus simple que REST API ; pas d'ALB.
- **Persistance : DynamoDB on-demand** (pas de RDS → pas de coût fixe). Modèle à quelques tables (§2.3).
- **Scheduling : EventBridge Scheduler** (cron configurable) → invoque la Lambda de scan.
- **Auth : Amazon Cognito** (user pool). JWT vérifié par l'authorizer API Gateway ; `userId` = `sub`.
- **IA : Amazon Bedrock** via l'interface `LlmProvider` (port). Impl. `BedrockLlmProvider` (Claude Haiku par
  défaut pour le scoring/extraction ; Sonnet pour le pitch). Impl. `NoopLlmProvider` pour désactiver/tester.
- **Secrets/config : SSM Parameter Store** (SecureString, gratuit pour params standard) — clés sources,
  token Telegram, paramètres de scan. Jamais en dur.
- **IaC : AWS SAM** (template.yaml) — proche des domaines d'examen DVA, simple à déployer (`sam build && sam deploy`).
- **Front : Ionic + Angular + Capacitor + Tailwind** (imposé), **Amplify Hosting** (CI/CD + CloudFront + ACM).
- **Observabilité : CloudWatch Logs** (logs FR), métriques de scan via `ScanRun`.

*Pourquoi serverless plutôt que Spring Boot/Hetzner* : à cette échelle le coût est négligeable des deux côtés ;
le serverless apporte ici une **valeur d'entraînement aux certifications AWS** (Lambda, API GW, DynamoDB,
EventBridge, Cognito, IAM, CloudWatch, SAM) demandée par le commanditaire.

### 2.3 Modèle de données (DynamoDB, multi-utilisateur)

Tables (on-demand) — `userId` = `sub` Cognito :

```
Profiles        PK=userId
  label, contractTypes[FREELANCE|CDI], locations[], remoteMin, targetTjmMin,
  targetSalaryMin, skills[{name,weight}], keywords[], excludedKeywords[],
  notifyThreshold, telegramChatId, active, createdAt, updatedAt

Offers          PK=dedupKey            (pool global d'offres normalisées)
  source, sourceExternalId, title, company, locationRaw, city, country,
  remotePercent, contractType, salaryMin, salaryMax, tjmMin, tjmMax, currency,
  stack[], url, descriptionRaw, descriptionExtracted(json IA), publishedAt, fetchedAt
  GSI1: source#sourceExternalId  (idempotence d'ingestion)

ScoredOffers    PK=userId  SK=offerDedupKey
  score(0..100), ruleScore, llmScore, reasons[], confidenceLabel, scoredAt
  GSI: userId + score  (tri par pertinence)

SeenOffers      PK=userId  SK=dedupKey  (historique anti-re-notification)
  firstSeenAt

ScanRuns        PK=scanId
  startedAt, endedAt, status, sourcesQueried, fetched, new, notified, errorSummary

Notifications   PK=userId  SK=sentAt#offerDedupKey
  channel(TELEGRAM), status, payloadHash
```

> Option « single-table design » possible plus tard (pattern avancé DVA/SAA) ; on démarre en multi-tables
> (plus lisible) vu le faible volume.

### 2.4 Ingestion pluggable — voir §5

```java
interface JobSource {
    String code();
    boolean enabled();
    List<RawJob> fetch(SearchCriteria criteria);
}
```

Un connecteur par source, activation par config (SSM). Module **scraping** isolé, **désactivé par défaut**,
basse fréquence, signalé, seulement pour sources sans API/RSS.

### 2.5 Normalisation, déduplication, historique

- **Normalisation (F5)** : champs hétérogènes → modèle commun (contrat, ville/pays, remote %, stack par
  mots-clés + IA optionnelle, salaire/TJM + devise).
- **Déduplication (F6)** : `dedupKey` = norm(titre)+norm(entreprise)+ville ; matching **flou**
  (Jaro-Winkler / token-set) pour les quasi-doublons inter-sources.
- **Historique (F6)** : `SeenOffers` empêche de re-notifier une offre déjà signalée (même via une autre source).

### 2.6 Scoring / matching (F7)

Score `0..100` = combinaison pondérée :
1. **Couche règles** (déterministe, gratuite) : recouvrement compétences/stack, type de contrat,
   localisation/remote, TJM/salaire vs cible, fraîcheur, mots-clés exclus. Sert aussi de **pré-filtre**.
2. **Couche IA** (Bedrock, derrière `LlmProvider`) : pertinence sémantique + justification courte.
   N'est appelée que pour les offres passant le pré-filtre → **maîtrise du coût**. Activable/désactivable,
   plafond mensuel configurable.

`confidenceLabel` (élevé/moyen/faible) = **indicatif**, explicitement **non statistique**.

### 2.7 Sécurité

- **Auth Cognito** (JWT) sur l'API ; isolation par `userId`.
- Secrets via **SSM Parameter Store SecureString** (KMS) ; jamais en dur, jamais commités. `.env.example` pour le local.
- **IAM least-privilege** par Lambda (accès ciblé DynamoDB / Bedrock / SSM / EventBridge).
- CORS restreint à `https://emplois.cachi.fr` (+ localhost en dev). HTTPS partout (ACM).
- Respect des CGU : **pas** de scraper naïf LinkedIn/Indeed/WTTJ/Malt. Scraping optionnel, désactivé.

### 2.8 Scheduling & déploiement

- **EventBridge Scheduler** (cron configurable, ex. toutes les 3 h) → Lambda de scan.
- **Déploiement** : `sam build && sam deploy` (CloudFormation). Front : `amplify`/console Amplify branchée
  sur le repo Git (CI/CD), domaine `emplois.cachi.fr`. API : custom domain `api.emplois.cachi.fr` + ACM.
- Logs **en français**, lisibles (CloudWatch).

### 2.9 Conventions

- **Commits** Conventional Commits, atomiques, push après chaque feature, sur les deux repos.
- **Windows / fins de ligne** : `.gitattributes` (LF), chemins relatifs.
- Logs/statuts **en français**. Détails par repo dans les `CLAUDE.md`.

---

## 3. Découpage Phase 2

Réalisation feature par feature F1 → F11 (MVP), puis F12+. Après chaque feature : mise à jour de
[`ETAT_AVANCEMENT.md`](./ETAT_AVANCEMENT.md), commit conventionnel, push sur les deux repos.

---

## 4. Décisions retenues (arbitrages tranchés)

- Backend **AWS serverless** (Lambda Java 21 + API Gateway + EventBridge + DynamoDB), IaC **SAM**.
- Auth **Cognito** (multi-utilisateur dès le MVP).
- IA **Bedrock / Claude Haiku** (scoring + extraction), Sonnet pour le pitch, derrière `LlmProvider`. Budget plafonné.
- Sources **gratuites** (France Travail, Adzuna dev, Remotive au MVP).
- Front **Amplify Hosting**, domaine **`emplois.cachi.fr`** (web) / **`api.emplois.cachi.fr`** (API), HTTPS ACM.
- App **mobile** via Capacitor (V1.1 ; web mobile-ready au MVP).

---

## 5. Catalogue des sources de données

> Vérifier les conditions d'accès **à jour** à l'intégration. Chaque connecteur documente sa vérification réelle.

| Source | Accès | Gratuité | Couverture | Limites / notes |
|---|---|---|---|---|
| **France Travail** (ex Pôle Emploi) — API « Offres d'emploi v2 » | API officielle (OAuth2 client credentials) | Gratuit (inscription + appli portail dev) | Large CDI/CDD/alternance FR | Quotas ; scope à demander ; **source prioritaire MVP**. |
| **Adzuna** — API agrégateur | API officielle (`app_id` + `app_key`) | Free tier dev | Agrégateur multi-pays (FR) | Salaire estimé ; pagination ; **MVP**. |
| **Remotive** — remote tech | API publique JSON | Gratuit, sans clé | Remote tech | Anglophone ; **MVP** (full remote). |
| **RemoteOK** | API/JSON publique | Gratuit (attribution) | Remote tech | Rate limit ; V1.1. |
| **The Muse** | API officielle (clé gratuite) | Free tier | US/intl | V1.1. |
| **Jooble** | API (clé sur demande) | Free/dev | Agrégateur FR | Clé par e-mail ; V1.1. |
| **APEC** — cadre | Pas d'API publique | — | Cadres FR (cœur de cible) | RSS/scraping fragile → **module optionnel désactivé**. |
| **Free-Work** — freelance + CDI tech | RSS / à vérifier | À vérifier | Freelance + CDI tech FR | Vérifier API/RSS & CGU ; V1.1. |
| **Flux RSS divers** | RSS (connecteur générique) | Gratuit | Variable | Paramétrable ; V1.1. |
| LinkedIn / Indeed / WTTJ / Malt | ❌ Non | — | — | CGU restrictives → **exclus**. |

---

## 6. Fichiers versionnés

- `PLAN.md`, `ETAT_AVANCEMENT.md` (racine + copie dans le repo back pour versioning Git).
- `recherche-emploi-back/CLAUDE.md`, `recherche-emploi-front/CLAUDE.md`.
- `.claude/settings.json`, `.env.example`, `.gitignore`, `.gitattributes` (chaque repo).
- `recherche-emploi-back/src/main/resources/prompts/` — templates LLM (scoring, extraction), packagés dans le jar.
- À venir (Phase 2) : `template.yaml` (SAM), `samconfig.toml`, code Lambda, `amplify.yml`.

---

## 7. Arbitrages payants — TRANCHÉS

| Sujet | Décision | Coût |
|---|---|---|
| Backend / hébergement | **AWS serverless** (entraînement examen + coût négligeable) | ~0–2 €/mois infra |
| API LLM | **Bedrock / Claude Haiku** (scoring+extraction), Sonnet pour pitch, plafonné | ~3–8 €/mois |
| APIs job boards | **Tiers gratuits** uniquement | 0 € |
| Auth | **Cognito** | 0 € (10 000 MAU gratuits) |
| Hébergement web | **Amplify Hosting** | ~0–1 €/mois |
| Nom de domaine | Sous-domaines de **cachi.fr** (`emplois` / `api.emplois`) | déjà possédé |

**Budget total estimé : ~5–10 €/mois** (moins la 1ʳᵉ année). Pièges à 50 € (NAT Gateway, RDS, ALB) écartés par construction.
