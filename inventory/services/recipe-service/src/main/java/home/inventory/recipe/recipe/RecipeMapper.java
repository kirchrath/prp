package home.inventory.recipe.recipe;

import home.inventory.recipe.common.AuditMapper;
import home.inventory.recipe.recipe.dto.IngredientDto;
import home.inventory.recipe.recipe.dto.RecipeDataDto;
import home.inventory.recipe.recipe.dto.RecipeDto;
import home.inventory.recipe.recipe.dto.RecipeEventDto;
import home.inventory.recipe.recipe.dto.RecipeListDto;
import java.util.List;
import org.jspecify.annotations.Nullable;

public final class RecipeMapper {

    private RecipeMapper() {
    }

    public static RecipeDto toDto(Recipe r) {
        return new RecipeDto(AuditMapper.toDto(r), data(r));
    }

    public static RecipeEventDto toEventDto(Recipe r) {
        return new RecipeEventDto(r.id(), AuditMapper.toDto(r), data(r));
    }

    public static RecipeListDto toListDto(Recipe r, @Nullable String categoryName) {
        return new RecipeListDto(r.id(), r.title(), r.imageUrl(), r.categoryId(), categoryName, tags(r), r.servings(),
                r.totalMinutes());
    }

    public static RecipeDataDto data(Recipe r) {
        return new RecipeDataDto(r.title(), r.description(), r.servings(), r.prepMinutes(), r.cookMinutes(), r.imageUrl(),
                r.sourceUrl(), r.categoryId(), tags(r), r.steps().stream().map(RecipeStep::text).toList(),
                r.ingredients().stream()
                        .map(i -> new IngredientDto(i.name(), i.amount(), i.unit(), i.articleId(), i.basic()))
                        .toList());
    }

    private static List<String> tags(Recipe r) {
        return r.tags().stream().map(RecipeTag::name).sorted().toList();
    }
}
