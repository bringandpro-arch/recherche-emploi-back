package fr.cachi.emplois.infrastructure.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Mapper JSON partagé (Jackson) configuré une fois pour toute l'application.
 */
public final class Json {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private Json() {
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    /** Sérialise un objet en JSON. Lève une RuntimeException en cas d'erreur (cas non nominal). */
    public static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Échec de sérialisation JSON", e);
        }
    }

    /** Désérialise du JSON vers le type demandé. */
    public static <T> T fromJson(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalArgumentException("Échec de désérialisation JSON", e);
        }
    }
}
