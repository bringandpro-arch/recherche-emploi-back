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
 * Connecteur Greenhouse — API publique des « job boards » d'entreprise, sans clé.
 * <b>Opt-in</b> : liste de boards (slugs d'entreprise) via {@code GREENHOUSE_BOARDS}
 * (séparés par des virgules). Désactivé si aucun board configuré.
 * Doc : https://developers.greenhouse.io/job-board.html
 */
public class GreenhouseJobSource implements JobSource {

    public static final String CODE = "greenhouse";
    private static final String BASE = "https://boards-api.greenhouse.io/v1/boards/";

    private final List<String> boards;

    public GreenhouseJobSource() {
        this(Boards.parse(Config.get("GREENHOUSE_BOARDS")));
    }

    public GreenhouseJobSource(List<String> boards) {
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
                JsonNode root = HttpJson.get(BASE + HttpJson.enc(board) + "/jobs?content=true", null);
                all.addAll(parse(root, board));
            } catch (Exception e) {
                System.out.println("[greenhouse] échec du board " + board + " : " + e.getMessage());
            }
        }
        return all;
    }

    /** Parsing testable hors-ligne de la réponse Greenhouse pour un board donné. */
    public static List<RawJob> parse(JsonNode root, String company) {
        List<RawJob> jobs = new ArrayList<>();
        JsonNode arr = root == null ? null : root.get("jobs");
        if (arr == null || !arr.isArray()) {
            return jobs;
        }
        for (JsonNode j : arr) {
            jobs.add(new RawJob(
                    CODE,
                    Nodes.text(j, "id"),
                    Nodes.text(j, "title"),
                    company,
                    Nodes.text(j, "location", "name"),
                    Nodes.text(j, "absolute_url"),
                    Nodes.text(j, "content"),
                    Nodes.parseInstant(Nodes.text(j, "updated_at")),
                    null,
                    null,
                    null,
                    List.of()));
        }
        return jobs;
    }
}
