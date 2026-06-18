package fr.cachi.emplois.domain.model;

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
        String r = raw.toLowerCase();
        if (r.contains("freelance") || r.contains("indépendant") || r.contains("independant")
                || r.contains("mission") || r.contains("régie") || r.contains("regie")) {
            return FREELANCE;
        }
        if (r.contains("cdi")) {
            return CDI;
        }
        if (r.contains("cdd") || r.contains("intérim") || r.contains("interim")) {
            return CDD;
        }
        return UNKNOWN;
    }
}
