package home.inventory.shopping;

import static org.assertj.core.api.Assertions.assertThat;

import home.inventory.shopping.common.UuidGeneratingCallback;
import home.inventory.shopping.item.ShoppingItem;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UuidGeneratingCallbackTest {

    private final UuidGeneratingCallback callback = new UuidGeneratingCallback();

    @Test
    void assignsIdToNewAggregates() {
        assertThat(((ShoppingItem) callback.onBeforeConvert(ShoppingItem.create(null, "x", 1, false))).id()).isNotNull();
    }

    @Test
    void keepsExistingId() {
        ShoppingItem item = ShoppingItem.create(null, "x", 1, false).withId(UUID.randomUUID());
        assertThat(callback.onBeforeConvert(item)).isSameAs(item);
    }
}
