package fr.cachi.emplois.infrastructure.source;

import com.fasterxml.jackson.databind.JsonNode;
import fr.cachi.emplois.domain.model.RawJob;
import fr.cachi.emplois.domain.model.SearchCriteria;
import fr.cachi.emplois.domain.port.JobSource;
import fr.cachi.emplois.infrastructure.source.http.HttpJson;
import fr.cachi.emplois.infrastructure.source.http.Nodes;

import java.util.ArrayList;
import java.util.List;

/**
 * Connecteur The Muse — API publique JSON. Clé <b>optionnelle</b> ({@code THE_MUSE_API_KEY}) :
 * fonctionne sans clé à un débit plus faible. Couverture US/international (tech).
 * Doc : https://www.themuse.com/developers/api/v2
 */
public class TheMuseJobSource implements JobSource {

    public static final String CODE = "the-muse";
    private static final String BASE = "https://www.themuse.com/api/public/jobs";

    private final String apiKey;
    private final boolean enabled;

    public TheMuseJobSource() {
        this(System.getenv("THE_MUSE_API_KEY"),
                !"false".equalsIgnoreCase(System.getenv("THE_MUSE_ENABLED")));
    }

    public TheMuseJobSource(String apiKey, boolean enabled) {
        this.apiKey = apiKey;
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
            StringBuilder url = new StringBuilder(BASE)
                    .append("?page=0&category=").append(HttpJson.enc("Software Engineering"));
            if (criteria.location() != null && !criteria.location().isBlank()) {
                url.append("&location=").append(HttpJson.enc(criteria.location()));
            }
            if (apiKey != null && !apiKey.isBlank()) {
                url.append("&api_key=").append(HttpJson.enc(apiKey));
            }
            JsonNode root = HttpJson.get(url.toString(), null);
            return parse(root);
        } catch (Exception e) {
            System.out.println("[the-muse] échec de récupération : " + e.getMessage());
            return List.of();
        }
    }

    /** Parsing testable hors-ligne de la réponse The Muse. */
    public static List<RawJob> parse(JsonNode root) {
        List<RawJob> jobs = new ArrayList<>();
        JsonNode arr = root == null ? null : root.get("results");
        if (arr == null || !arr.isArray()) {
            return jobs;
        }
        for (JsonNode j : arr) {
            String location = null;
            JsonNode locs = j.get("locations");
            if (locs != null && locs.isArray() && !locs.isEmpty()) {
                location = Nodes.text(locs.get(0), "name");
            }
            List<String> tags = new ArrayList<>();
            JsonNode cats = j.get("categories");
            if (cats != null && cats.isArray()) {
                cats.forEach(c -> {
                    String name = Nodes.text(c, "name");
                    if (name != null) {
                        tags.add(name);
                    }
                });
            }
            String remoteRaw = location != null && location.toLowerCase().contains("remote") ? "remote" : null;
            jobs.add(new RawJob(
                    CODE,
                    Nodes.text(j, "id"),
                    Nodes.text(j, "name"),
                    Nodes.text(j, "company", "name"),
                    location,
                    Nodes.text(j.get("refs"), "landing_page"),
                    Nodes.text(j, "contents"),
                    Nodes.parseInstant(Nodes.text(j, "publication_date")),
                    null,
                    null,
                    remoteRaw,
                    tags));
        }
        return jobs;
    }
}
