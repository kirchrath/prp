package home.inventory.catalog.category;

import home.inventory.catalog.category.dto.CategoryDataDto;
import home.inventory.catalog.category.dto.CategoryDto;
import home.inventory.catalog.category.dto.CategoryEventDto;
import home.inventory.catalog.category.dto.CategoryListDto;
import home.inventory.catalog.common.AuditMapper;

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
