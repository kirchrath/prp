package home.inventory.recipe.cooking.dto;

import home.inventory.recipe.recipe.IngredientUnit;
import java.math.BigDecimal;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** A missing ingredient; amount scaled to the requested servings, {@code packages} to buy. */
public record MissingIngredientDto(String name, @Nullable BigDecimal amount, @Nullable IngredientUnit unit,
                                   @Nullable UUID articleId, @Nullable String articleName, int packages) {
}
