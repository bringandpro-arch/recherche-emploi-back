package fr.cachi.emplois.infrastructure.persistence;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

import java.util.List;

/** Représentation DynamoDB d'une offre normalisée. */
@DynamoDbBean
public class OfferItem {

    private String dedupKey;
    private String sourceRef;
    private String source;
    private String sourceExternalId;
    private String title;
    private String company;
    private String locationRaw;
    private String city;
    private String country;
    private Integer remotePercent;
    private String contractType;
    private Integer salaryMin;
    private Integer salaryMax;
    private Integer tjmMin;
    private Integer tjmMax;
    private String currency;
    private List<String> stack;
    private String url;
    private String descriptionRaw;
    private String publishedAt;
    private String fetchedAt;

    @DynamoDbPartitionKey
    public String getDedupKey() { return dedupKey; }
    public void setDedupKey(String dedupKey) { this.dedupKey = dedupKey; }

    @DynamoDbSecondaryPartitionKey(indexNames = "bySourceRef")
    public String getSourceRef() { return sourceRef; }
    public void setSourceRef(String sourceRef) { this.sourceRef = sourceRef; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getSourceExternalId() { return sourceExternalId; }
    public void setSourceExternalId(String sourceExternalId) { this.sourceExternalId = sourceExternalId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getLocationRaw() { return locationRaw; }
    public void setLocationRaw(String locationRaw) { this.locationRaw = locationRaw; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public Integer getRemotePercent() { return remotePercent; }
    public void setRemotePercent(Integer remotePercent) { this.remotePercent = remotePercent; }

    public String getContractType() { return contractType; }
    public void setContractType(String contractType) { this.contractType = contractType; }

    public Integer getSalaryMin() { return salaryMin; }
    public void setSalaryMin(Integer salaryMin) { this.salaryMin = salaryMin; }

    public Integer getSalaryMax() { return salaryMax; }
    public void setSalaryMax(Integer salaryMax) { this.salaryMax = salaryMax; }

    public Integer getTjmMin() { return tjmMin; }
    public void setTjmMin(Integer tjmMin) { this.tjmMin = tjmMin; }

    public Integer getTjmMax() { return tjmMax; }
    public void setTjmMax(Integer tjmMax) { this.tjmMax = tjmMax; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public List<String> getStack() { return stack; }
    public void setStack(List<String> stack) { this.stack = stack; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getDescriptionRaw() { return descriptionRaw; }
    public void setDescriptionRaw(String descriptionRaw) { this.descriptionRaw = descriptionRaw; }

    public String getPublishedAt() { return publishedAt; }
    public void setPublishedAt(String publishedAt) { this.publishedAt = publishedAt; }

    public String getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(String fetchedAt) { this.fetchedAt = fetchedAt; }
}
