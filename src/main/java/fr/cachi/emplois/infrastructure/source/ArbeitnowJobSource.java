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
 * Connecteur Arbeitnow — API publique JSON (offres tech UE / remote), sans clé.
 * Doc : https://www.arbeitnow.com/api
 */
public class ArbeitnowJobSource implements JobSource {

    public static final String CODE = "arbeitnow";
    private static final String BASE = "https://www.arbeitnow.com/api/job-board-api";

    private final boolean enabled;

    public ArbeitnowJobSource() {
        this(!"false".equalsIgnoreCase(Config.get("ARBEITNOW_ENABLED")));
    }

    public ArbeitnowJobSource(boolean enabled) {
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
            JsonNode root = HttpJson.get(BASE, null);
            return parse(root);
        } catch (Exception e) {
            System.out.println("[arbeitnow] échec de récupération : " + e.getMessage());
            return List.of();
        }
    }

    /** Parsing testable hors-ligne de la réponse Arbeitnow. */
    public static List<RawJob> parse(JsonNode root) {
        List<RawJob> jobs = new ArrayList<>();
        JsonNode arr = root == null ? null : root.get("data");
        if (arr == null || !arr.isArray()) {
            return jobs;
        }
        for (JsonNode j : arr) {
            List<String> tags = new ArrayList<>();
            JsonNode tagsNode = j.get("tags");
            if (tagsNode != null && tagsNode.isArray()) {
                tagsNode.forEach(t -> tags.add(t.asText()));
            }
            String contract = null;
            JsonNode types = j.get("job_types");
            if (types != null && types.isArray() && !types.isEmpty()) {
                contract = types.get(0).asText();
            }
            JsonNode remote = j.get("remote");
            String remoteRaw = remote != null && remote.asBoolean(false) ? "remote" : null;
            jobs.add(new RawJob(
                    CODE,
                    Nodes.text(j, "slug"),
                    Nodes.text(j, "title"),
                    Nodes.text(j, "company_name"),
                    Nodes.text(j, "location"),
                    Nodes.text(j, "url"),
                    Nodes.text(j, "description"),
                    Nodes.epochSeconds(j, "created_at"),
                    contract,
                    null,
                    remoteRaw,
                    tags));
        }
        return jobs;
    }
}
