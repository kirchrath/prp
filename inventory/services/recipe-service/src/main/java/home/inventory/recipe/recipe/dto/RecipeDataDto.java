package home.inventory.recipe.recipe.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Recipe data; {@code servings} is the number of persons the amounts are meant for. */
public record RecipeDataDto(
        @NotBlank @Size(max = 200) String title,
        @Nullable @Size(max = 2000) String description,
        @Min(1) @Max(100) int servings,
        @Nullable @Min(0) Integer prepMinutes,
        @Nullable @Min(0) Integer cookMinutes,
        @Nullable @Size(max = 1000) String imageUrl,
        @Nullable @Size(max = 1000) String sourceUrl,
        @Nullable UUID categoryId,
        @NotNull List<@NotBlank @Size(max = 50) String> tags,
        @NotNull List<@NotBlank @Size(max = 4000) String> steps,
        @NotNull List<@NotNull @Valid IngredientDto> ingredients) {
}
