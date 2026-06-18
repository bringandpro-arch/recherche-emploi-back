# CLAUDE.md — recherche-emploi-back

Backend **AWS serverless** de l'app de recherche d'offres d'emploi. Voir [`../PLAN.md`](../PLAN.md) pour la
conception globale et [`../ETAT_AVANCEMENT.md`](../ETAT_AVANCEMENT.md) pour l'avancement.

## Contexte

API + moteur de scan : agrège des offres (APIs officielles + RSS), normalise, dédoublonne, score par
rapport à un profil, notifie via Telegram. **Multi-utilisateur** (Cognito) dès le MVP.

## Stack

- **Java 21**, **AWS Lambda** (handlers `aws-lambda-java-core`/`-events`), **AWS SDK v2**, **Maven**.
- **Amazon API Gateway** (HTTP API) + **autorizer JWT Cognito**.
- **Amazon DynamoDB** (on-demand, Enhanced Client).
- **Amazon EventBridge Scheduler** (cron → Lambda de scan).
- **Amazon Bedrock** (Claude Haiku/Sonnet) derrière l'interface `LlmProvider`.
- **SSM Parameter Store** (SecureString) pour secrets/config.
- **CloudWatch Logs** (logs en français).
- **IaC : AWS SAM** (`template.yaml`, `samconfig.toml`).
- Telegram Bot API (HTTP).

## Architecture (hexagonale légère)

```
src/main/java/.../
  domain/          # entités, value objects, ports : JobSource, LlmProvider, Notifier, repositories
  application/     # services : ProfileService, ScanService, ScoringService, DedupService
  infrastructure/
    handler/       # handlers Lambda (API + scan)
    persistence/   # DynamoDB (Enhanced Client) + impl. repositories
    source/        # connecteurs JobSource (france-travail, adzuna, remotive, rss, ...)
    llm/           # BedrockLlmProvider, NoopLlmProvider
    telegram/      # TelegramNotifier
    config/        # chargement SSM / env, mapping
template.yaml      # SAM : Lambdas, API, DynamoDB, Cognito, Scheduler, IAM
```

## Commandes

```bash
mvn clean verify          # build + tests
sam build                 # build SAM (artefacts Lambda)
sam local invoke ...      # test local d'une Lambda
sam deploy                # déploiement (CloudFormation)
```

## Conventions

- Commits **Conventional Commits**, atomiques, push après chaque feature.
- Logs/statuts **en français**, lisibles (CloudWatch).
- **Secrets via SSM Parameter Store** (jamais en dur, jamais commités). `.env.example` = repère pour le local.
- **IAM least-privilege** par Lambda (DynamoDB / Bedrock / SSM / EventBridge ciblés).
- Connecteurs : implémenter `JobSource`, activation par config. **Pas de scraper naïf** des sites à CGU
  restrictives (LinkedIn/Indeed/WTTJ/Malt). Scraping = optionnel, désactivé, basse fréquence.
- LLM : tout appel passe par `LlmProvider`. Score de confiance IA **indicatif, non probabiliste**.
- **Coût** : pas de VPC pour les Lambda (pas de NAT Gateway), DynamoDB (pas de RDS), API Gateway (pas d'ALB).

## Do / Don't

- ✅ Ajouter une source = connecteur `JobSource` + config, sans toucher au domaine.
- ✅ Normaliser tous les champs ; dédoublonner avant de notifier ; historiser `SeenOffers`.
- ✅ Scoper toutes les données par `userId` (sub Cognito).
- ❌ Pas de secret commité. ❌ Pas d'appel LLM hors `LlmProvider`. ❌ Pas de scraping de source à CGU restrictives.
- ❌ Pas de NAT Gateway / RDS / ALB (pièges de coût).
