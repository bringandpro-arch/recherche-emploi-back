package fr.cachi.emplois.infrastructure.persistence;

import fr.cachi.emplois.domain.model.ContractType;
import fr.cachi.emplois.domain.model.Profile;
import fr.cachi.emplois.domain.port.ProfileRepository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Implémentation DynamoDB du {@link ProfileRepository}. */
public class DynamoProfileRepository implements ProfileRepository {

    private final DynamoDbTable<ProfileItem> table;

    public DynamoProfileRepository() {
        this.table = Dynamo.enhanced().table(
                Dynamo.table("TABLE_PROFILES", "Profiles"),
                TableSchema.fromBean(ProfileItem.class));
    }

    @Override
    public Optional<Profile> findByUserId(String userId) {
        ProfileItem item = table.getItem(Key.builder().partitionValue(userId).build());
        return Optional.ofNullable(item).map(DynamoProfileRepository::toDomain);
    }

    @Override
    public Profile save(Profile profile) {
        table.putItem(toItem(profile));
        return profile;
    }

    @Override
    public List<Profile> findAllActive() {
        return table.scan().items().stream()
                .map(DynamoProfileRepository::toDomain)
                .filter(Profile::active)
                .toList();
    }

    // ───────────────────────── mapping ─────────────────────────

    static ProfileItem toItem(Profile p) {
        ProfileItem i = new ProfileItem();
        i.setUserId(p.userId());
        i.setLabel(p.label());
        i.setContractTypes(p.contractTypes() == null ? List.of()
                : p.contractTypes().stream().map(Enum::name).toList());
        i.setLocations(p.locations());
        i.setRemoteMin(p.remoteMin());
        i.setTargetTjmMin(p.targetTjmMin());
        i.setTargetSalaryMin(p.targetSalaryMin());
        i.setSkills(p.skills());
        i.setKeywords(p.keywords());
        i.setExcludedKeywords(p.excludedKeywords());
        i.setNotifyThreshold(p.notifyThreshold());
        i.setTelegramChatId(p.telegramChatId());
        i.setActive(p.active());
        i.setCreatedAt(p.createdAt() == null ? null : p.createdAt().toString());
        i.setUpdatedAt(p.updatedAt() == null ? null : p.updatedAt().toString());
        return i;
    }

    static Profile toDomain(ProfileItem i) {
        return new Profile(
                i.getUserId(),
                i.getLabel(),
                i.getContractTypes() == null ? List.of()
                        : i.getContractTypes().stream().map(DynamoProfileRepository::parseContract).toList(),
                i.getLocations() == null ? List.of() : i.getLocations(),
                i.getRemoteMin(),
                i.getTargetTjmMin(),
                i.getTargetSalaryMin(),
                i.getSkills() == null ? List.of() : i.getSkills(),
                i.getKeywords() == null ? List.of() : i.getKeywords(),
                i.getExcludedKeywords() == null ? List.of() : i.getExcludedKeywords(),
                i.getNotifyThreshold(),
                i.getTelegramChatId(),
                i.getActive() != null && i.getActive(),
                parseInstant(i.getCreatedAt()),
                parseInstant(i.getUpdatedAt()));
    }

    private static ContractType parseContract(String s) {
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
