package fr.cachi.emplois.infrastructure.llm;

import fr.cachi.emplois.domain.model.Offer;
import fr.cachi.emplois.domain.model.Profile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

/** Chargement et remplissage des gabarits de prompts (packagés dans le jar). */
public final class Prompts {

    private Prompts() {
    }

    /** Charge un gabarit depuis le classpath (src/main/resources/prompts/...). */
    public static String load(String resource) {
        try (InputStream in = Prompts.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Gabarit introuvable : " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Échec de chargement du gabarit " + resource, e);
        }
    }

    /** Construit le message utilisateur de scoring à partir du profil et de l'offre. */
    public static String scoringUser(Profile p, Offer o) {
        String tjm = p.targetTjmMin() == null ? "non précisé" : p.targetTjmMin() + "€";
        String salary = p.targetSalaryMin() == null ? "non précisé" : p.targetSalaryMin() + "€";
        String compensation = compensation(o);
        return fill(load("prompts/scoring-user.txt"), Map.ofEntries(
                Map.entry("skills", joinList(p.skills())),
                Map.entry("contractTypes", p.contractTypes() == null ? "" :
                        p.contractTypes().stream().map(Enum::name).collect(Collectors.joining(", "))),
                Map.entry("locations", joinList(p.locations())),
                Map.entry("remoteMin", String.valueOf(p.remoteMin() == null ? 0 : p.remoteMin())),
                Map.entry("targetTjm", tjm),
                Map.entry("targetSalary", salary),
                Map.entry("excludedKeywords", joinList(p.excludedKeywords())),
                Map.entry("title", nz(o.title())),
                Map.entry("company", nz(o.company())),
                Map.entry("location", nz(o.city() != null ? o.city() : o.locationRaw())),
                Map.entry("contractType", String.valueOf(o.contractType())),
                Map.entry("remotePercent", String.valueOf(o.remotePercent() == null ? 0 : o.remotePercent())),
                Map.entry("compensation", compensation),
                Map.entry("description", truncate(nz(o.descriptionRaw()), 4000))));
    }

    private static String compensation(Offer o) {
        if (o.tjmMin() != null || o.tjmMax() != null) {
            return "TJM " + nz2(o.tjmMin()) + "-" + nz2(o.tjmMax()) + "€";
        }
        if (o.salaryMin() != null || o.salaryMax() != null) {
            return "Salaire " + nz2(o.salaryMin()) + "-" + nz2(o.salaryMax()) + "€";
        }
        return "non précisée";
    }

    static String fill(String template, Map<String, String> values) {
        String out = template;
        for (Map.Entry<String, String> e : values.entrySet()) {
            out = out.replace("{{" + e.getKey() + "}}", e.getValue() == null ? "" : e.getValue());
        }
        return out;
    }

    private static String joinList(java.util.List<String> l) {
        return l == null || l.isEmpty() ? "aucune" : String.join(", ", l);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String nz2(Integer i) {
        return i == null ? "?" : i.toString();
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
