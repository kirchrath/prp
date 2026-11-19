package home.inventory.shopping.item.dto;

import home.inventory.shopping.common.dto.AuditDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

/** Create (without audit) or update (audit of the edited state). */
public record ShoppingItemSaveDto(@Nullable AuditDto audit, @NotNull @Valid ShoppingItemDataDto data) {
}
