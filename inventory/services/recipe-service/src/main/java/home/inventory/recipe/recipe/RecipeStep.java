package home.inventory.recipe.recipe;

import org.springframework.data.relational.core.mapping.Table;

/** Preparation step (part of the recipe aggregate); the order is the list order. */
@Table("recipe_step")
public record RecipeStep(String text) {
}
