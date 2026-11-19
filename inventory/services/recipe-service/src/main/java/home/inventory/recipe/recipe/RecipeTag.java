package home.inventory.recipe.recipe;

import org.springframework.data.relational.core.mapping.Table;

/** Tag of a recipe, e.g. "vegetarisch" (part of the recipe aggregate). */
@Table("recipe_tag")
public record RecipeTag(String name) {
}
