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
 * Connecteur Adzuna — API agrégateur (FR), clés app_id + app_key (free tier dev).
 * Doc : https://developer.adzuna.com/
 */
public class AdzunaJobSource implements JobSource {

    public static final String CODE = "adzuna";
    private static final String BASE = "https://api.adzuna.com/v1/api/jobs/fr/search/1";

    private final String appId;
    private final String appKey;

    public AdzunaJobSource() {
        this(Config.get("ADZUNA_APP_ID"), Config.get("ADZUNA_APP_KEY"));
    }

    public AdzunaJobSource(String appId, String appKey) {
        this.appId = appId;
        this.appKey = appKey;
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public boolean enabled() {
        return appId != null && !appId.isBlank() && appKey != null && !appKey.isBlank()
                && !"false".equalsIgnoreCase(Config.get("ADZUNA_ENABLED"));
    }

    @Override
    public List<RawJob> fetch(SearchCriteria criteria) {
        try {
            StringBuilder url = new StringBuilder(BASE)
                    .append("?app_id=").append(HttpJson.enc(appId))
                    .append("&app_key=").append(HttpJson.enc(appKey))
                    .append("&results_per_page=").append(Math.max(1, criteria.limit()))
                    .append("&content-type=application/json");
            if (criteria.query() != null && !criteria.query().isBlank()) {
                url.append("&what=").append(HttpJson.enc(criteria.query()));
            }
            if (criteria.location() != null && !criteria.location().isBlank()) {
                url.append("&where=").append(HttpJson.enc(criteria.location()));
            }
            JsonNode root = HttpJson.get(url.toString(), null);
            return parse(root);
        } catch (Exception e) {
            System.out.println("[adzuna] échec de récupération : " + e.getMessage());
            return List.of();
        }
    }

    /** Parsing testable hors-ligne de la réponse Adzuna. */
    public static List<RawJob> parse(JsonNode root) {
        List<RawJob> jobs = new ArrayList<>();
        JsonNode arr = root == null ? null : root.get("results");
        if (arr == null || !arr.isArray()) {
            return jobs;
        }
        for (JsonNode j : arr) {
            String salary = null;
            JsonNode min = j.get("salary_min");
            JsonNode max = j.get("salary_max");
            if (min != null && !min.isNull()) {
                salary = (max != null && !max.isNull())
                        ? min.asText() + " - " + max.asText() : min.asText();
            }
            jobs.add(new RawJob(
                    CODE,
                    Nodes.text(j, "id"),
                    Nodes.text(j, "title"),
                    Nodes.text(j, "company", "display_name"),
                    Nodes.text(j, "location", "display_name"),
                    Nodes.text(j, "redirect_url"),
                    Nodes.text(j, "description"),
                    Nodes.parseInstant(Nodes.text(j, "created")),
                    Nodes.text(j, "contract_type"),
                    salary,
                    null,
                    List.of()));
        }
        return jobs;
    }
}
