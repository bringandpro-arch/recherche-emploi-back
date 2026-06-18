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
 * Connecteur Remotive — API publique JSON (remote tech), sans clé.
 * Doc : https://remotive.com/api/remote-jobs
 */
public class RemotiveJobSource implements JobSource {

    public static final String CODE = "remotive";
    private static final String BASE = "https://remotive.com/api/remote-jobs";

    private final boolean enabled;

    public RemotiveJobSource() {
        this(!"false".equalsIgnoreCase(Config.get("REMOTIVE_ENABLED")));
    }

    public RemotiveJobSource(boolean enabled) {
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
            if (criteria.query() != null && !criteria.query().isBlank()) {
                url += "&search=" + HttpJson.enc(criteria.query());
            }
            JsonNode root = HttpJson.get(url, null);
            return parse(root);
        } catch (Exception e) {
            System.out.println("[remotive] échec de récupération : " + e.getMessage());
            return List.of();
        }
    }

    /** Parsing testable hors-ligne de la réponse Remotive. */
    public static List<RawJob> parse(JsonNode root) {
        List<RawJob> jobs = new ArrayList<>();
        JsonNode arr = root == null ? null : root.get("jobs");
        if (arr == null || !arr.isArray()) {
            return jobs;
        }
        for (JsonNode j : arr) {
            List<String> tags = new ArrayList<>();
            JsonNode tagsNode = j.get("tags");
            if (tagsNode != null && tagsNode.isArray()) {
                tagsNode.forEach(t -> tags.add(t.asText()));
            }
            jobs.add(new RawJob(
                    CODE,
                    Nodes.text(j, "id"),
                    Nodes.text(j, "title"),
                    Nodes.text(j, "company_name"),
                    Nodes.text(j, "candidate_required_location"),
                    Nodes.text(j, "url"),
                    Nodes.text(j, "description"),
                    Nodes.parseInstant(Nodes.text(j, "publication_date")),
                    Nodes.text(j, "job_type"),
                    Nodes.text(j, "salary"),
                    "remote",
                    tags));
        }
        return jobs;
    }
}
