package fr.cachi.emplois.infrastructure.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConfigTest {

    @Test
    void variable_d_environnement_prioritaire_sur_ssm() {
        Map<String, String> ssm = Map.of("ADZUNA_APP_ID", "depuis-ssm");
        assertEquals("depuis-env",
                Config.resolve("ADZUNA_APP_ID", k -> "depuis-env", ssm));
    }

    @Test
    void repli_sur_ssm_quand_env_absente_ou_vide() {
        Map<String, String> ssm = Map.of("TELEGRAM_BOT_TOKEN", "secret-ssm");
        assertEquals("secret-ssm", Config.resolve("TELEGRAM_BOT_TOKEN", k -> null, ssm));
        assertEquals("secret-ssm", Config.resolve("TELEGRAM_BOT_TOKEN", k -> "   ", ssm));
    }

    @Test
    void null_quand_absente_partout() {
        assertNull(Config.resolve("INEXISTANT", k -> null, Map.of()));
        assertNull(Config.resolve("INEXISTANT", k -> "", Map.of("AUTRE", "x")));
    }

    @Test
    void getOrDefault_renvoie_le_defaut_si_absente() {
        // Clé improbable : ni en env, ni en SSM (hors Lambda => pas d'appel SSM).
        assertEquals("def", Config.getOrDefault("CLE_DE_TEST_IMPROBABLE_XYZ", "def"));
    }
}
