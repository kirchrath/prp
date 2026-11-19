package home.inventory.stock.location.dto;

import home.inventory.stock.common.dto.AuditDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

/** Create (without audit) or update (audit of the edited state). */
public record LocationSaveDto(@Nullable AuditDto audit, @NotNull @Valid LocationDataDto data) {
}
