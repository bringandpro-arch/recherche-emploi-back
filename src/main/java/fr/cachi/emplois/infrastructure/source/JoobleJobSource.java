package fr.cachi.emplois.infrastructure.source;

import com.fasterxml.jackson.databind.JsonNode;
import fr.cachi.emplois.domain.model.RawJob;
import fr.cachi.emplois.domain.model.SearchCriteria;
import fr.cachi.emplois.domain.port.JobSource;
import fr.cachi.emplois.infrastructure.config.Config;
import fr.cachi.emplois.infrastructure.source.http.HttpJson;
import fr.cachi.emplois.infrastructure.source.http.Nodes;

import java.util.ArrayList;
import java.util.List;

/**
 * Connecteur Jooble — agrégateur (FR), API REST avec clé gratuite (obtenue par e-mail).
 * Requête <b>POST JSON</b> sur {@code https://jooble.org/api/{key}}. Désactivé sans clé.
 * Doc : https://jooble.org/api/about
 */
public class JoobleJobSource implements JobSource {

    public static final String CODE = "jooble";
    private static final String BASE = "https://jooble.org/api/";

    private final String apiKey;

    public JoobleJobSource() {
        this(Config.get("JOOBLE_API_KEY"));
    }

    public JoobleJobSource(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public boolean enabled() {
        return apiKey != null && !apiKey.isBlank()
                && !"false".equalsIgnoreCase(Config.get("JOOBLE_ENABLED"));
    }

    @Override
    public List<RawJob> fetch(SearchCriteria criteria) {
        try {
            String keywords = criteria.query() == null ? "" : criteria.query();
            String location = criteria.location() == null ? "" : criteria.location();
            String body = "{\"keywords\":\"" + esc(keywords) + "\",\"location\":\"" + esc(location) + "\"}";
            JsonNode root = HttpJson.postJson(BASE + HttpJson.enc(apiKey), body, null);
            return parse(root);
        } catch (Exception e) {
            System.out.println("[jooble] échec de récupération : " + e.getMessage());
            return List.of();
        }
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /** Parsing testable hors-ligne de la réponse Jooble. */
    public static List<RawJob> parse(JsonNode root) {
        List<RawJob> jobs = new ArrayList<>();
        JsonNode arr = root == null ? null : root.get("jobs");
        if (arr == null || !arr.isArray()) {
            return jobs;
        }
        for (JsonNode j : arr) {
            jobs.add(new RawJob(
                    CODE,
                    Nodes.text(j, "id"),
                    Nodes.text(j, "title"),
                    Nodes.text(j, "company"),
                    Nodes.text(j, "location"),
                    Nodes.text(j, "link"),
                    Nodes.text(j, "snippet"),
                    Nodes.parseInstant(Nodes.text(j, "updated")),
                    Nodes.text(j, "type"),
                    Nodes.text(j, "salary"),
                    null,
                    List.of()));
        }
        return jobs;
    }
}
