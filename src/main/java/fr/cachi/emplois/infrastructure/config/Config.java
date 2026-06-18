package fr.cachi.emplois.infrastructure.config;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Configuration centralisée des secrets/paramètres.
 *
 * <p>Résout une clé en privilégiant la <b>variable d'environnement</b> (pratique en local et pour
 * surcharger ponctuellement), puis en repli sur <b>AWS SSM Parameter Store</b> : paramètres rangés
 * sous {@code /recherche-emploi/{stage}/<CLE>} (SecureString déchiffrés via KMS). La clé SSM (partie
 * après le préfixe) porte le même nom que la variable d'environnement, ex. {@code ADZUNA_APP_ID}.</p>
 *
 * <p>Le chargement SSM est <b>paresseux et mis en cache</b> (réutilisé entre invocations chaudes) et
 * ne se déclenche qu'en environnement Lambda. Toute défaillance SSM (droits, réseau, exécution locale)
 * est <b>non bloquante</b> : on retombe sur les variables d'environnement.</p>
 */
public final class Config {

    private static final String PREFIX_FORMAT = "/recherche-emploi/%s/";
    private static volatile Map<String, String> ssmCache;

    private Config() {
    }

    /** Valeur de la clé (env d'abord, puis SSM), ou {@code null} si absente/vide. */
    public static String get(String key) {
        return resolve(key, System::getenv, ssm());
    }

    /** Valeur de la clé, ou {@code def} si absente/vide. */
    public static String getOrDefault(String key, String def) {
        String v = get(key);
        return v == null || v.isBlank() ? def : v;
    }

    /** Résolution pure (testable) : variable d'environnement prioritaire, puis SSM. */
    static String resolve(String key, Function<String, String> env, Map<String, String> ssm) {
        String e = env.apply(key);
        if (e != null && !e.isBlank()) {
            return e;
        }
        String s = ssm == null ? null : ssm.get(key);
        return s == null || s.isBlank() ? null : s;
    }

    private static Map<String, String> ssm() {
        if (ssmCache == null) {
            synchronized (Config.class) {
                if (ssmCache == null) {
                    ssmCache = loadSsm();
                }
            }
        }
        return ssmCache;
    }

    /** Charge tous les paramètres sous le préfixe du stage. Hors Lambda : map vide (env seules). */
    private static Map<String, String> loadSsm() {
        if (System.getenv("AWS_LAMBDA_FUNCTION_NAME") == null
                || "true".equalsIgnoreCase(System.getenv("CONFIG_DISABLE_SSM"))) {
            return Map.of();
        }
        String stage = System.getenv().getOrDefault("STAGE", "dev");
        String prefix = String.format(PREFIX_FORMAT, stage);
        Region region = Region.of(System.getenv().getOrDefault("AWS_REGION", "eu-west-3"));
        Map<String, String> params = new HashMap<>();
        try (SsmClient client = SsmClient.builder().region(region).build()) {
            String next = null;
            do {
                GetParametersByPathResponse resp = client.getParametersByPath(
                        GetParametersByPathRequest.builder()
                                .path(prefix)
                                .recursive(true)
                                .withDecryption(true)
                                .nextToken(next)
                                .build());
                for (Parameter p : resp.parameters()) {
                    String name = p.name();
                    String key = name.startsWith(prefix) ? name.substring(prefix.length()) : name;
                    params.put(key, p.value());
                }
                next = resp.nextToken();
            } while (next != null && !next.isBlank());
            System.out.println("[config] " + params.size() + " paramètre(s) SSM chargé(s) depuis " + prefix);
        } catch (Exception e) {
            System.out.println("[config] SSM indisponible, repli sur les variables d'environnement : "
                    + e.getMessage());
            return Map.of();
        }
        return params;
    }
}
