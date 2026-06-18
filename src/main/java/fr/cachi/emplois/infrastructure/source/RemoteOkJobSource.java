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
import java.util.Map;

/**
 * Connecteur RemoteOK — API publique JSON (remote tech), sans clé (attribution demandée par la source).
 * La réponse est un tableau dont le <b>premier élément</b> est une mention légale (sans champ
 * {@code position}) : on l'ignore. Doc : https://remoteok.com/api
 */
public class RemoteOkJobSource implements JobSource {

    public static final String CODE = "remoteok";
    private static final String BASE = "https://remoteok.com/api";

    private final boolean enabled;

    public RemoteOkJobSource() {
        this(!"false".equalsIgnoreCase(Config.get("REMOTEOK_ENABLED")));
    }

    public RemoteOkJobSource(boolean enabled) {
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
            String url = BASE;
            if (criteria.query() != null && !criteria.query().isBlank()) {
                url += "?tags=" + HttpJson.enc(criteria.query());
            }
            // RemoteOK exige un User-Agent « non-bot ».
            JsonNode root = HttpJson.get(url, Map.of("User-Agent", "recherche-emploi/1.0"));
            return parse(root);
        } catch (Exception e) {
            System.out.println("[remoteok] échec de récupération : " + e.getMessage());
            return List.of();
        }
    }

    /** Parsing testable hors-ligne de la réponse RemoteOK. */
    public static List<RawJob> parse(JsonNode root) {
        List<RawJob> jobs = new ArrayList<>();
        if (root == null || !root.isArray()) {
            return jobs;
        }
        for (JsonNode j : root) {
            String title = Nodes.text(j, "position");
            if (title == null || title.isBlank()) {
                continue; // mention légale ou entrée sans poste
            }
            String salary = null;
            JsonNode min = j.get("salary_min");
            JsonNode max = j.get("salary_max");
            if (min != null && min.isNumber() && min.asInt() > 0) {
                salary = max != null && max.isNumber() && max.asInt() > 0
                        ? min.asText() + " - " + max.asText() : min.asText();
            }
            List<String> tags = new ArrayList<>();
            JsonNode tagsNode = j.get("tags");
            if (tagsNode != null && tagsNode.isArray()) {
                tagsNode.forEach(t -> tags.add(t.asText()));
            }
            jobs.add(new RawJob(
                    CODE,
                    Nodes.text(j, "id"),
                    title,
                    Nodes.text(j, "company"),
                    Nodes.text(j, "location"),
                    Nodes.text(j, "url"),
                    Nodes.text(j, "description"),
                    Nodes.parseInstant(Nodes.text(j, "date")),
                    null,
                    salary,
                    "remote",
                    tags));
        }
        return jobs;
    }
}
