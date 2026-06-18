package fr.cachi.emplois.infrastructure.persistence;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.util.List;

/**
 * Représentation DynamoDB d'une offre scorée (PK userId, SK offerDedupKey).
 * Champs d'offre dénormalisés pour un affichage direct (F10) sans jointure.
 */
@DynamoDbBean
public class ScoredOfferItem {

    private String userId;
    private String offerDedupKey;
    private Integer score;
    private Integer ruleScore;
    private Integer llmScore;
    private String confidenceLabel;
    private List<String> reasons;
    private Boolean freelanceConvertible;
    private String scoredAt;

    // Offre dénormalisée
    private String source;
    private String title;
    private String company;
    private String city;
    private String contractType;
    private Integer remotePercent;
    private Integer salaryMin;
    private Integer salaryMax;
    private Integer tjmMin;
    private Integer tjmMax;
    private List<String> stack;
    private String url;
    private String publishedAt;

    @DynamoDbPartitionKey
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    @DynamoDbSortKey
    public String getOfferDedupKey() { return offerDedupKey; }
    public void setOfferDedupKey(String offerDedupKey) { this.offerDedupKey = offerDedupKey; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public Integer getRuleScore() { return ruleScore; }
    public void setRuleScore(Integer ruleScore) { this.ruleScore = ruleScore; }

    public Integer getLlmScore() { return llmScore; }
    public void setLlmScore(Integer llmScore) { this.llmScore = llmScore; }

    public String getConfidenceLabel() { return confidenceLabel; }
    public void setConfidenceLabel(String confidenceLabel) { this.confidenceLabel = confidenceLabel; }

    public List<String> getReasons() { return reasons; }
    public void setReasons(List<String> reasons) { this.reasons = reasons; }

    public Boolean getFreelanceConvertible() { return freelanceConvertible; }
    public void setFreelanceConvertible(Boolean freelanceConvertible) { this.freelanceConvertible = freelanceConvertible; }

    public String getScoredAt() { return scoredAt; }
    public void setScoredAt(String scoredAt) { this.scoredAt = scoredAt; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getContractType() { return contractType; }
    public void setContractType(String contractType) { this.contractType = contractType; }

    public Integer getRemotePercent() { return remotePercent; }
    public void setRemotePercent(Integer remotePercent) { this.remotePercent = remotePercent; }

    public Integer getSalaryMin() { return salaryMin; }
    public void setSalaryMin(Integer salaryMin) { this.salaryMin = salaryMin; }

    public Integer getSalaryMax() { return salaryMax; }
    public void setSalaryMax(Integer salaryMax) { this.salaryMax = salaryMax; }

    public Integer getTjmMin() { return tjmMin; }
    public void setTjmMin(Integer tjmMin) { this.tjmMin = tjmMin; }

    public Integer getTjmMax() { return tjmMax; }
    public void setTjmMax(Integer tjmMax) { this.tjmMax = tjmMax; }

    public List<String> getStack() { return stack; }
    public void setStack(List<String> stack) { this.stack = stack; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getPublishedAt() { return publishedAt; }
    public void setPublishedAt(String publishedAt) { this.publishedAt = publishedAt; }
}
