package home.inventory.recipe.category.dto;

import home.inventory.recipe.common.dto.AuditDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

/** Create (without audit) or update (audit.version of the edited state). */
public record CategorySaveDto(@Nullable AuditDto audit, @NotNull @Valid CategoryDataDto data) {
}
