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
 * Connecteur Careerjet — agrégateur (FR), API publique avec identifiant d'affiliation gratuit
 * ({@code affid}). Locale {@code fr_FR}. Désactivé sans affid.
 * Doc : https://www.careerjet.fr/partners/api/
 */
public class CareerjetJobSource implements JobSource {

    public static final String CODE = "careerjet";
    private static final String BASE = "https://public.api.careerjet.net/search";

    private final String affid;

    public CareerjetJobSource() {
        this(Config.get("CAREERJET_AFFID"));
    }

    public CareerjetJobSource(String affid) {
        this.affid = affid;
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public boolean enabled() {
        return affid != null && !affid.isBlank()
                && !"false".equalsIgnoreCase(Config.get("CAREERJET_ENABLED"));
    }

    @Override
    public List<RawJob> fetch(SearchCriteria criteria) {
        try {
            StringBuilder url = new StringBuilder(BASE)
                    .append("?locale_code=fr_FR")
                    .append("&affid=").append(HttpJson.enc(affid))
                    .append("&pagesize=").append(Math.max(1, criteria.limit()))
                    .append("&contenttype=application/json")
                    .append("&user_ip=127.0.0.1")
                    .append("&user_agent=").append(HttpJson.enc("recherche-emploi/1.0"));
            if (criteria.query() != null && !criteria.query().isBlank()) {
                url.append("&keywords=").append(HttpJson.enc(criteria.query()));
            }
            if (criteria.location() != null && !criteria.location().isBlank()) {
                url.append("&location=").append(HttpJson.enc(criteria.location()));
            }
            JsonNode root = HttpJson.get(url.toString(), null);
            return parse(root);
        } catch (Exception e) {
            System.out.println("[careerjet] échec de récupération : " + e.getMessage());
            return List.of();
        }
    }

    /** Parsing testable hors-ligne de la réponse Careerjet. */
    public static List<RawJob> parse(JsonNode root) {
        List<RawJob> jobs = new ArrayList<>();
        JsonNode arr = root == null ? null : root.get("jobs");
        if (arr == null || !arr.isArray()) {
            return jobs;
        }
        for (JsonNode j : arr) {
            String url = Nodes.text(j, "url");
            jobs.add(new RawJob(
                    CODE,
                    url,
                    Nodes.text(j, "title"),
                    Nodes.text(j, "company"),
                    Nodes.text(j, "locations"),
                    url,
                    Nodes.text(j, "description"),
                    Nodes.parseInstant(Nodes.text(j, "date")),
                    null,
                    Nodes.text(j, "salary"),
                    null,
                    List.of()));
        }
        return jobs;
    }
}
