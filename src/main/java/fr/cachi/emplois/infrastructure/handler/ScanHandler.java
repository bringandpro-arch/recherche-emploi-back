package fr.cachi.emplois.infrastructure.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import fr.cachi.emplois.domain.model.ScanRun;
import fr.cachi.emplois.infrastructure.config.Wiring;

import java.util.Map;

/**
 * Handler du scan périodique (déclenché par EventBridge Scheduler).
 * Orchestre le pipeline complet via {@link fr.cachi.emplois.application.ScanService} (F8/F9).
 */
public class ScanHandler implements RequestHandler<Map<String, Object>, String> {

    @Override
    public String handleRequest(Map<String, Object> event, Context context) {
        context.getLogger().log("Démarrage du scan périodique");
        ScanRun run = Wiring.scanService().run();
        String summary = "scan " + run.status() + " : sources=" + run.sourcesQueried()
                + ", fetched=" + run.fetched() + ", new=" + run.newCount()
                + ", notified=" + run.notified();
        context.getLogger().log(summary);
        return summary;
    }
}
