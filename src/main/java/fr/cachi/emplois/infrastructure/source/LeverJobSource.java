package fr.cachi.emplois.infrastructure.source;

import com.fasterxml.jackson.databind.JsonNode;
import fr.cachi.emplois.domain.model.RawJob;
import fr.cachi.emplois.domain.model.SearchCriteria;
import fr.cachi.emplois.domain.port.JobSource;
import fr.cachi.emplois.infrastructure.config.Config;
import fr.cachi.emplois.infrastructure.source.http.HttpJson;
import fr.cachi.emplois.infrastructure.source.http.Nodes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Connecteur Lever — API publique « postings » d'entreprise, sans clé.
 * <b>Opt-in</b> : liste d'entreprises via {@code LEVER_BOARDS} (séparées par des virgules).
 * Désactivé si aucune entreprise configurée. Doc : https://github.com/lever/postings-api
 */
public class LeverJobSource implements JobSource {

    public static final String CODE = "lever";
    private static final String BASE = "https://api.lever.co/v0/postings/";

    private final List<String> boards;

    public LeverJobSource() {
        this(Boards.parse(Config.get("LEVER_BOARDS")));
    }

    public LeverJobSource(List<String> boards) {
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
                JsonNode root = HttpJson.get(BASE + HttpJson.enc(board) + "?mode=json", null);
                all.addAll(parse(root, board));
            } catch (Exception e) {
                System.out.println("[lever] échec du board " + board + " : " + e.getMessage());
            }
        }
        return all;
    }

    /** Parsing testable hors-ligne de la réponse Lever pour une entreprise donnée. */
    public static List<RawJob> parse(JsonNode root, String company) {
        List<RawJob> jobs = new ArrayList<>();
        if (root == null || !root.isArray()) {
            return jobs;
        }
        for (JsonNode j : root) {
            JsonNode cat = j.get("categories");
            String location = Nodes.text(cat, "location");
            String commitment = Nodes.text(cat, "commitment");
            String team = Nodes.text(cat, "team");
            String description = Nodes.text(j, "descriptionPlain");
            if (description == null) {
                description = Nodes.text(j, "description");
            }
            Instant published = null;
            JsonNode created = j.get("createdAt");
            if (created != null && created.isNumber()) {
                published = Instant.ofEpochMilli(created.asLong());
            }
            String remoteRaw = location != null && location.toLowerCase().contains("remote") ? "remote" : null;
            jobs.add(new RawJob(
                    CODE,
                    Nodes.text(j, "id"),
                    Nodes.text(j, "text"),
                    company,
                    location,
                    Nodes.text(j, "hostedUrl"),
                    description,
                    published,
                    commitment,
                    null,
                    remoteRaw,
                    team != null ? List.of(team) : List.of()));
        }
        return jobs;
    }
}
