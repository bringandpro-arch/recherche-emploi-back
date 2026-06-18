package fr.cachi.emplois.infrastructure.llm;

import fr.cachi.emplois.domain.model.LlmScore;
import fr.cachi.emplois.domain.model.Offer;
import fr.cachi.emplois.domain.model.Profile;
import fr.cachi.emplois.domain.port.LlmProvider;

import java.util.List;

/** Provider IA neutre : désactive la couche IA (scoring 100% règles). */
public class NoopLlmProvider implements LlmProvider {

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public LlmScore score(Profile profile, Offer offer) {
        return new LlmScore(0, "faible", List.of(), false);
    }
}
