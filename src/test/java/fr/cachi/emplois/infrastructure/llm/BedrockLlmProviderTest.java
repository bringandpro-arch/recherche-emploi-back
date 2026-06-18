package fr.cachi.emplois.infrastructure.llm;

import fr.cachi.emplois.domain.model.LlmScore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
