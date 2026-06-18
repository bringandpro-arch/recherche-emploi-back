package fr.cachi.emplois.infrastructure.persistence;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.util.List;

/**
 * Représentation DynamoDB du profil (bean mutable requis par l'Enhanced Client).
 * Le mapping vers/depuis le modèle de domaine {@code Profile} est fait dans le repository.
 */
@DynamoDbBean
public class ProfileItem {

    private String userId;
    private String label;
    private List<String> contractTypes;
    private List<String> locations;
    private Integer remoteMin;
    private Integer targetTjmMin;
    private Integer targetSalaryMin;
    private List<String> skills;
    private List<String> keywords;
    private List<String> excludedKeywords;
    private Integer notifyThreshold;
    private String telegramChatId;
    private Boolean active;
    private String createdAt;
    private String updatedAt;

    @DynamoDbPartitionKey
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public List<String> getContractTypes() { return contractTypes; }
    public void setContractTypes(List<String> contractTypes) { this.contractTypes = contractTypes; }

    public List<String> getLocations() { return locations; }
    public void setLocations(List<String> locations) { this.locations = locations; }

    public Integer getRemoteMin() { return remoteMin; }
    public void setRemoteMin(Integer remoteMin) { this.remoteMin = remoteMin; }

    public Integer getTargetTjmMin() { return targetTjmMin; }
    public void setTargetTjmMin(Integer targetTjmMin) { this.targetTjmMin = targetTjmMin; }

    public Integer getTargetSalaryMin() { return targetSalaryMin; }
    public void setTargetSalaryMin(Integer targetSalaryMin) { this.targetSalaryMin = targetSalaryMin; }

    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }

    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }

    public List<String> getExcludedKeywords() { return excludedKeywords; }
    public void setExcludedKeywords(List<String> excludedKeywords) { this.excludedKeywords = excludedKeywords; }

    public Integer getNotifyThreshold() { return notifyThreshold; }
    public void setNotifyThreshold(Integer notifyThreshold) { this.notifyThreshold = notifyThreshold; }

    public String getTelegramChatId() { return telegramChatId; }
    public void setTelegramChatId(String telegramChatId) { this.telegramChatId = telegramChatId; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
