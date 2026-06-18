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
 * Connecteur Jobicy — API publique JSON v2 (offres remote tech), sans clé.
 * Toutes les offres sont en télétravail. Doc : https://jobicy.com/jobs-rss-feed (API JSON v2).
 */
public class JobicyJobSource implements JobSource {

    public static final String CODE = "jobicy";
    private static final String BASE = "https://jobicy.com/api/v2/remote-jobs";

    private final boolean enabled;

    public JobicyJobSource() {
        this(!"false".equalsIgnoreCase(Config.get("JOBICY_ENABLED")));
    }

    public JobicyJobSource(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public List<RawJob> fetch(SearchCriteria criteria) {
        try {
            String url = BASE + "?count=" + Math.max(1, criteria.limit());
            if (criteria.query() != null && !criteria.query().isBlank()) {
                url += "&tag=" + HttpJson.enc(criteria.query());
            }
            JsonNode root = HttpJson.get(url, null);
            return parse(root);
        } catch (Exception e) {
            System.out.println("[jobicy] échec de récupération : " + e.getMessage());
            return List.of();
        }
    }

    /** Parsing testable hors-ligne de la réponse Jobicy. */
    public static List<RawJob> parse(JsonNode root) {
        List<RawJob> jobs = new ArrayList<>();
        JsonNode arr = root == null ? null : root.get("jobs");
        if (arr == null || !arr.isArray()) {
            return jobs;
        }
        for (JsonNode j : arr) {
            String contract = null;
            JsonNode types = j.get("jobType");
            if (types != null && types.isArray() && !types.isEmpty()) {
                contract = types.get(0).asText();
            } else if (types != null && types.isTextual()) {
                contract = types.asText();
            }
            String salary = null;
            JsonNode min = j.get("annualSalaryMin");
            JsonNode max = j.get("annualSalaryMax");
            if (min != null && min.isNumber() && min.asInt() > 0) {
                salary = max != null && max.isNumber() && max.asInt() > 0
                        ? min.asText() + " - " + max.asText() : min.asText();
            }
            List<String> tags = new ArrayList<>();
            JsonNode industry = j.get("jobIndustry");
            if (industry != null && industry.isArray()) {
                industry.forEach(t -> tags.add(t.asText()));
            }
            String description = Nodes.text(j, "jobDescription");
            if (description == null) {
                description = Nodes.text(j, "jobExcerpt");
            }
            jobs.add(new RawJob(
                    CODE,
                    Nodes.text(j, "id"),
                    Nodes.text(j, "jobTitle"),
                    Nodes.text(j, "companyName"),
                    Nodes.text(j, "jobGeo"),
                    Nodes.text(j, "url"),
                    description,
                    Nodes.parseInstant(Nodes.text(j, "pubDate")),
                    contract,
                    salary,
                    "remote",
                    tags));
        }
        return jobs;
    }
}
