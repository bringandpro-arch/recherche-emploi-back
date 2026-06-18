package fr.cachi.emplois.domain.model;

import java.text.Normalizer;
import java.util.Locale;

/** Type de contrat normalisé. */
public enum ContractType {
    FREELANCE,
    CDI,
    CDD,
    UNKNOWN;

    public static ContractType fromLabel(String raw) {
        if (raw == null) {
            return UNKNOWN;
        }
        String r = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);

        if (r.contains("freelance") || r.contains("independant") || r.contains("mission")
                || r.contains("regie") || r.contains("portage")) {
            return FREELANCE;
        }
        // "indetermine" doit être testé avant "determine" (sous-chaîne).
        if (r.contains("cdi") || r.contains("duree indetermin") || r.contains("indetermin")) {
            return CDI;
        }
        if (r.contains("cdd") || r.contains("duree determin") || r.contains("determin")
                || r.contains("interim") || r.contains("temporaire")) {
            return CDD;
        }
        return UNKNOWN;
    }
}
