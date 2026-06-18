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
 * Connecteur Ashby — API publique « job board » d'entreprise, sans clé.
 * <b>Opt-in</b> : liste de boards via {@code ASHBY_BOARDS} (séparés par des virgules).
 * Désactivé si aucun board configuré. Doc : https://developers.ashbyhq.com/docs/public-job-posting-api
 */
public class AshbyJobSource implements JobSource {

    public static final String CODE = "ashby";
    private static final String BASE = "https://api.ashbyhq.com/posting-api/job-board/";

    private final List<String> boards;

    public AshbyJobSource() {
        this(Boards.parse(Config.get("ASHBY_BOARDS")));
    }

    public AshbyJobSource(List<String> boards) {
        this.boards = boards == null ? List.of() : List.copyOf(boards);
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public boolean enabled() {
        return !boards.isEmpty();
    }

    @Override
    public List<RawJob> fetch(SearchCriteria criteria) {
        List<RawJob> all = new ArrayList<>();
        for (String board : boards) {
            try {
                JsonNode root = HttpJson.get(
                        BASE + HttpJson.enc(board) + "?includeCompensation=true", null);
                all.addAll(parse(root, board));
            } catch (Exception e) {
                System.out.println("[ashby] échec du board " + board + " : " + e.getMessage());
            }
        }
        return all;
    }

    /** Parsing testable hors-ligne de la réponse Ashby pour un board donné. */
    public static List<RawJob> parse(JsonNode root, String company) {
        List<RawJob> jobs = new ArrayList<>();
        JsonNode arr = root == null ? null : root.get("jobs");
        if (arr == null || !arr.isArray()) {
            return jobs;
        }
        for (JsonNode j : arr) {
            JsonNode remote = j.get("isRemote");
            String remoteRaw = remote != null && remote.asBoolean(false) ? "remote" : null;
            String department = Nodes.text(j, "departmentName");
            jobs.add(new RawJob(
                    CODE,
                    Nodes.text(j, "id"),
                    Nodes.text(j, "title"),
                    company,
                    Nodes.text(j, "location"),
                    Nodes.text(j, "jobUrl"),
                    Nodes.text(j, "descriptionPlain"),
                    Nodes.parseInstant(Nodes.text(j, "publishedAt")),
                    Nodes.text(j, "employmentType"),
                    null,
                    remoteRaw,
                    department != null ? List.of(department) : List.of()));
        }
        return jobs;
    }
}
