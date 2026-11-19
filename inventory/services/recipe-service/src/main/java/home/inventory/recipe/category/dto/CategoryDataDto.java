package home.inventory.recipe.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryDataDto(@NotBlank @Size(max = 100) String name) {
}
