package fr.cachi.emplois.infrastructure.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiHandlerTest {

    private final ApiHandler handler = new ApiHandler();

    @Test
    void health_renvoie_200_et_statut_ok() {
        APIGatewayV2HTTPEvent event = new APIGatewayV2HTTPEvent();
        event.setRawPath("/health");
        var http = new APIGatewayV2HTTPEvent.RequestContext.Http();
        http.setMethod("GET");
        http.setPath("/health");
        var ctx = new APIGatewayV2HTTPEvent.RequestContext();
        ctx.setHttp(http);
        event.setRequestContext(ctx);

        APIGatewayV2HTTPResponse response = handler.handleRequest(event, new NoopContext());

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("\"status\":\"ok\""));
    }

    @Test
    void route_inconnue_renvoie_404() {
        APIGatewayV2HTTPEvent event = new APIGatewayV2HTTPEvent();
        event.setRawPath("/inexistant");
        var http = new APIGatewayV2HTTPEvent.RequestContext.Http();
        http.setMethod("GET");
        http.setPath("/inexistant");
        var ctx = new APIGatewayV2HTTPEvent.RequestContext();
        ctx.setHttp(http);
        event.setRequestContext(ctx);

        APIGatewayV2HTTPResponse response = handler.handleRequest(event, new NoopContext());

        assertEquals(404, response.getStatusCode());
    }

    /** Contexte Lambda minimal pour les tests. */
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
