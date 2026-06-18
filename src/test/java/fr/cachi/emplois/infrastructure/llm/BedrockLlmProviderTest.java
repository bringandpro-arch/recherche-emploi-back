package fr.cachi.emplois.infrastructure.llm;

import fr.cachi.emplois.domain.model.ContractType;
import fr.cachi.emplois.domain.model.ExtractedFields;
import fr.cachi.emplois.domain.model.LlmScore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BedrockLlmProviderTest {

    @Test
    void parse_reponse_bedrock_anthropic() {
        String bedrock = """
            {"content":[{"type":"text","text":"{\\"score\\":82,\\"confidence\\":\\"élevé\\",\\"reasons\\":[\\"AWS et Terraform présents\\"],\\"freelance_convertible\\":true}"}]}
            """;
        LlmScore s = BedrockLlmProvider.parse(bedrock);
        assertEquals(82, s.score());
        assertEquals("élevé", s.confidence());
        assertTrue(s.freelanceConvertible());
        assertTrue(s.reasons().get(0).contains("AWS"));
    }

    @Test
    void extrait_objet_json_meme_avec_texte_autour() {
        String text = "Voici le résultat : {\"score\":50} merci";
        assertEquals("{\"score\":50}", BedrockLlmProvider.extractJsonObject(text));
    }

    @Test
    void parse_extraction_structuree() {
        String bedrock = """
            {"content":[{"type":"text","text":"{\\"contract_type\\":\\"FREELANCE\\",\\"remote_percent\\":100,\\"stack\\":[\\"aws\\",\\"terraform\\"],\\"tjm_min\\":600,\\"tjm_max\\":750,\\"salary_min\\":null,\\"salary_max\\":null,\\"currency\\":\\"EUR\\",\\"location\\":\\"Lyon\\"}"}]}
            """;
        ExtractedFields ex = BedrockLlmProvider.parseExtraction(bedrock);
        assertEquals(ContractType.FREELANCE, ex.contractType());
        assertEquals(100, ex.remotePercent());
        assertTrue(ex.stack().contains("terraform"));
        assertEquals(600, ex.tjmMin());
        assertNull(ex.salaryMin());
        assertEquals("EUR", ex.currency());
        assertEquals("Lyon", ex.location());
    }
}
