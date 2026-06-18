package fr.cachi.emplois.infrastructure.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.time.Instant;
import java.util.Map;

/**
 * Handler du scan périodique (déclenché par EventBridge Scheduler).
 *
 * <p>F1 : squelette. Le pipeline réel (fetch sources → normalise → dédoublonne → score → notifie)
 * sera implémenté aux features F4 à F9.</p>
 */
public class ScanHandler implements RequestHandler<Map<String, Object>, String> {

    @Override
    public String handleRequest(Map<String, Object> event, Context context) {
        Instant start = Instant.now();
        context.getLogger().log("Démarrage du scan périodique à " + start);
        // TODO F4-F9 : orchestration du pipeline de scan.
        context.getLogger().log("Scan terminé (squelette, aucun pipeline branché pour l'instant).");
        return "scan-ok";
    }
}
