package home.inventory.notification.settings;

import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;

public interface NotificationSettingsRepository extends ListCrudRepository<NotificationSettings, UUID> {
}
