package home.inventory.recipe.recipe;

import java.math.BigDecimal;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Ingredient of a recipe (part of the recipe aggregate). {@code basic} ingredients (salt, water, …) never count as
 * missing; {@code articleId} links the ingredient to a catalog article.
 */
@Table("recipe_ingredient")
public record RecipeIngredient(String name, @Nullable BigDecimal amount, @Nullable IngredientUnit unit,
                               @Nullable UUID articleId, boolean basic) {
}
