package home.inventory.recipe.recipe.dto;

import home.inventory.recipe.recipe.IngredientUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Ingredient; without amount ("etwas Salz") it only needs to be in stock. */
public record IngredientDto(@NotBlank @Size(max = 200) String name, @Nullable @Positive BigDecimal amount,
                            @Nullable IngredientUnit unit, @Nullable UUID articleId, boolean basic) {
}
