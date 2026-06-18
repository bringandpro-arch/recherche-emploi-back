package fr.cachi.emplois.infrastructure.source;

import com.fasterxml.jackson.databind.JsonNode;
import fr.cachi.emplois.domain.model.RawJob;
import fr.cachi.emplois.domain.model.SearchCriteria;
import fr.cachi.emplois.domain.port.JobSource;
import fr.cachi.emplois.infrastructure.config.Config;
import fr.cachi.emplois.infrastructure.json.Json;
import fr.cachi.emplois.infrastructure.source.http.HttpJson;
import fr.cachi.emplois.infrastructure.source.http.Nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Connecteur France Travail (ex Pôle Emploi) — API « Offres d'emploi v2 » officielle.
 * Auth OAuth2 client_credentials. Source prioritaire (large couverture CDI/CDD FR).
 * Doc : https://francetravail.io/
 */
public class FranceTravailJobSource implements JobSource {

    public static final String CODE = "france-travail";
    private static final String TOKEN_URL =
            "https://entreprise.francetravail.fr/connexion/oauth2/access_token?realm=%2Fpartenaire";
    private static final String SEARCH_URL =
            "https://api.francetravail.io/partenaire/offresdemploi/v2/offres/search";
    private static final String SCOPE = "api_offresdemploiv2 o2dsoffre";

    private final String clientId;
    private final String clientSecret;

    public FranceTravailJobSource() {
        this(Config.get("FRANCE_TRAVAIL_CLIENT_ID"), Config.get("FRANCE_TRAVAIL_CLIENT_SECRET"));
    }

    public FranceTravailJobSource(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public boolean enabled() {
        return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank()
                && !"false".equalsIgnoreCase(Config.get("FRANCE_TRAVAIL_ENABLED"));
    }

    @Override
    public List<RawJob> fetch(SearchCriteria criteria) {
        try {
            String token = fetchToken();
            StringBuilder url = new StringBuilder(SEARCH_URL).append("?range=0-")
                    .append(Math.max(0, criteria.limit() - 1));
            if (criteria.query() != null && !criteria.query().isBlank()) {
                url.append("&motsCles=").append(HttpJson.enc(criteria.query()));
            }
            JsonNode root = HttpJson.get(url.toString(), Map.of("Authorization", "Bearer " + token));
            return parse(root);
        } catch (Exception e) {
            System.out.println("[france-travail] échec de récupération : " + e.getMessage());
            return List.of();
        }
    }

    private String fetchToken() throws Exception {
        String body = HttpJson.postForm(TOKEN_URL, Map.of(
                "grant_type", "client_credentials",
                "client_id", clientId,
                "client_secret", clientSecret,
                "scope", SCOPE), null);
        JsonNode json = Json.mapper().readTree(body);
        return json.get("access_token").asText();
    }

    /** Parsing testable hors-ligne de la réponse France Travail. */
    public static List<RawJob> parse(JsonNode root) {
        List<RawJob> jobs = new ArrayList<>();
        JsonNode arr = root == null ? null : root.get("resultats");
        if (arr == null || !arr.isArray()) {
            return jobs;
        }
        for (JsonNode j : arr) {
            jobs.add(new RawJob(
                    CODE,
                    Nodes.text(j, "id"),
                    Nodes.text(j, "intitule"),
                    Nodes.text(j, "entreprise", "nom"),
                    Nodes.text(j, "lieuTravail", "libelle"),
                    Nodes.text(j, "origineOffre", "urlOrigine"),
                    Nodes.text(j, "description"),
                    Nodes.parseInstant(Nodes.text(j, "dateCreation")),
                    firstNonNull(Nodes.text(j, "typeContratLibelle"), Nodes.text(j, "typeContrat")),
                    Nodes.text(j, "salaire", "libelle"),
                    null,
                    List.of()));
        }
        return jobs;
    }

    private static String firstNonNull(String a, String b) {
        return a != null ? a : b;
    }
}
