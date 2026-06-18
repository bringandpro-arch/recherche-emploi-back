package fr.cachi.emplois.infrastructure.persistence;

import fr.cachi.emplois.domain.model.ContractType;
import fr.cachi.emplois.domain.model.Offer;
import fr.cachi.emplois.domain.port.OfferRepository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Implémentation DynamoDB du {@link OfferRepository}. */
public class DynamoOfferRepository implements OfferRepository {

    private final DynamoDbTable<OfferItem> table;

    public DynamoOfferRepository() {
        this.table = Dynamo.enhanced().table(
                Dynamo.table("TABLE_OFFERS", "Offers"),
                TableSchema.fromBean(OfferItem.class));
    }

    @Override
    public void upsert(Offer offer) {
        table.putItem(toItem(offer));
    }

    @Override
    public Optional<Offer> findByDedupKey(String dedupKey) {
        OfferItem item = table.getItem(Key.builder().partitionValue(dedupKey).build());
        return Optional.ofNullable(item).map(DynamoOfferRepository::toDomain);
    }

    static OfferItem toItem(Offer o) {
        OfferItem i = new OfferItem();
        i.setDedupKey(o.dedupKey());
        i.setSourceRef(o.source() + "#" + o.sourceExternalId());
        i.setSource(o.source());
        i.setSourceExternalId(o.sourceExternalId());
        i.setTitle(o.title());
        i.setCompany(o.company());
        i.setLocationRaw(o.locationRaw());
        i.setCity(o.city());
        i.setCountry(o.country());
        i.setRemotePercent(o.remotePercent());
        i.setContractType(o.contractType() == null ? null : o.contractType().name());
        i.setSalaryMin(o.salaryMin());
        i.setSalaryMax(o.salaryMax());
        i.setTjmMin(o.tjmMin());
        i.setTjmMax(o.tjmMax());
        i.setCurrency(o.currency());
        i.setStack(o.stack());
        i.setUrl(o.url());
        i.setDescriptionRaw(o.descriptionRaw());
        i.setPublishedAt(o.publishedAt() == null ? null : o.publishedAt().toString());
        i.setFetchedAt(o.fetchedAt() == null ? null : o.fetchedAt().toString());
        return i;
    }

    static Offer toDomain(OfferItem i) {
        return new Offer(
                i.getSource(), i.getSourceExternalId(), i.getTitle(), i.getCompany(),
                i.getLocationRaw(), i.getCity(), i.getCountry(), i.getRemotePercent(),
                parseContract(i.getContractType()), i.getSalaryMin(), i.getSalaryMax(),
                i.getTjmMin(), i.getTjmMax(), i.getCurrency(),
                i.getStack() == null ? List.of() : i.getStack(), i.getUrl(), i.getDescriptionRaw(),
                parseInstant(i.getPublishedAt()), parseInstant(i.getFetchedAt()), i.getDedupKey());
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
