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
 * Connecteur Himalayas — API publique JSON (offres remote tech), sans clé.
 * Toutes les offres sont en télétravail. Doc : https://himalayas.app/jobs/api
 */
public class HimalayasJobSource implements JobSource {

    public static final String CODE = "himalayas";
    private static final String BASE = "https://himalayas.app/jobs/api";

    private final boolean enabled;

    public HimalayasJobSource() {
        this(!"false".equalsIgnoreCase(Config.get("HIMALAYAS_ENABLED")));
    }

    public HimalayasJobSource(boolean enabled) {
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
            String url = BASE + "?limit=" + Math.max(1, criteria.limit());
            JsonNode root = HttpJson.get(url, null);
            return parse(root);
        } catch (Exception e) {
            System.out.println("[himalayas] échec de récupération : " + e.getMessage());
            return List.of();
        }
    }

    /** Parsing testable hors-ligne de la réponse Himalayas. */
    public static List<RawJob> parse(JsonNode root) {
        List<RawJob> jobs = new ArrayList<>();
        JsonNode arr = root == null ? null : root.get("jobs");
        if (arr == null || !arr.isArray()) {
            return jobs;
        }
        for (JsonNode j : arr) {
            String location = null;
            JsonNode locs = j.get("locationRestrictions");
            if (locs != null && locs.isArray() && !locs.isEmpty()) {
                location = locs.get(0).asText();
            }
            String salary = null;
            JsonNode min = j.get("minSalary");
            JsonNode max = j.get("maxSalary");
            if (min != null && min.isNumber() && min.asInt() > 0) {
                salary = max != null && max.isNumber() && max.asInt() > 0
                        ? min.asText() + " - " + max.asText() : min.asText();
            }
            List<String> tags = new ArrayList<>();
            JsonNode cats = j.get("categories");
            if (cats != null && cats.isArray()) {
                cats.forEach(t -> tags.add(t.asText()));
            }
            jobs.add(new RawJob(
                    CODE,
                    Nodes.text(j, "guid"),
                    Nodes.text(j, "title"),
                    Nodes.text(j, "companyName"),
                    location,
                    Nodes.text(j, "applicationLink"),
                    Nodes.text(j, "description"),
                    Nodes.epochSeconds(j, "pubDate"),
                    Nodes.text(j, "employmentType"),
                    salary,
                    "remote",
                    tags));
        }
        return jobs;
    }
}
