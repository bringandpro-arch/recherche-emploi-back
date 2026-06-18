# Mode d'emploi — déploiement de *recherche_emploi*

Guide pas à pas pour déployer l'application (backend AWS serverless + front Ionic) **de zéro**.
Conçu pour un déploiement `dev` mono-utilisateur, dans la région **`eu-west-3`** (Paris), avec
**Bedrock** en **`eu-west-1`** (Irlande). Budget cible : ~5–10 €/mois.

> Conventions : les commandes sont données pour un terminal (Git Bash **ou** PowerShell sauf mention
> contraire). Les valeurs entre `<…>` sont à remplacer.

---

## 0. Vue d'ensemble

```
Front Ionic/Angular ──HTTPS──> API Gateway HTTP ──> Lambda API ──> DynamoDB
   (Amplify Hosting)              (+ JWT Cognito)
                                                EventBridge (cron) ──> Lambda Scan ──> Sources web
                                                                                  └─> Bedrock (Claude)
                                                                                  └─> Telegram
Secrets ──> SSM Parameter Store (/recherche-emploi/dev/*)
```

Ordre de déploiement : **backend d'abord** (il produit les identifiants Cognito + l'URL d'API dont le
front a besoin), puis les **secrets**, puis le **front**.

---

## 1. Prérequis

### 1.1 Outils à installer (machine Windows)

| Outil | Vérifier | Installer (exemples) |
|---|---|---|
| **AWS CLI v2** | `aws --version` | MSI officiel, ou `choco install awscli` |
| **AWS SAM CLI** | `sam --version` | MSI officiel, ou `choco install aws-sam-cli` |
| **Java 21** | `java -version` | Temurin 21 (Adoptium) |
| **Maven** | `mvn -version` | `choco install maven` |
| **Node.js LTS (≥ 20)** | `node -v` | `choco install nodejs-lts` |
| **Ionic CLI** (front, dev local) | `ionic --version` | `npm i -g @ionic/cli` |

### 1.2 Compte AWS + credentials

Il faut un compte AWS et des credentials configurés **localement** (commande interactive — à lancer
toi-même) :

```
aws configure
```
Renseigne `Access Key`, `Secret Key`, région par défaut **`eu-west-3`**, format `json`.
(Si tu utilises AWS SSO : `aws sso login` après `aws configure sso`.)

Vérifier que ça répond :
```
aws sts get-caller-identity
```

### 1.3 Permission set SSO dédié (recommandé)

Pour des droits **scopés à ce projet** et indépendants des autres, créer un *permission set* dédié dans
**IAM Identity Center** plutôt que d'utiliser un compte large. La policy correspondante est versionnée
dans le fichier **`policy`** à la racine du projet (déploiement SAM + IAM/SSM/KMS scopés, SNS, CloudWatch,
Scheduler, Bedrock, Amplify).

1. **Créer le permission set** : IAM Identity Center → *Permission sets* → *Create* → *Custom* →
   nom `recherche-emploi-deploy` → à l'étape *Inline policy*, **coller le contenu du fichier `policy`**.
2. **Assigner** : IAM Identity Center → *AWS accounts* → compte cible → *Assign users or groups* →
   sélectionner **l'utilisateur (ou un groupe)** existant → choisir le permission set. *(Pas besoin de
   créer un nouvel utilisateur : un même user peut porter plusieurs permission sets.)*
3. **Configurer le profil CLI** (`~/.aws/config`) :
   ```ini
   [profile recherche-emploi]
   sso_session = <ta-session-sso>
   sso_account_id = <ID_DU_COMPTE>
   sso_role_name = recherche-emploi-deploy
   region = eu-west-3
   ```
   (Si pas encore de `sso_session` : `aws configure sso` une fois.)
4. **Se connecter et utiliser ce profil** :
   ```
   aws sso login --profile recherche-emploi
   aws sts get-caller-identity --profile recherche-emploi
   ```
   Puis pour toutes les commandes : `export AWS_PROFILE=recherche-emploi` (Git Bash) /
   `$env:AWS_PROFILE="recherche-emploi"` (PowerShell), ou ajouter `profile = "recherche-emploi"` sous
   `[default.deploy.parameters]` dans `samconfig.toml`, ou `--profile recherche-emploi` sur chaque commande.

