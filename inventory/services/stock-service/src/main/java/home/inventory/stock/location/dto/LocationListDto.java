package home.inventory.stock.location.dto;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Location as shown in lists and selects; {@code path} like "Keller / Regal 2". */
public record LocationListDto(UUID id, String name, @Nullable String description, @Nullable UUID parentId,
                              String path) {
}
