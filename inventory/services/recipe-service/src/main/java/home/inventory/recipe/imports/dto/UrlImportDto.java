package home.inventory.recipe.imports.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Link to a recipe page. */
public record UrlImportDto(@NotBlank @Size(max = 1000) @Pattern(regexp = "https?://\\S+") String url) {
}
