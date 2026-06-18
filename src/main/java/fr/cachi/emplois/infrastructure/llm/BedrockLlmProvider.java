package fr.cachi.emplois.infrastructure.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.cachi.emplois.domain.model.LlmScore;
import fr.cachi.emplois.domain.model.Offer;
import fr.cachi.emplois.domain.model.Profile;
import fr.cachi.emplois.domain.port.LlmProvider;
import fr.cachi.emplois.infrastructure.json.Json;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Provider IA via Amazon Bedrock (modèle Claude). Construit le prompt à partir des gabarits,
 * appelle Bedrock et parse la réponse JSON. Modèle configurable via {@code LLM_MODEL}.
 */
public class BedrockLlmProvider implements LlmProvider {

    private final String model;
    private final String systemPrompt;
    private final Region region;
    private volatile BedrockRuntimeClient client;

    public BedrockLlmProvider() {
        this(System.getenv().getOrDefault("LLM_MODEL", "anthropic.claude-haiku-4-5"),
                System.getenv().getOrDefault("BEDROCK_REGION", "eu-west-1"));
    }

    public BedrockLlmProvider(String model, String region) {
        this.model = model;
        this.region = Region.of(region);
        this.systemPrompt = Prompts.load("prompts/scoring-system.txt");
    }

    @Override
    public boolean available() {
        String provider = System.getenv().getOrDefault("LLM_PROVIDER", "bedrock");
        return "bedrock".equalsIgnoreCase(provider);
    }

    @Override
    public LlmScore score(Profile profile, Offer offer) {
        String userMessage = Prompts.scoringUser(profile, offer);
        String responseText = invoke(userMessage);
        return parse(responseText);
    }

    private String invoke(String userMessage) {
        ObjectNode body = Json.mapper().createObjectNode();
        body.put("anthropic_version", "bedrock-2023-05-31");
        body.put("max_tokens", 500);
        body.put("system", systemPrompt);
        ArrayNode messages = body.putArray("messages");
        ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");
        ArrayNode content = userMsg.putArray("content");
        ObjectNode textBlock = content.addObject();
        textBlock.put("type", "text");
        textBlock.put("text", userMessage);

        InvokeModelRequest request = InvokeModelRequest.builder()
                .modelId(model)
                .contentType("application/json")
                .accept("application/json")
                .body(SdkBytes.fromUtf8String(body.toString()))
                .build();

        InvokeModelResponse response = client().invokeModel(request);
        return response.body().asUtf8String();
    }

    private BedrockRuntimeClient client() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    client = BedrockRuntimeClient.builder().region(region).build();
                }
            }
        }
        return client;
    }

    /** Extrait le texte de la réponse Bedrock puis parse le JSON métier. */
    static LlmScore parse(String bedrockJson) {
        try {
            JsonNode root = Json.mapper().readTree(bedrockJson);
            String text = extractText(root);
            JsonNode obj = Json.mapper().readTree(extractJsonObject(text));

            int score = obj.path("score").asInt(0);
            String confidence = obj.path("confidence").asText("moyen");
            boolean freelance = obj.path("freelance_convertible").asBoolean(false);
            List<String> reasons = new ArrayList<>();
            JsonNode r = obj.get("reasons");
            if (r != null && r.isArray()) {
                r.forEach(n -> reasons.add(n.asText()));
            }
            return new LlmScore(Math.max(0, Math.min(100, score)), confidence, reasons, freelance);
        } catch (Exception e) {
            throw new IllegalStateException("Réponse Bedrock non interprétable", e);
        }
    }

    /** Réponse Anthropic sur Bedrock : { "content": [ { "type":"text", "text":"..." } ] }. */
    private static String extractText(JsonNode root) {
        JsonNode content = root.get("content");
        if (content != null && content.isArray()) {
            for (JsonNode block : content) {
                if ("text".equals(block.path("type").asText())) {
                    return block.path("text").asText("");
                }
            }
        }
        return root.path("completion").asText(""); // repli formats anciens
    }

    /** Isole le premier objet JSON du texte (le modèle peut ajouter du texte autour). */
    static String extractJsonObject(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }
}
