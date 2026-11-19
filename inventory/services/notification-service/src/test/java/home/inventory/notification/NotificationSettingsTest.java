package home.inventory.notification;

import static org.assertj.core.api.Assertions.assertThat;

import home.inventory.notification.settings.CategoryThreshold;
import home.inventory.notification.settings.NotificationSettings;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationSettingsTest {

    @Test
    void thresholdsAndWithers() {
        UUID dairy = UUID.randomUUID();
        NotificationSettings settings = new NotificationSettings(null, null, null, null, false, true, LocalTime.NOON, 3,
                Set.of(new CategoryThreshold(dairy, 1), new CategoryThreshold(UUID.randomUUID(), 10)));
        assertThat(settings.daysFor(dairy)).isEqualTo(1);
        assertThat(settings.daysFor(UUID.randomUUID())).isEqualTo(3);
        assertThat(settings.maxDays()).isEqualTo(10);

        Instant now = Instant.now();
        NotificationSettings copy = settings.withId(NotificationSettings.ID).withCreatedDate(now);
        assertThat(copy.id()).isEqualTo(NotificationSettings.ID);
        assertThat(copy.createdDate()).isEqualTo(now);
    }
}
