package fr.cachi.emplois.application;

import fr.cachi.emplois.application.support.Text;
import fr.cachi.emplois.domain.model.ContractType;
import fr.cachi.emplois.domain.model.Offer;
import fr.cachi.emplois.domain.model.RawJob;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Normalisation (F5) : transforme une offre brute {@link RawJob} en {@link Offer} (modèle commun).
 * Heuristiques volontairement simples et documentées ; l'IA (F12) pourra affiner l'extraction.
 */
public class OfferNormalizer {

    private static final List<String> KNOWN_CITIES = List.of(
            "paris", "lyon", "marseille", "toulouse", "bordeaux", "nantes", "lille",
            "nice", "rennes", "grenoble", "strasbourg", "montpellier", "sophia antipolis",
            "aix en provence", "annecy", "clermont ferrand", "tours", "dijon", "metz", "nancy");

    private static final List<String> TECHS = List.of(
            "aws", "azure", "gcp", "terraform", "ansible", "kubernetes", "k8s", "docker",
            "prometheus", "grafana", "datadog", "elk", "observabilite", "observability",
            "java", "spring", "python", "go", "golang", "node", "typescript", "react",
            "angular", "kafka", "postgresql", "mongodb", "linux", "ci/cd", "gitlab",
            "github", "jenkins", "argocd", "helm", "openshift", "lambda", "serverless");

    private static final Pattern AMOUNT = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(k)?", Pattern.CASE_INSENSITIVE);

    /** Normalise une offre brute. */
    public Offer normalize(RawJob raw) {
        String text = ((raw.title() == null ? "" : raw.title()) + " "
                + (raw.description() == null ? "" : raw.description()) + " "
                + (raw.contractRaw() == null ? "" : raw.contractRaw())).trim();

        ContractType contract = detectContract(raw, text);
        Integer remote = detectRemote(raw, text);
        String city = detectCity(raw.locationRaw());
        String country = city != null ? "France" : null;
        int[] amounts = parseAmounts(raw.salaryRaw());
        boolean tjm = isTjm(raw.salaryRaw()) || contract == ContractType.FREELANCE;
        List<String> stack = detectStack(text, raw.tags());

        String dedupKey = Text.slug(raw.title()) + "|" + Text.slug(raw.company()) + "|"
                + Text.slug(city != null ? city : raw.locationRaw());

        return new Offer(
                raw.source(),
                raw.externalId(),
                raw.title(),
                raw.company(),
                raw.locationRaw(),
                city,
                country,
                remote,
                contract,
                tjm ? null : amounts[0] > 0 ? amounts[0] : null,
                tjm ? null : amounts[1] > 0 ? amounts[1] : null,
                tjm ? (amounts[0] > 0 ? amounts[0] : null) : null,
                tjm ? (amounts[1] > 0 ? amounts[1] : null) : null,
                raw.salaryRaw() != null && (raw.salaryRaw().contains("€")
                        || Text.deaccentLower(raw.salaryRaw()).contains("eur")) ? "EUR"
                        : (amounts[0] > 0 || amounts[1] > 0 ? "EUR" : null),
                stack,
                raw.url(),
                raw.description(),
                raw.publishedAt(),
                Instant.now(),
                dedupKey);
    }

    static ContractType detectContract(RawJob raw, String text) {
        ContractType c = ContractType.fromLabel(raw.contractRaw());
        if (c == ContractType.UNKNOWN) {
            c = ContractType.fromLabel(text);
        }
        return c;
    }

    static Integer detectRemote(RawJob raw, String text) {
        String t = Text.deaccentLower((raw.remoteRaw() == null ? "" : raw.remoteRaw()) + " "
                + (raw.locationRaw() == null ? "" : raw.locationRaw()) + " " + text);
        if (t.contains("full remote") || t.contains("100% remote") || t.contains("100 % remote")
                || t.contains("teletravail total") || t.contains("remote first")
                || t.equals("remote") || t.contains(" remote ")) {
            return 100;
        }
        if (t.contains("hybride") || t.contains("hybrid") || t.contains("teletravail partiel")
                || t.contains("partiel") || t.contains("jours de teletravail")) {
            return 50;
        }
        if (t.contains("presentiel") || t.contains("sur site") || t.contains("no remote")
                || t.contains("pas de teletravail")) {
            return 0;
        }
        return null;
    }

    static String detectCity(String locationRaw) {
        if (locationRaw == null || locationRaw.isBlank()) {
            return null;
        }
        String l = Text.deaccentLower(locationRaw);
        for (String city : KNOWN_CITIES) {
            if (l.contains(city)) {
                return capitalize(city);
            }
        }
        // Repli : retire un préfixe de département "69 - " puis prend la partie pertinente.
        String cleaned = locationRaw.replaceAll("^\\s*\\d{2,3}\\s*-\\s*", "").trim();
        cleaned = cleaned.split("[,(]")[0].trim();
        if (cleaned.isEmpty() || cleaned.length() > 40) {
            return null;
        }
        return cleaned;
    }

    static boolean isTjm(String salaryRaw) {
        if (salaryRaw == null) {
            return false;
        }
        String s = Text.deaccentLower(salaryRaw);
        return s.contains("tjm") || s.contains("jour") || s.contains("/j") || s.contains(" j ");
    }

    /** Renvoie [min, max] en euros (0 si absent). Filtre les valeurs aberrantes. */
    static int[] parseAmounts(String salaryRaw) {
        if (salaryRaw == null || salaryRaw.isBlank()) {
            return new int[]{0, 0};
        }
        boolean tjm = isTjm(salaryRaw);
        List<Integer> values = new ArrayList<>();
        Matcher m = AMOUNT.matcher(salaryRaw);
        while (m.find()) {
            try {
                double v = Double.parseDouble(m.group(1).replace(',', '.'));
                if (m.group(2) != null) {
                    v *= 1000;
                }
                int iv = (int) Math.round(v);
                if (tjm) {
                    if (iv >= 100 && iv <= 3000) {
                        values.add(iv);
                    }
                } else if (iv >= 1000) {
                    values.add(iv);
                }
            } catch (NumberFormatException ignored) {
                // ignore le token
            }
        }
        if (values.isEmpty()) {
            return new int[]{0, 0};
        }
        int min = values.stream().min(Integer::compareTo).orElse(0);
        int max = values.stream().max(Integer::compareTo).orElse(min);
        return new int[]{min, max};
    }

    static List<String> detectStack(String text, List<String> tags) {
        Set<String> stack = new LinkedHashSet<>();
        if (tags != null) {
            tags.forEach(t -> stack.add(Text.deaccentLower(t)));
        }
        String t = Text.deaccentLower(text);
        for (String tech : TECHS) {
            if (t.contains(tech)) {
                stack.add(tech);
            }
        }
        return new ArrayList<>(stack);
    }

    private static String capitalize(String s) {
        if (s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
