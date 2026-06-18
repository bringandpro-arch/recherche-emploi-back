package fr.cachi.emplois.infrastructure.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import fr.cachi.emplois.domain.model.Profile;
import fr.cachi.emplois.domain.port.ProfileRepository;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiHandlerTest {

    private final ApiHandler handler = new ApiHandler(new InMemoryProfileRepository());

    @Test
    void health_renvoie_200_et_statut_ok() {
        APIGatewayV2HTTPResponse response = handler.handleRequest(event("GET", "/health", null, null), ctx());
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("\"status\":\"ok\""));
    }

    @Test
    void route_inconnue_renvoie_404() {
        APIGatewayV2HTTPResponse response = handler.handleRequest(event("GET", "/inexistant", null, null), ctx());
        assertEquals(404, response.getStatusCode());
    }

    @Test
    void profile_sans_jwt_renvoie_401() {
        APIGatewayV2HTTPResponse response = handler.handleRequest(event("GET", "/profile", null, null), ctx());
        assertEquals(401, response.getStatusCode());
    }

    @Test
    void get_profile_authentifie_renvoie_profil_par_defaut() {
        APIGatewayV2HTTPResponse response = handler.handleRequest(event("GET", "/profile", null, "user-123"), ctx());
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("\"userId\":\"user-123\""));
    }

    @Test
    void put_profile_persiste_puis_get_le_retrouve() {
        String body = "{\"label\":\"Archi Cloud\",\"skills\":[\"AWS\",\"Terraform\"],\"remoteMin\":80}";
        APIGatewayV2HTTPResponse put = handler.handleRequest(event("PUT", "/profile", body, "user-123"), ctx());
        assertEquals(200, put.getStatusCode());
        assertTrue(put.getBody().contains("Archi Cloud"));

        APIGatewayV2HTTPResponse get = handler.handleRequest(event("GET", "/profile", null, "user-123"), ctx());
        assertTrue(get.getBody().contains("Terraform"));
        assertTrue(get.getBody().contains("\"remoteMin\":80"));
    }

    // ───────────────────────── helpers ─────────────────────────

    private static APIGatewayV2HTTPEvent event(String method, String path, String body, String userId) {
        APIGatewayV2HTTPEvent event = new APIGatewayV2HTTPEvent();
        event.setRawPath(path);
        event.setBody(body);
        var http = new APIGatewayV2HTTPEvent.RequestContext.Http();
        http.setMethod(method);
        http.setPath(path);
        var ctx = new APIGatewayV2HTTPEvent.RequestContext();
        ctx.setHttp(http);
        if (userId != null) {
            var jwt = new APIGatewayV2HTTPEvent.RequestContext.Authorizer.JWT();
            jwt.setClaims(Map.of("sub", userId));
            var authorizer = new APIGatewayV2HTTPEvent.RequestContext.Authorizer();
            authorizer.setJwt(jwt);
            ctx.setAuthorizer(authorizer);
        }
        event.setRequestContext(ctx);
        return event;
    }

    private static Context ctx() {
        return new NoopContext();
    }

    /** Repository en mémoire pour les tests (pas d'appel AWS). */
    static class InMemoryProfileRepository implements ProfileRepository {
        private final Map<String, Profile> store = new HashMap<>();
        public Optional<Profile> findByUserId(String userId) { return Optional.ofNullable(store.get(userId)); }
        public Profile save(Profile profile) { store.put(profile.userId(), profile); return profile; }
        public List<Profile> findAllActive() { return store.values().stream().filter(Profile::active).toList(); }
    }

    private static class NoopContext implements Context {
        public String getAwsRequestId() { return "test"; }
        public String getLogGroupName() { return ""; }
        public String getLogStreamName() { return ""; }
        public String getFunctionName() { return "test"; }
        public String getFunctionVersion() { return "1"; }
        public String getInvokedFunctionArn() { return ""; }
        public com.amazonaws.services.lambda.runtime.CognitoIdentity getIdentity() { return null; }
        public com.amazonaws.services.lambda.runtime.ClientContext getClientContext() { return null; }
        public int getRemainingTimeInMillis() { return 30000; }
        public int getMemoryLimitInMB() { return 1024; }
        public LambdaLogger getLogger() {
            return new LambdaLogger() {
                public void log(String message) { }
                public void log(byte[] message) { }
            };
        }
    }
}
