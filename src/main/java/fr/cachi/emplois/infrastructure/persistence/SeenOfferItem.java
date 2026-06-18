package fr.cachi.emplois.infrastructure.persistence;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

/** Représentation DynamoDB de l'historique « offre déjà vue » (PK userId, SK dedupKey). */
@DynamoDbBean
public class SeenOfferItem {

    private String userId;
    private String dedupKey;
    private String firstSeenAt;

    @DynamoDbPartitionKey
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    @DynamoDbSortKey
    public String getDedupKey() { return dedupKey; }
    public void setDedupKey(String dedupKey) { this.dedupKey = dedupKey; }

    public String getFirstSeenAt() { return firstSeenAt; }
    public void setFirstSeenAt(String firstSeenAt) { this.firstSeenAt = firstSeenAt; }
}
