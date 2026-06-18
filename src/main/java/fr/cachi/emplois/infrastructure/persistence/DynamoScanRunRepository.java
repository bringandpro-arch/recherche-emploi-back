package fr.cachi.emplois.infrastructure.persistence;

import fr.cachi.emplois.domain.model.ScanRun;
import fr.cachi.emplois.domain.port.ScanRunRepository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

/** Implémentation DynamoDB du {@link ScanRunRepository}. */
public class DynamoScanRunRepository implements ScanRunRepository {

    private final DynamoDbTable<ScanRunItem> table;

    public DynamoScanRunRepository() {
        this.table = Dynamo.enhanced().table(
                Dynamo.table("TABLE_SCANRUNS", "ScanRuns"),
                TableSchema.fromBean(ScanRunItem.class));
    }

    @Override
    public void save(ScanRun run) {
        ScanRunItem i = new ScanRunItem();
        i.setScanId(run.scanId());
        i.setStartedAt(run.startedAt() == null ? null : run.startedAt().toString());
        i.setEndedAt(run.endedAt() == null ? null : run.endedAt().toString());
        i.setStatus(run.status());
        i.setSourcesQueried(run.sourcesQueried());
        i.setFetched(run.fetched());
        i.setNewCount(run.newCount());
        i.setNotified(run.notified());
        i.setErrorSummary(run.errorSummary());
        table.putItem(i);
    }
}
