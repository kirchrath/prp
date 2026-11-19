package home.inventory.recipe.imports.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Recipe as free text, e.g. copied from a cookbook or a message. */
public record TextImportDto(@NotBlank @Size(max = 20000) String text) {
}
