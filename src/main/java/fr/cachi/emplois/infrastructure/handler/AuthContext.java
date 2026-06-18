package fr.cachi.emplois.infrastructure.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;

import java.util.Optional;

/**
 * Extraction de l'identité de l'utilisateur authentifié depuis le JWT Cognito
 * validé par l'authorizer de l'API Gateway.
 */
public final class AuthContext {

    private AuthContext() {
    }

    /** Retourne le {@code sub} Cognito (identifiant utilisateur) si présent. */
    public static Optional<String> userId(APIGatewayV2HTTPEvent event) {
        try {
            var ctx = event.getRequestContext();
            if (ctx == null || ctx.getAuthorizer() == null || ctx.getAuthorizer().getJwt() == null) {
                return Optional.empty();
            }
            var claims = ctx.getAuthorizer().getJwt().getClaims();
            if (claims == null) {
                return Optional.empty();
            }
            String sub = claims.get("sub");
            return Optional.ofNullable(sub).filter(s -> !s.isBlank());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
