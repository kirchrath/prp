package home.inventory.recipe.recipe.dto;

import home.inventory.recipe.common.dto.AuditDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

/** Create (without audit) or update (audit of the edited state). */
public record RecipeSaveDto(@Nullable AuditDto audit, @NotNull @Valid RecipeDataDto data) {
}
