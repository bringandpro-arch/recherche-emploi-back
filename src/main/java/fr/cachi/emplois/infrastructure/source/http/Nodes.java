package fr.cachi.emplois.infrastructure.source.http;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/** Helpers de lecture tolérante des nœuds JSON renvoyés par les sources. */
public final class Nodes {

    private Nodes() {
    }

    /** Texte d'un champ, ou null si absent/null. */
    public static String text(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    /** Texte d'un sous-champ imbriqué (ex. company.display_name). */
    public static String text(JsonNode node, String field, String subField) {
        if (node == null) {
            return null;
        }
        return text(node.get(field), subField);
    }

    /**
     * Parse tolérant de date : ISO instant, ISO offset, ou "yyyy-MM-dd HH:mm:ss" (Remotive).
     * Renvoie null si non interprétable.
     */
    public static Instant parseInstant(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(raw);
        } catch (Exception ignored) {
            // suite
        }
        try {
            return OffsetDateTime.parse(raw).toInstant();
        } catch (Exception ignored) {
            // suite
        }
        try {
            return LocalDateTime.parse(raw, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    .toInstant(ZoneOffset.UTC);
        } catch (Exception ignored) {
            return null;
        }
    }
}
