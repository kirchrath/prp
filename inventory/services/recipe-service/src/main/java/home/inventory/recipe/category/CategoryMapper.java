package home.inventory.recipe.category;

import home.inventory.recipe.category.dto.CategoryDataDto;
import home.inventory.recipe.category.dto.CategoryDto;
import home.inventory.recipe.category.dto.CategoryEventDto;
import home.inventory.recipe.category.dto.CategoryListDto;
import home.inventory.recipe.common.AuditMapper;

public final class CategoryMapper {

    private CategoryMapper() {
    }

    public static CategoryDto toDto(Category c) {
        return new CategoryDto(AuditMapper.toDto(c), data(c));
    }

    public static CategoryListDto toListDto(Category c) {
        return new CategoryListDto(c.id(), c.name());
    }

    public static CategoryEventDto toEventDto(Category c) {
        return new CategoryEventDto(c.id(), AuditMapper.toDto(c), data(c));
    }

    private static CategoryDataDto data(Category c) {
        return new CategoryDataDto(c.name());
    }
}
