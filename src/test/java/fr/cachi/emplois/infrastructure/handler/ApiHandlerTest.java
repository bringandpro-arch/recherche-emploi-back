package fr.cachi.emplois.infrastructure.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import fr.cachi.emplois.domain.model.ContractType;
import fr.cachi.emplois.domain.model.Offer;
import fr.cachi.emplois.domain.model.Profile;
import fr.cachi.emplois.domain.model.ScoreResult;
import fr.cachi.emplois.domain.model.ScoredOffer;
import fr.cachi.emplois.domain.port.ProfileRepository;
import fr.cachi.emplois.domain.port.ScoredOfferRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiHandlerTest {

    private final ApiHandler handler = new ApiHandler(new InMemoryProfileRepository(), seededScored());

    @Test
    void health_renvoie_200_et_statut_ok() {
        APIGatewayV2HTTPResponse r = handler.handleRequest(event("GET", "/health", null, null, null), ctx());
        assertEquals(200, r.getStatusCode());
        assertTrue(r.getBody().contains("\"status\":\"ok\""));
    }

    @Test
    void route_inconnue_renvoie_404() {
        assertEquals(404, handler.handleRequest(event("GET", "/inexistant", null, null, null), ctx()).getStatusCode());
    }

    @Test
    void profile_sans_jwt_renvoie_401() {
        assertEquals(401, handler.handleRequest(event("GET", "/profile", null, null, null), ctx()).getStatusCode());
    }

    @Test
    void get_profile_authentifie_renvoie_profil_par_defaut() {
        APIGatewayV2HTTPResponse r = handler.handleRequest(event("GET", "/profile", null, "user-123", null), ctx());
        assertEquals(200, r.getStatusCode());
        assertTrue(r.getBody().contains("\"userId\":\"user-123\""));
    }

    @Test
    void put_profile_persiste_puis_get_le_retrouve() {
        String body = "{\"label\":\"Archi Cloud\",\"skills\":[\"AWS\",\"Terraform\"],\"remoteMin\":80}";
        APIGatewayV2HTTPResponse put = handler.handleRequest(event("PUT", "/profile", body, "user-123", null), ctx());
        assertEquals(200, put.getStatusCode());
        assertTrue(put.getBody().contains("Archi Cloud"));
        APIGatewayV2HTTPResponse get = handler.handleRequest(event("GET", "/profile", null, "user-123", null), ctx());
        assertTrue(get.getBody().contains("Terraform"));
    }

    @Test
    void offers_sans_jwt_renvoie_401() {
        assertEquals(401, handler.handleRequest(event("GET", "/offers", null, null, null), ctx()).getStatusCode());
    }

    @Test
    void offers_authentifie_renvoie_les_offres_triees() {
        APIGatewayV2HTTPResponse r = handler.handleRequest(event("GET", "/offers", null, "user-123", null), ctx());
        assertEquals(200, r.getStatusCode());
        assertTrue(r.getBody().contains("Architecte"));
        assertTrue(r.getBody().contains("Dev Java"));
    }

    @Test
    void offers_filtre_par_contrat_freelance() {
        APIGatewayV2HTTPResponse r = handler.handleRequest(
                event("GET", "/offers", null, "user-123", Map.of("contract", "FREELANCE")), ctx());
        assertEquals(200, r.getStatusCode());
        assertTrue(r.getBody().contains("Architecte"));
        assertFalse(r.getBody().contains("Dev Java"), "le CDI doit être filtré");
    }

    // ───────────────────────── helpers ─────────────────────────

    private static ScoredOfferRepository seededScored() {
        InMemoryScored repo = new InMemoryScored();
        repo.save(scored("user-123", "Architecte Cloud", ContractType.FREELANCE, 90));
        repo.save(scored("user-123", "Dev Java", ContractType.CDI, 70));
        return repo;
    }

    private static ScoredOffer scored(String userId, String title, ContractType contract, int score) {
        Offer o = new Offer("remotive", "1", title, "Acme", "Lyon", "Lyon", "France", 100,
                contract, null, null, null, null, "EUR", List.of(), "url", "desc",
                Instant.now(), Instant.now(), title);
        ScoreResult r = new ScoreResult(score, score, null, List.of(), ScoreResult.labelFor(score), false);
        return new ScoredOffer(userId, o, r, Instant.now());
    }

    private static APIGatewayV2HTTPEvent event(String method, String path, String body, String userId,
                                               Map<String, String> qs) {
        APIGatewayV2HTTPEvent event = new APIGatewayV2HTTPEvent();
        event.setRawPath(path);
        event.setBody(body);
        event.setQueryStringParameters(qs);
        var http = new APIGatewayV2HTTPEvent.RequestContext.Http();
        http.setMethod(method);
        http.setPath(path);
        var rc = new APIGatewayV2HTTPEvent.RequestContext();
        rc.setHttp(http);
        if (userId != null) {
            var jwt = new APIGatewayV2HTTPEvent.RequestContext.Authorizer.JWT();
            jwt.setClaims(Map.of("sub", userId));
            var authorizer = new APIGatewayV2HTTPEvent.RequestContext.Authorizer();
            authorizer.setJwt(jwt);
            rc.setAuthorizer(authorizer);
        }
        event.setRequestContext(rc);
        return event;
    }

    private static Context ctx() {
        return new NoopContext();
    }

    static class InMemoryProfileRepository implements ProfileRepository {
        private final Map<String, Profile> store = new HashMap<>();
        public Optional<Profile> findByUserId(String userId) { return Optional.ofNullable(store.get(userId)); }
        public Profile save(Profile profile) { store.put(profile.userId(), profile); return profile; }
        public List<Profile> findAllActive() { return store.values().stream().filter(Profile::active).toList(); }
    }

    static class InMemoryScored implements ScoredOfferRepository {
        private final List<ScoredOffer> store = new ArrayList<>();
        public void save(ScoredOffer s) { store.add(s); }
        public List<ScoredOffer> listByUser(String userId) {
            return store.stream().filter(s -> s.userId().equals(userId))
                    .sorted((a, b) -> Integer.compare(b.result().score(), a.result().score())).toList();
        }
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
