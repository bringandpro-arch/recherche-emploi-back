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
 * Connecteur Recruitee — API publique « offers » d'entreprise, sans clé.
 * <b>Opt-in</b> : liste d'entreprises (sous-domaines Recruitee) via {@code RECRUITEE_COMPANIES}
 * (séparées par des virgules). Désactivé si aucune configurée.
 * Doc : https://docs.recruitee.com/reference/offers (endpoint public {company}.recruitee.com).
 */
public class RecruiteeJobSource implements JobSource {

    public static final String CODE = "recruitee";

    private final List<String> companies;

    public RecruiteeJobSource() {
        this(Boards.parse(Config.get("RECRUITEE_COMPANIES")));
    }

    public RecruiteeJobSource(List<String> companies) {
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
                        "https://" + HttpJson.enc(company) + ".recruitee.com/api/offers/", null);
                all.addAll(parse(root, company));
            } catch (Exception e) {
                System.out.println("[recruitee] échec de l'entreprise " + company + " : " + e.getMessage());
            }
        }
        return all;
    }

    /** Parsing testable hors-ligne de la réponse Recruitee pour une entreprise donnée. */
    public static List<RawJob> parse(JsonNode root, String company) {
        List<RawJob> jobs = new ArrayList<>();
        JsonNode arr = root == null ? null : root.get("offers");
        if (arr == null || !arr.isArray()) {
            return jobs;
        }
        for (JsonNode j : arr) {
            String location = Nodes.text(j, "location");
            if (location == null) {
                location = Nodes.text(j, "city");
            }
            JsonNode remote = j.get("remote");
            String remoteRaw = remote != null && remote.asBoolean(false) ? "remote" : null;
            String department = Nodes.text(j, "department");
            jobs.add(new RawJob(
                    CODE,
                    Nodes.text(j, "id"),
                    Nodes.text(j, "title"),
                    company,
                    location,
                    Nodes.text(j, "careers_url"),
                    Nodes.text(j, "description"),
                    Nodes.parseInstant(Nodes.text(j, "published_at")),
                    Nodes.text(j, "employment_type_code"),
                    null,
                    remoteRaw,
                    department != null ? List.of(department) : List.of()));
        }
        return jobs;
    }
}
