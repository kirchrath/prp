package home.inventory.notification.settings;

import java.util.UUID;
import org.springframework.data.relational.core.mapping.Table;

/** Own warning threshold (days before the best-before date) for an article category. */
@Table("category_threshold")
public record CategoryThreshold(UUID categoryId, int days) {
}
