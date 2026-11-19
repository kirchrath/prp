package home.inventory.stock;

import static org.assertj.core.api.Assertions.assertThat;

import home.inventory.stock.common.UuidGeneratingCallback;
import home.inventory.stock.location.Location;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UuidGeneratingCallbackTest {

    private final UuidGeneratingCallback callback = new UuidGeneratingCallback();

    @Test
    void assignsIdToNewAggregates() {
        assertThat(((Location) callback.onBeforeConvert(Location.create("x", null, null))).id()).isNotNull();
    }

    @Test
    void keepsExistingId() {
        Location location = Location.create("x", null, null).withId(UUID.randomUUID());
        assertThat(callback.onBeforeConvert(location)).isSameAs(location);
    }
}
