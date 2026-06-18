package fr.cachi.emplois.infrastructure.persistence;

import fr.cachi.emplois.domain.model.ContractType;
import fr.cachi.emplois.domain.model.Offer;
import fr.cachi.emplois.domain.model.ScoreResult;
import fr.cachi.emplois.domain.model.ScoredOffer;
import fr.cachi.emplois.domain.port.ScoredOfferRepository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/** Implémentation DynamoDB du {@link ScoredOfferRepository}. */
public class DynamoScoredOfferRepository implements ScoredOfferRepository {

    private final DynamoDbTable<ScoredOfferItem> table;

    public DynamoScoredOfferRepository() {
        this.table = Dynamo.enhanced().table(
                Dynamo.table("TABLE_SCORED", "ScoredOffers"),
                TableSchema.fromBean(ScoredOfferItem.class));
    }

    @Override
    public void save(ScoredOffer s) {
        table.putItem(toItem(s));
    }

    @Override
    public List<ScoredOffer> listByUser(String userId) {
        return table.query(r -> r.queryConditional(
                        software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional
                                .keyEqualTo(Key.builder().partitionValue(userId).build())))
                .items().stream()
                .map(DynamoScoredOfferRepository::toDomain)
                .sorted(Comparator.comparingInt((ScoredOffer s) -> s.result().score()).reversed())
                .toList();
    }

    static ScoredOfferItem toItem(ScoredOffer s) {
        Offer o = s.offer();
        ScoreResult r = s.result();
        ScoredOfferItem i = new ScoredOfferItem();
        i.setUserId(s.userId());
        i.setOfferDedupKey(o.dedupKey());
        i.setScore(r.score());
        i.setRuleScore(r.ruleScore());
        i.setLlmScore(r.llmScore());
        i.setConfidenceLabel(r.confidenceLabel());
        i.setReasons(r.reasons());
        i.setFreelanceConvertible(r.freelanceConvertible());
        i.setScoredAt(s.scoredAt() == null ? null : s.scoredAt().toString());
        i.setSource(o.source());
        i.setTitle(o.title());
        i.setCompany(o.company());
        i.setCity(o.city());
        i.setContractType(o.contractType() == null ? null : o.contractType().name());
        i.setRemotePercent(o.remotePercent());
        i.setSalaryMin(o.salaryMin());
        i.setSalaryMax(o.salaryMax());
        i.setTjmMin(o.tjmMin());
        i.setTjmMax(o.tjmMax());
        i.setStack(o.stack());
        i.setUrl(o.url());
        i.setPublishedAt(o.publishedAt() == null ? null : o.publishedAt().toString());
        return i;
    }

    static ScoredOffer toDomain(ScoredOfferItem i) {
        Offer o = new Offer(i.getSource(), null, i.getTitle(), i.getCompany(), null, i.getCity(), null,
                i.getRemotePercent(), parseContract(i.getContractType()), i.getSalaryMin(), i.getSalaryMax(),
                i.getTjmMin(), i.getTjmMax(), null, i.getStack() == null ? List.of() : i.getStack(),
                i.getUrl(), null, parseInstant(i.getPublishedAt()), null, i.getOfferDedupKey());
        ScoreResult r = new ScoreResult(
                i.getScore() == null ? 0 : i.getScore(),
                i.getRuleScore() == null ? 0 : i.getRuleScore(),
                i.getLlmScore(),
                i.getReasons() == null ? List.of() : i.getReasons(),
                i.getConfidenceLabel(),
                i.getFreelanceConvertible() != null && i.getFreelanceConvertible());
        return new ScoredOffer(i.getUserId(), o, r, parseInstant(i.getScoredAt()));
    }

    private static ContractType parseContract(String s) {
        if (s == null) {
            return ContractType.UNKNOWN;
        }
        try {
            return ContractType.valueOf(s);
        } catch (Exception e) {
            return ContractType.UNKNOWN;
        }
    }

    private static Instant parseInstant(String s) {
        return s == null ? null : Instant.parse(s);
    }
}
