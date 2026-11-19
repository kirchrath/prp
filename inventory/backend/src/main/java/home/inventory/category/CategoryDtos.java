package home.inventory.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class CategoryDtos {

    private CategoryDtos() {
    }

    public record CategoryDto(Long id, String name) {
        static CategoryDto of(Category c) {
            return new CategoryDto(c.getId(), c.getName());
        }
    }

    public record CategoryRequest(@NotBlank @Size(max = 100) String name) {
    }
}
