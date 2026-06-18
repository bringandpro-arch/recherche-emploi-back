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
 * Connecteur SmartRecruiters — API publique « postings » d'entreprise, sans clé.
 * <b>Opt-in</b> : liste d'entreprises (identifiants SmartRecruiters) via
 * {@code SMARTRECRUITERS_COMPANIES} (séparées par des virgules). Désactivé si aucune configurée.
 * Doc : https://developers.smartrecruiters.com/reference/postings-1
 */
public class SmartRecruitersJobSource implements JobSource {

    public static final String CODE = "smartrecruiters";
    private static final String BASE = "https://api.smartrecruiters.com/v1/companies/";
    private static final String LANDING = "https://jobs.smartrecruiters.com/";

    private final List<String> companies;

    public SmartRecruitersJobSource() {
        this(Boards.parse(Config.get("SMARTRECRUITERS_COMPANIES")));
    }

    public SmartRecruitersJobSource(List<String> companies) {
        this.companies = companies == null ? List.of() : List.copyOf(companies);
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public boolean enabled() {
        return !companies.isEmpty();
    }

    @Override
    public List<RawJob> fetch(SearchCriteria criteria) {
        List<RawJob> all = new ArrayList<>();
        for (String company : companies) {
            try {
                JsonNode root = HttpJson.get(
                        BASE + HttpJson.enc(company) + "/postings?limit=" + Math.max(1, criteria.limit()), null);
                all.addAll(parse(root, company));
            } catch (Exception e) {
                System.out.println("[smartrecruiters] échec de l'entreprise " + company + " : " + e.getMessage());
            }
        }
        return all;
    }

    /** Parsing testable hors-ligne de la réponse SmartRecruiters pour une entreprise donnée. */
    public static List<RawJob> parse(JsonNode root, String company) {
        List<RawJob> jobs = new ArrayList<>();
        JsonNode arr = root == null ? null : root.get("content");
        if (arr == null || !arr.isArray()) {
            return jobs;
        }
        for (JsonNode j : arr) {
            JsonNode loc = j.get("location");
            String location = location(loc);
            JsonNode remote = loc == null ? null : loc.get("remote");
            String remoteRaw = remote != null && remote.asBoolean(false) ? "remote" : null;
            String id = Nodes.text(j, "id");
            jobs.add(new RawJob(
                    CODE,
                    id,
                    Nodes.text(j, "name"),
                    company,
                    location,
                    LANDING + company + (id != null ? "/" + id : ""),
                    null,
                    Nodes.parseInstant(Nodes.text(j, "releasedDate")),
                    null,
                    null,
                    remoteRaw,
                    List.of()));
        }
        return jobs;
    }

    /** Concatène ville / région / pays s'ils sont présents. */
    private static String location(JsonNode loc) {
        if (loc == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (String field : new String[] {"city", "region", "country"}) {
            String v = Nodes.text(loc, field);
            if (v != null && !v.isBlank()) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(v);
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }
}
