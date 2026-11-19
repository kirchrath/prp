package home.inventory.device.scanner.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Assignment of a button (1..8) of a button box to an article. */
public record ButtonDto(@Min(1) @Max(8) int button, @NotNull UUID articleId) {
}
