package home.inventory.stock.location.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record LocationDataDto(@NotBlank @Size(max = 100) String name,
                              @Nullable @Size(max = 500) String description,
                              @Nullable UUID parentId) {
}