> Limites connues : l'**activation du modèle Bedrock** (console *Model access*) peut exiger en plus des
> droits `aws-marketplace:Subscribe` (souvent admin) — à faire une fois par un compte admin si le toggle
> est grisé. La première connexion **Amplify ↔ GitHub** demande un consentement OAuth interactif.
> Pour des **domaines custom** (§10), ajouter `acm:*` et `route53:*` à la policy.

---

## 2. Récupérer les clés des sources externes

À faire **avant** ou **après** le déploiement (elles seront stockées en SSM à l'étape 4). Toutes sont
**gratuites**. Une source dont la clé n'est pas fournie reste simplement **désactivée** — l'app
fonctionne quand même avec les sources sans clé (Remotive, RemoteOK, The Muse, RSS).

| Source | Où obtenir | Ce qu'on récupère |
|---|---|---|
| **France Travail** | https://francetravail.io → créer un compte développeur → créer une application → souscrire à l'API **« Offres d'emploi v2 »** | `client_id` + `client_secret` |
| **Adzuna** | https://developer.adzuna.com → *Register* → *Get API access* | `app_id` + `app_key` |
| **Telegram** (notifications) | Dans Telegram, parler à **@BotFather** → `/newbot` | un **token** `123456:ABC-...` |
| **The Muse** *(optionnel)* | https://www.themuse.com/developers/api/v2 → demander une clé | `api_key` (facultatif, marche sans) |


### 2.1 Obtenir son `chat_id` Telegram

1. Dans Telegram, ouvrir la conversation avec **ton bot** (celui créé via BotFather), appuyer sur
   **Démarrer / Start**, puis lui envoyer un message quelconque (ex. `bonjour`). **Indispensable** :
   `getUpdates` ne renvoie rien tant que tu n'as pas écrit au bot.
2. Appeler (remplace `<TOKEN>`) :
   ```
   curl "https://api.telegram.org/bot<TOKEN>/getUpdates"
   ```
3. Dans la réponse, lire l'id du **dernier** update : pour un chat privé c'est
   `result[-1].message.chat.id` (entier **positif**) ; pour un **groupe** c'est aussi
   `message.chat.id` mais **négatif** ; pour un **canal**, c'est `result[-1].channel_post.chat.id`.

> **Si tu obtiens `{"ok":true,"result":[]}`** (réponse vide), dans l'ordre :
> 1. **Renvoie un nouveau message** au bot, puis rappelle `getUpdates` **immédiatement** —
>    Telegram ne garde les updates que ~24 h et un précédent `getUpdates` a pu avancer l'offset.
>    Pour relire le tout dernier update sans en renvoyer : `getUpdates?offset=-1`.
> 2. Vérifie qu'**aucun webhook** n'intercepte les updates (sinon `getUpdates` reste vide) :
>    `curl "https://api.telegram.org/bot<TOKEN>/getWebhookInfo"`. S'il y a une `url`, la retirer :
>    `curl "https://api.telegram.org/bot<TOKEN>/deleteWebhook"`, puis reprendre à l'étape 1.
> 3. Confirme que le **token** est le bon (un mauvais token donne `{"ok":false,...}`, pas un `result` vide).

> Le `chat_id` peut être posé **globalement** en SSM (`TELEGRAM_CHAT_ID`) **ou** par utilisateur dans
> son profil (champ *Telegram chat id* de l'écran Profil). Le profil prime sur le global.

---

## 3. Déployer le backend (`sam deploy`)

Le `template.yaml` référence le fat-jar Maven (`CodeUri: target/recherche-emploi-back.jar`) : on
**build d'abord**, puis on déploie.

```bash
cd C:/dev/projets/recherche_emploi/recherche-emploi-back

# 1) Build + tests (produit le fat-jar)
mvn clean package

# 2) Déploiement (utilise samconfig.toml : région eu-west-3, Stage=dev)
sam deploy
```

`sam deploy` affiche le **changeset** (Cognito User Pool, API HTTP, 6 tables DynamoDB, 2 Lambdas,
EventBridge Scheduler, rôles IAM) et demande confirmation (`confirm_changeset = true`). Tape `y`.

> Premier déploiement « propre » possible avec `sam deploy --guided` pour revoir les paramètres
> (`Stage`, `ScanSchedule`, `CorsOrigin`, `BedrockRegion`, `LlmModel`, `LogRetentionDays`, `AlarmEmail`).
> Sinon `sam deploy` suffit.
>
> Pour recevoir les alertes par email dès le déploiement (voir § 7 Observabilité) :
> ```
> sam deploy --parameter-overrides Stage=dev AlarmEmail=toi@exemple.fr
> ```

### 3.1 Récupérer les *Outputs* (indispensables pour le front)

À la fin du déploiement, SAM affiche les **Outputs**. Les revoir à tout moment :
```
aws cloudformation describe-stacks --stack-name recherche-emploi-back \
  --region eu-west-3 --query "Stacks[0].Outputs" --output table
```

| Output | Sert à | Exemple |
|---|---|---|
| `ApiUrl` | `apiBaseUrl` du front + tests | `https://abc123.execute-api.eu-west-3.amazonaws.com/dev` |
| `UserPoolId` | config Cognito du front | `eu-west-3_XXXXXXXXX` |
| `UserPoolClientId` | config Cognito du front | `xxxxxxxxxxxxxxxxxxxxxxxxxx` |

---

## 4. Créer les secrets dans SSM Parameter Store

Les connecteurs et Telegram lisent leurs secrets via la classe `Config` : **variable d'environnement
d'abord**, puis **SSM** `/recherche-emploi/<stage>/<CLÉ>` (SecureString, déchiffré KMS). CloudFormation
ne crée pas de SecureString : on les pose en CLI (région **eu-west-3**, même que la stack).

```bash
# France Travail
aws ssm put-parameter --type SecureString --region eu-west-3 --name /recherche-emploi/dev/FRANCE_TRAVAIL_CLIENT_ID     --value "<client_id>"
aws ssm put-parameter --type SecureString --region eu-west-3 --name /recherche-emploi/dev/FRANCE_TRAVAIL_CLIENT_SECRET --value "<client_secret>"

# Adzuna
aws ssm put-parameter --type SecureString --region eu-west-3 --name /recherche-emploi/dev/ADZUNA_APP_ID  --value "<app_id>"
aws ssm put-parameter --type SecureString --region eu-west-3 --name /recherche-emploi/dev/ADZUNA_APP_KEY --value "<app_key>"

# Telegram (token obligatoire pour notifier ; chat_id optionnel si renseigné dans le profil)
aws ssm put-parameter --type SecureString --region eu-west-3 --name /recherche-emploi/dev/TELEGRAM_BOT_TOKEN --value "<token>"
aws ssm put-parameter --type SecureString --region eu-west-3 --name /recherche-emploi/dev/TELEGRAM_CHAT_ID   --value "<chat_id>"

# Optionnels
aws ssm put-parameter --type SecureString --region eu-west-3 --name /recherche-emploi/dev/THE_MUSE_API_KEY --value "<cle>"
aws ssm put-parameter --type String       --region eu-west-3 --name /recherche-emploi/dev/RSS_FEEDS        --value "https://flux1.xml,https://flux2.xml"

# Sources additionnelles (F16+) — toutes optionnelles, le connecteur se désactive seul si absent
#  - Agrégateurs FR à clé/affid :
aws ssm put-parameter --type SecureString --region eu-west-3 --name /recherche-emploi/dev/JOOBLE_API_KEY  --value "<cle>"
aws ssm put-parameter --type SecureString --region eu-west-3 --name /recherche-emploi/dev/CAREERJET_AFFID --value "<affid>"
#  - Agrégateurs ATS (sans clé) : listes d'entreprises/boards à suivre (slugs séparés par des virgules) :
aws ssm put-parameter --type String --region eu-west-3 --name /recherche-emploi/dev/GREENHOUSE_BOARDS        --value "entreprise1,entreprise2"
aws ssm put-parameter --type String --region eu-west-3 --name /recherche-emploi/dev/LEVER_BOARDS             --value "entreprise1,entreprise2"
aws ssm put-parameter --type String --region eu-west-3 --name /recherche-emploi/dev/ASHBY_BOARDS             --value "entreprise1"
aws ssm put-parameter --type String --region eu-west-3 --name /recherche-emploi/dev/SMARTRECRUITERS_COMPANIES --value "entreprise1"
aws ssm put-parameter --type String --region eu-west-3 --name /recherche-emploi/dev/RECRUITEE_COMPANIES      --value "entreprise1"
#  - APIs publiques sans clé (Arbeitnow, Jobicy, Himalayas) : actives par défaut, rien à poser.
#    Pour en couper une : poser p.ex. ARBEITNOW_ENABLED=false (type String).
```

> **Mettre à jour** une valeur : ajouter `--overwrite`.
> Les secrets sont relus à la prochaine invocation **à froid** de la Lambda — **pas besoin de
> redéployer**. Pour forcer un rechargement immédiat, redéployer (`sam deploy`) ou attendre un cold start.

Vérifier ce qui est en place (valeurs masquées par défaut) :
```
aws ssm get-parameters-by-path --path /recherche-emploi/dev/ --region eu-west-3 --query "Parameters[].Name"
```

---

## 5. Activer l'accès au modèle Claude sur Bedrock

L'IA (scoring sémantique F7 + extraction F12) passe par **Amazon Bedrock** en **`eu-west-1`**.

1. Console AWS → région **Europe (Ireland) `eu-west-1`** → service **Bedrock**.
2. Menu **Model access** → **Manage / Enable models**.
3. Activer **Anthropic Claude Haiku 4.5** (ID modèle : `anthropic.claude-haiku-4-5`, valeur du paramètre
   `LlmModel`). Validation quasi immédiate.

> Sans cet accès, les appels IA échouent et le scan **retombe automatiquement sur le score de règles**
> (l'app reste fonctionnelle, juste moins fine). Pour désactiver volontairement l'IA : paramètre
> `LLM_PROVIDER=noop`.

---

## 6. Tester le backend

### 6.1 Endpoint public de santé
```
curl <ApiUrl>/health
```
Doit répondre `200`.

### 6.2 Déclencher un scan manuellement (sans attendre le cron)
Le scan tourne automatiquement selon `ScanSchedule` (défaut `rate(3 hours)`). Pour le lancer tout de suite :
```
aws lambda invoke --function-name recherche-emploi-scan-dev --region eu-west-3 --payload "{}" out.json
cat out.json
```
(Ou : console **Lambda** → `recherche-emploi-scan-dev` → onglet **Test** → événement vide `{}`.)

### 6.3 Lire les logs (français)
```
sam logs --stack-name recherche-emploi-back --name ScanFunction --region eu-west-3 --tail
```
On y voit le nombre de paramètres SSM chargés, les sources actives, les offres récupérées/notifiées.

---

## 7. Observabilité (inclus dans la stack, free tier)

Le `template.yaml` provisionne automatiquement :

- **Rétention des logs** : 30 jours par défaut sur les deux Lambdas (paramètre `LogRetentionDays`).
  Évite le piège du stockage CloudWatch *illimité* (coût qui grimpe en silence).
- **Tracing X-Ray** (`Tracing: Active`) : traces des invocations Lambda. Échantillonnage X-Ray par
  défaut (1 req/s + 5 %) → bien en dessous des 100 000 traces/mois gratuites.
- **5 alarmes CloudWatch** (métriques `AWS/Lambda` standard, gratuites) → publient sur un **topic SNS** :
  | Alarme | Déclenchement |
  |---|---|
  | `recherche-emploi-scan-errors-dev` | ≥ 1 erreur de scan / 5 min |
  | `recherche-emploi-scan-throttles-dev` | throttling du scan |
  | `recherche-emploi-scan-duration-dev` | scan > 240 s (timeout = 300 s) |
  | `recherche-emploi-api-errors-dev` | ≥ 1 erreur API / 5 min |
  | `recherche-emploi-api-throttles-dev` | throttling de l'API |
- **Dashboard CloudWatch** `recherche-emploi-dev` (invocations / erreurs / durée scan + API).

### 7.1 Recevoir les alertes par email

Si tu n'as pas passé `AlarmEmail` au déploiement, l'abonnement n'existe pas. Deux options :

- Redéployer avec l'email : `sam deploy --parameter-overrides Stage=dev AlarmEmail=toi@exemple.fr`
- Ou s'abonner ponctuellement au topic (ARN dans l'output `AlarmTopicArn`) :
  ```
  aws sns subscribe --region eu-west-3 --protocol email \
    --topic-arn <AlarmTopicArn> --notification-endpoint toi@exemple.fr
  ```
**Important** : AWS envoie un mail de **confirmation** d'abonnement → cliquer le lien, sinon aucune alerte.

### 7.2 Ouvrir le dashboard et les traces

- Dashboard : lien dans l'output `DashboardUrl` (console CloudWatch → *Dashboards* → `recherche-emploi-dev`).
- Traces X-Ray : console **CloudWatch → X-Ray traces** (ou *Service map*), région `eu-west-3`.

### 7.3 Coût

À l'échelle de ce projet, l'ensemble reste **dans le free tier** (métriques AWS standard gratuites,
< 5 Go de logs, < 100 000 traces, 10 alarmes et 3 dashboards offerts). Pour garder la maîtrise à plus
grande échelle : baisser `LogRetentionDays`, éviter les métriques custom haute cardinalité, et poser un
**AWS Budget** avec alerte de coût.

---

## 8. Déployer le front

### 8.1 Renseigner la configuration Cognito + API

Éditer **`recherche-emploi-front/src/environments/environment.prod.ts`** avec les *Outputs* de l'étape 3.1 :
```ts
export const environment = {
  production: true,
  apiBaseUrl: '<ApiUrl>',          // ex. https://abc123.execute-api.eu-west-3.amazonaws.com/dev
  cognito: {
    region: 'eu-west-3',
    userPoolId: '<UserPoolId>',
    userPoolClientId: '<UserPoolClientId>',
  },
};
```
(Pour le **dev local**, faire de même dans `environment.ts` ; `apiBaseUrl` peut rester `http://localhost:8080`
si tu fais tourner l'API en local, sinon mettre l'`ApiUrl` déployée.)

Commit + push (le front est versionné dans son propre repo `recherche-emploi-front`) :
```bash
cd C:/dev/projets/recherche_emploi/recherche-emploi-front
git add src/environments/environment.prod.ts
git commit -m "chore: configure Cognito et API pour l'environnement de prod"
git push origin master
```

### 8.2 Option A — Amplify Hosting (recommandé pour le web)

1. Console AWS → **Amplify** → **Host web app** → source **GitHub** → repo
   `bringandpro-arch/recherche-emploi-front`, branche `master`.
2. Amplify détecte **`amplify.yml`** (build : `npm ci` → `npm run build`, artefacts dans `www/`). Laisser tel quel.
3. Lancer le déploiement. Amplify fournit une URL `https://master.xxxx.amplifyapp.com`.
4. **CORS** : l'API autorise par défaut `https://emplois.cachi.fr` + `http://localhost:8100`. Tant que le
   domaine custom n'est pas branché, ajuster le paramètre `CorsOrigin` au domaine Amplify temporaire :
   ```
   sam deploy --parameter-overrides Stage=dev CorsOrigin=https://master.xxxx.amplifyapp.com
   ```

### 8.3 Option B — Tester en local
```bash
cd C:/dev/projets/recherche_emploi/recherche-emploi-front
npm install
ionic serve        # http://localhost:8100   (ou: npm start -> http://localhost:4200)
```

---

## 9. Créer le premier utilisateur et son profil

### 9.1 Via l'app (recommandé)
1. Ouvrir le front (URL Amplify ou `localhost`).
2. Onglet **« Créer un compte »** → email + mot de passe (politique Cognito : **≥ 10 caractères**, au
   moins une minuscule, une majuscule, un chiffre).
3. Un **code de vérification** est envoyé par email → le saisir dans l'écran de confirmation.
4. Se connecter, puis remplir l'écran **Profil** : compétences, type(s) de contrat (FREELANCE/CDI),
   localisations, % télétravail mini, TJM/salaire cible, mots-clés exclus, **seuil de notification**,
   **Telegram chat id** (optionnel). Enregistrer (`PUT /profile`).

### 9.2 Alternative — créer l'utilisateur en console / CLI
```
aws cognito-idp admin-create-user --user-pool-id <UserPoolId> --region eu-west-3 \
  --username <email> --user-attributes Name=email,Value=<email> Name=email_verified,Value=true
```
(puis définir un mot de passe permanent avec `admin-set-user-password`).

> Le scan ne traite que les **profils actifs**. Tant qu'aucun profil n'existe, le scan ne notifie rien.

---

## 10. (Optionnel) Domaines personnalisés + HTTPS

Cibles : web `emplois.cachi.fr`, API `api.emplois.cachi.fr`.

- **Web** : console Amplify → *Domain management* → ajouter `emplois.cachi.fr` (Amplify gère le
  certificat ACM et les enregistrements DNS — déléguer le domaine ou ajouter les CNAME fournis).
- **API** : API Gateway → *Custom domain names* → `api.emplois.cachi.fr` + certificat **ACM dans
  eu-west-3** → *API mapping* vers l'API HTTP, stage `dev`. Créer le CNAME DNS vers le domaine régional.
- Après bascule, repasser `CorsOrigin=https://emplois.cachi.fr` (valeur par défaut) et mettre
  `apiBaseUrl: 'https://api.emplois.cachi.fr'` dans `environment.prod.ts`.

> ⚠️ Vérifier l'orthographe du domaine possédé (`cachi.fr` vs `cachis.fr`) avant l'achat des certificats.

---

## 11. Ajuster la fréquence du scan

Le cron par défaut est `rate(3 hours)`. Pour changer (ex. toutes les 6 h) :
```
sam deploy --parameter-overrides Stage=dev ScanSchedule="rate(6 hours)"
```
Expressions possibles : `rate(N hours|minutes)` ou `cron(...)` (syntaxe EventBridge Scheduler).

---

## 12. Dépannage

| Symptôme | Piste |
|---|---|
| Front affiche « Cognito n'est pas configuré » | `userPoolId`/`userPoolClientId` vides dans `environment(.prod).ts` |
| Appels API en 401 | JWT absent/expiré ; vérifier qu'on est bien connecté et que l'`audience` = `UserPoolClientId` |
| Erreur CORS dans le navigateur | `CorsOrigin` ≠ origine réelle du front → redéployer avec la bonne valeur |
| Aucune offre France Travail/Adzuna | Secrets SSM absents/incorrects → voir étape 4 ; vérifier les logs scan (étape 6.3) |
| Pas de notification Telegram | `TELEGRAM_BOT_TOKEN` manquant, ou aucun `chat_id` (ni profil ni SSM), ou aucune offre ≥ seuil |
| Logs IA « repli sur le score de règles » | Accès Bedrock non activé (étape 5) ou région incorrecte |
| Pas d'email d'alerte CloudWatch | Abonnement SNS non confirmé (cliquer le lien reçu) ou `AlarmEmail` non fourni (étape 7.1) |
| `sam deploy` : bucket S3 | `resolve_s3 = true` gère le bucket ; sinon `sam deploy --guided` |

Logs détaillés (français) :
```
sam logs --stack-name recherche-emploi-back --name ScanFunction --region eu-west-3 --tail
sam logs --stack-name recherche-emploi-back --name ApiFunction  --region eu-west-3 --tail
```

---

## 13. Mettre à jour l'application

```bash
# Backend : rebuild + redeploy
cd C:/dev/projets/recherche_emploi/recherche-emploi-back && mvn clean package && sam deploy

# Front : push sur master -> Amplify rebuild automatiquement (si Hosting branché)
```

---

## 14. Tout supprimer (éviter les coûts)

```
sam delete --stack-name recherche-emploi-back --region eu-west-3
```
Supprime la stack (Lambdas, API, tables, Cognito, scheduler). **À part :**
- Les **paramètres SSM** ne sont pas dans la stack → les retirer à la main si besoin :
  ```
  aws ssm delete-parameters --region eu-west-3 --names /recherche-emploi/dev/ADZUNA_APP_ID /recherche-emploi/dev/ADZUNA_APP_KEY ...
  ```
- L'app **Amplify Hosting** se supprime depuis sa console.
- Les **certificats ACM** / entrées DNS éventuels.

---

## Récapitulatif express

```bash
# 1. Backend
cd recherche-emploi-back && mvn clean package && sam deploy
# 2. Noter ApiUrl / UserPoolId / UserPoolClientId (Outputs)
# 3. Secrets SSM (France Travail, Adzuna, Telegram) -> aws ssm put-parameter ...
# 4. Bedrock : activer Claude Haiku (console eu-west-1)
# 5. Front : remplir environment.prod.ts puis Amplify Hosting (ou ionic serve)
# 6. Créer un compte dans l'app + remplir le profil
# 7. Tester : curl <ApiUrl>/health  +  invoke scan manuel
```
```
Régions : stack/API/DynamoDB/SSM = eu-west-3 · Bedrock = eu-west-1
Stack : recherche-emploi-back · Scan Lambda : recherche-emploi-scan-dev
Secrets SSM : /recherche-emploi/dev/<CLÉ>
```
