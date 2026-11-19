package home.inventory.notification.settings;

import home.inventory.notification.common.AuditedEntity;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Settings of the daily best-before digest; exactly one instance ({@link #ID}, created by Flyway).
 *
 * @param sendTime    local time from which the digest of the day is sent
 * @param defaultDays an article is "expiring soon" this many days before its best-before date
 */
@Table("notification_settings")
public record NotificationSettings(
        @Id UUID id,
        @Version Long version,
        @CreatedDate Instant createdDate,
        @LastModifiedDate Instant lastModifiedDate,
        boolean deleted,
        boolean enabled,
        LocalTime sendTime,
        int defaultDays,
        @MappedCollection(idColumn = "settings_id") Set<CategoryThreshold> categoryThresholds)
        implements AuditedEntity<NotificationSettings> {

    public static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Override
    public NotificationSettings withId(UUID id) {
        return new NotificationSettings(id, version, createdDate, lastModifiedDate, deleted, enabled, sendTime,
                defaultDays, categoryThresholds);
    }

    public NotificationSettings withVersion(Long version) {
        return new NotificationSettings(id, version, createdDate, lastModifiedDate, deleted, enabled, sendTime,
                defaultDays, categoryThresholds);
    }

    public NotificationSettings withCreatedDate(Instant createdDate) {
        return new NotificationSettings(id, version, createdDate, lastModifiedDate, deleted, enabled, sendTime,
                defaultDays, categoryThresholds);
    }

    public NotificationSettings withLastModifiedDate(Instant lastModifiedDate) {
        return new NotificationSettings(id, version, createdDate, lastModifiedDate, deleted, enabled, sendTime,
                defaultDays, categoryThresholds);
    }

    public NotificationSettings withData(boolean enabled, LocalTime sendTime, int defaultDays,
                                         Set<CategoryThreshold> categoryThresholds) {
        return new NotificationSettings(id, version, createdDate, lastModifiedDate, deleted, enabled, sendTime,
                defaultDays, categoryThresholds);
    }

    /** Warning threshold in days for an article of this category. */
    public int daysFor(UUID categoryId) {
        return categoryThresholds.stream()
                .filter(t -> t.categoryId().equals(categoryId))
                .mapToInt(CategoryThreshold::days)
                .findFirst()
                .orElse(defaultDays);
    }

    /** Largest threshold: batches further away never need to be loaded. */
    public int maxDays() {
        return categoryThresholds.stream().mapToInt(CategoryThreshold::days).reduce(defaultDays, Math::max);
    }
}
