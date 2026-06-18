package fr.cachi.emplois.infrastructure.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import fr.cachi.emplois.application.OffersFilter;
import fr.cachi.emplois.application.OffersQueryService;
import fr.cachi.emplois.application.ProfileService;
import fr.cachi.emplois.domain.model.ContractType;
import fr.cachi.emplois.domain.model.Profile;
import fr.cachi.emplois.domain.port.ProfileRepository;
import fr.cachi.emplois.domain.port.ScoredOfferRepository;
import fr.cachi.emplois.infrastructure.config.Wiring;
import fr.cachi.emplois.infrastructure.json.Json;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Handler principal de l'API HTTP (API Gateway, payload v2).
 *
 * <p>Routes :</p>
 * <ul>
 *   <li>{@code GET /health} — publique (F1)</li>
 *   <li>{@code GET|PUT /profile} — profil de l'utilisateur authentifié (F3)</li>
 *   <li>{@code GET /offers} — offres scorées filtrées (F10)</li>
 * </ul>
 */
public class ApiHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private final ProfileService profileService;
    private final OffersQueryService offersQueryService;

    /** Constructeur Lambda : câblage DynamoDB via Wiring. */
    public ApiHandler() {
        this(Wiring.profileService(), Wiring.scoredOfferRepository());
    }

    public ApiHandler(ProfileService profileService, ScoredOfferRepository scoredRepository) {
        this.profileService = profileService;
        this.offersQueryService = new OffersQueryService(scoredRepository);
    }

    /** Surcharge pratique pour les tests. */
    public ApiHandler(ProfileRepository profileRepository, ScoredOfferRepository scoredRepository) {
        this(new ProfileService(profileRepository), scoredRepository);
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        String method = method(event);
        String path = normalize(path(event));
        context.getLogger().log("Requête API : " + method + " " + path);

        try {
            if ("GET".equalsIgnoreCase(method) && path.equals("/health")) {
                return handleHealth();
            }

            Optional<String> userId = AuthContext.userId(event);
            if (path.equals("/profile") || path.equals("/offers")) {
                if (userId.isEmpty()) {
                    return json(401, "{\"erreur\":\"non authentifié\"}");
                }
            }

            if (path.equals("/profile")) {
                if ("GET".equalsIgnoreCase(method)) {
                    return json(200, Json.toJson(profileService.getOrDefault(userId.get())));
                }
                if ("PUT".equalsIgnoreCase(method) || "POST".equalsIgnoreCase(method)) {
                    Profile input = Json.fromJson(
                            event.getBody() == null ? "{}" : event.getBody(), Profile.class);
                    return json(200, Json.toJson(profileService.save(userId.get(), input)));
                }
            }

            if (path.equals("/offers") && "GET".equalsIgnoreCase(method)) {
                OffersFilter filter = parseFilter(event.getQueryStringParameters());
                return json(200, Json.toJson(offersQueryService.list(userId.get(), filter)));
            }

            return json(404, "{\"erreur\":\"route inconnue\"}");
        } catch (IllegalArgumentException e) {
            context.getLogger().log("Requête invalide : " + e.getMessage());
            return json(400, "{\"erreur\":\"requête invalide\"}");
        } catch (Exception e) {
            context.getLogger().log("Erreur interne : " + e);
            return json(500, "{\"erreur\":\"erreur interne\"}");
        }
    }

    static OffersFilter parseFilter(Map<String, String> qs) {
        if (qs == null) {
            return OffersFilter.none();
        }
        ContractType contract = null;
        if (qs.get("contract") != null) {
            try {
                contract = ContractType.valueOf(qs.get("contract").toUpperCase());
            } catch (Exception ignored) {
                // filtre ignoré si valeur invalide
            }
        }
        return new OffersFilter(contract, parseInt(qs.get("remoteMin")), qs.get("location"),
                parseInt(qs.get("minScore")));
    }

    private static Integer parseInt(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private APIGatewayV2HTTPResponse handleHealth() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "ok");
        body.put("service", "recherche-emploi-back");
        body.put("stage", System.getenv().getOrDefault("STAGE", "dev"));
        return json(200, Json.toJson(body));
    }

    private static String method(APIGatewayV2HTTPEvent event) {
        if (event.getRequestContext() != null && event.getRequestContext().getHttp() != null) {
            return event.getRequestContext().getHttp().getMethod();
        }
        return "GET";
    }

    private static String path(APIGatewayV2HTTPEvent event) {
        if (event.getRawPath() != null) {
            return event.getRawPath();
        }
        if (event.getRequestContext() != null && event.getRequestContext().getHttp() != null) {
            return event.getRequestContext().getHttp().getPath();
        }
        return "";
    }

    /** Retire le préfixe de stage éventuel et les "/" superflus pour matcher les routes. */
    static String normalize(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return "/";
        }
        String p = rawPath;
        if (p.endsWith("/") && p.length() > 1) {
            p = p.substring(0, p.length() - 1);
        }
        for (String known : new String[]{"/health", "/profile", "/offers"}) {
            if (p.equals(known) || p.endsWith(known)) {
                return known;
            }
        }
        return p;
    }

    private static APIGatewayV2HTTPResponse json(int status, String body) {
        APIGatewayV2HTTPResponse response = new APIGatewayV2HTTPResponse();
        response.setStatusCode(status);
        response.setHeaders(Map.of("Content-Type", "application/json; charset=utf-8"));
        response.setBody(body);
        return response;
    }
}
