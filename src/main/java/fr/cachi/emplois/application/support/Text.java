package fr.cachi.emplois.application.support;

import java.text.Normalizer;
import java.util.Locale;

/** Utilitaires de normalisation de texte (slug, suppression d'accents). */
public final class Text {

    private Text() {
    }

    /** Minuscule + suppression des accents. */
    public static String deaccentLower(String s) {
        if (s == null) {
            return "";
        }
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return n.toLowerCase(Locale.ROOT).trim();
    }

    /** Slug : minuscule, sans accent, caractères non alphanumériques remplacés par un espace, compacté. */
    public static String slug(String s) {
        if (s == null) {
            return "";
        }
        String d = deaccentLower(s).replaceAll("[^a-z0-9]+", " ").trim();
        return d.replaceAll("\\s+", " ");
    }
}
