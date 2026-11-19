package home.inventory.device.common;

import java.util.UUID;
import org.springframework.data.relational.core.mapping.event.BeforeConvertCallback;
import org.springframework.stereotype.Component;

/** Assigns a random UUID to new aggregates before they are written. */
@Component
public class UuidGeneratingCallback implements BeforeConvertCallback<AuditedEntity<?>> {

    @Override
    public AuditedEntity<?> onBeforeConvert(AuditedEntity<?> aggregate) {
        return aggregate.id() == null ? aggregate.withId(UUID.randomUUID()) : aggregate;
    }
}
