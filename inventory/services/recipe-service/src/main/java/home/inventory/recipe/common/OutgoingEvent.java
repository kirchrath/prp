package home.inventory.recipe.common;

import java.util.UUID;

/** An event waiting for the end of the current transaction. */
public record OutgoingEvent(String topic, UUID key, Object payload) {
}
