package fr.cachi.emplois.infrastructure.source;

import java.util.Arrays;
import java.util.List;

/**
 * Petit utilitaire partagé par les connecteurs ATS (Greenhouse, Lever, Ashby…) : transforme une
 * configuration « liste de boards/entreprises » (CSV) en liste de slugs nettoyés. Vide/null ⇒ liste
 * vide (le connecteur se désactive alors via {@code enabled()}).
 */
public final class Boards {

    private Boards() {
    }

    public static List<String> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }
}
