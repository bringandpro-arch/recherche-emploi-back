package fr.cachi.emplois.infrastructure.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import fr.cachi.emplois.infrastructure.json.Json;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handler principal de l'API HTTP (API Gateway, payload v2).
 *
 * <p>F1 : route /health (publique). Les routes métier (profil, offres…) seront branchées
 * au fil des features suivantes.</p>
 */
public class ApiHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        String method = method(event);
        String path = path(event);
        context.getLogger().log("Requête API reçue : " + method + " " + path);

        if ("GET".equalsIgnoreCase(method) && path.endsWith("/health")) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", "ok");
            body.put("service", "recherche-emploi-back");
            body.put("stage", System.getenv().getOrDefault("STAGE", "dev"));
            return json(200, Json.toJson(body));
        }

        return json(404, "{\"erreur\":\"route inconnue\"}");
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

    private static APIGatewayV2HTTPResponse json(int status, String body) {
        APIGatewayV2HTTPResponse response = new APIGatewayV2HTTPResponse();
        response.setStatusCode(status);
        response.setHeaders(Map.of("Content-Type", "application/json; charset=utf-8"));
        response.setBody(body);
        return response;
    }
}
