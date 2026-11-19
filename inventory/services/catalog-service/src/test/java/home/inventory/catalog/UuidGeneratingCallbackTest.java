package home.inventory.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import home.inventory.catalog.category.Category;
import home.inventory.catalog.common.UuidGeneratingCallback;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UuidGeneratingCallbackTest {

    private final UuidGeneratingCallback callback = new UuidGeneratingCallback();

    @Test
    void assignsIdToNewAggregates() {
        Category result = (Category) callback.onBeforeConvert(Category.create("x"));
        assertThat(result.id()).isNotNull();
    }

    @Test
    void keepsExistingId() {
        UUID id = UUID.randomUUID();
        Category category = Category.create("x").withId(id);
        assertThat(callback.onBeforeConvert(category)).isSameAs(category);
    }
}
