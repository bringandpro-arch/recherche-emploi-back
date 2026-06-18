package fr.cachi.emplois.infrastructure.persistence;

import fr.cachi.emplois.domain.port.SeenOfferRepository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.time.Instant;

/** Implémentation DynamoDB du {@link SeenOfferRepository}. */
public class DynamoSeenOfferRepository implements SeenOfferRepository {

    private final DynamoDbTable<SeenOfferItem> table;

    public DynamoSeenOfferRepository() {
        this.table = Dynamo.enhanced().table(
                Dynamo.table("TABLE_SEEN", "SeenOffers"),
                TableSchema.fromBean(SeenOfferItem.class));
    }

    @Override
    public boolean isSeen(String userId, String dedupKey) {
        return table.getItem(Key.builder().partitionValue(userId).sortValue(dedupKey).build()) != null;
    }

    @Override
    public void markSeen(String userId, String dedupKey, Instant when) {
        SeenOfferItem item = new SeenOfferItem();
        item.setUserId(userId);
        item.setDedupKey(dedupKey);
        item.setFirstSeenAt((when == null ? Instant.now() : when).toString());
        table.putItem(item);
    }
}
