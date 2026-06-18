package fr.cachi.emplois.infrastructure.persistence;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Fournit des clients DynamoDB partagés (réutilisés entre invocations Lambda chaudes).
 */
public final class Dynamo {

    private static volatile DynamoDbClient client;
    private static volatile DynamoDbEnhancedClient enhanced;

    private Dynamo() {
    }

    public static DynamoDbClient client() {
        if (client == null) {
            synchronized (Dynamo.class) {
                if (client == null) {
                    client = DynamoDbClient.create();
                }
            }
        }
        return client;
    }

    public static DynamoDbEnhancedClient enhanced() {
        if (enhanced == null) {
            synchronized (Dynamo.class) {
                if (enhanced == null) {
                    enhanced = DynamoDbEnhancedClient.builder()
                            .dynamoDbClient(client())
                            .build();
                }
            }
        }
        return enhanced;
    }

    /** Nom de table depuis l'environnement (défini par SAM), avec repli pour le local. */
    public static String table(String envVar, String fallback) {
        String v = System.getenv(envVar);
        return v == null || v.isBlank() ? fallback : v;
    }
}
