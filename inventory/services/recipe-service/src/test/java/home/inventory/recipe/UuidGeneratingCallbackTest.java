package home.inventory.recipe;

import static org.assertj.core.api.Assertions.assertThat;

import home.inventory.recipe.common.UuidGeneratingCallback;
import home.inventory.recipe.recipe.Recipe;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UuidGeneratingCallbackTest {

    private final UuidGeneratingCallback callback = new UuidGeneratingCallback();

    @Test
    void assignsIdToNewAggregates() {
        assertThat(((Recipe) callback.onBeforeConvert(Recipe.create())).id()).isNotNull();
    }

    @Test
    void keepsExistingId() {
        Recipe recipe = Recipe.create().withId(UUID.randomUUID());
        assertThat(callback.onBeforeConvert(recipe)).isSameAs(recipe);
    }
}
