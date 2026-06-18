package fr.cachi.emplois.infrastructure.persistence;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

/** Représentation DynamoDB d'une exécution de scan. */
@DynamoDbBean
public class ScanRunItem {

    private String scanId;
    private String startedAt;
    private String endedAt;
    private String status;
    private Integer sourcesQueried;
    private Integer fetched;
    private Integer newCount;
    private Integer notified;
    private String errorSummary;

    @DynamoDbPartitionKey
    public String getScanId() { return scanId; }
    public void setScanId(String scanId) { this.scanId = scanId; }

    public String getStartedAt() { return startedAt; }
    public void setStartedAt(String startedAt) { this.startedAt = startedAt; }

    public String getEndedAt() { return endedAt; }
    public void setEndedAt(String endedAt) { this.endedAt = endedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getSourcesQueried() { return sourcesQueried; }
    public void setSourcesQueried(Integer sourcesQueried) { this.sourcesQueried = sourcesQueried; }

    public Integer getFetched() { return fetched; }
    public void setFetched(Integer fetched) { this.fetched = fetched; }

    public Integer getNewCount() { return newCount; }
    public void setNewCount(Integer newCount) { this.newCount = newCount; }

    public Integer getNotified() { return notified; }
    public void setNotified(Integer notified) { this.notified = notified; }

    public String getErrorSummary() { return errorSummary; }
    public void setErrorSummary(String errorSummary) { this.errorSummary = errorSummary; }
}
