package home.inventory.recipe.recipe;

import home.inventory.recipe.common.AuditedEntity;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/** A recipe with its ingredients, steps and tags (child records of the aggregate). */
@Table("recipe")
public record Recipe(
        @Id UUID id,
        @Version Long version,
        @CreatedDate Instant createdDate,
        @LastModifiedDate Instant lastModifiedDate,
        boolean deleted,
        String title,
        @Nullable String description,
        int servings,
        @Nullable Integer prepMinutes,
        @Nullable Integer cookMinutes,
        @Nullable String imageUrl,
        @Nullable String sourceUrl,
        @Nullable UUID categoryId,
        @MappedCollection(idColumn = "recipe_id") Set<RecipeTag> tags,
        @MappedCollection(idColumn = "recipe_id", keyColumn = "position") List<RecipeStep> steps,
        @MappedCollection(idColumn = "recipe_id", keyColumn = "position") List<RecipeIngredient> ingredients)
        implements AuditedEntity<Recipe> {

    public static Recipe create() {
        return new Recipe(null, null, null, null, false, "", null, 1, null, null, null, null, null, Set.of(), List.of(),
                List.of());
    }

    @Override
    public Recipe withId(UUID id) {
        return new Recipe(id, version, createdDate, lastModifiedDate, deleted, title, description, servings, prepMinutes,
                cookMinutes, imageUrl, sourceUrl, categoryId, tags, steps, ingredients);
    }

    public Recipe withVersion(Long version) {
        return new Recipe(id, version, createdDate, lastModifiedDate, deleted, title, description, servings, prepMinutes,
                cookMinutes, imageUrl, sourceUrl, categoryId, tags, steps, ingredients);
    }

    public Recipe withCreatedDate(Instant createdDate) {
        return new Recipe(id, version, createdDate, lastModifiedDate, deleted, title, description, servings, prepMinutes,
                cookMinutes, imageUrl, sourceUrl, categoryId, tags, steps, ingredients);
    }

    public Recipe withLastModifiedDate(Instant lastModifiedDate) {
        return new Recipe(id, version, createdDate, lastModifiedDate, deleted, title, description, servings, prepMinutes,
                cookMinutes, imageUrl, sourceUrl, categoryId, tags, steps, ingredients);
    }

    public Recipe withData(String title, @Nullable String description, int servings, @Nullable Integer prepMinutes,
                           @Nullable Integer cookMinutes, @Nullable String imageUrl, @Nullable String sourceUrl,
                           @Nullable UUID categoryId, Set<RecipeTag> tags, List<RecipeStep> steps,
                           List<RecipeIngredient> ingredients) {
        return new Recipe(id, version, createdDate, lastModifiedDate, deleted, title, description, servings, prepMinutes,
                cookMinutes, imageUrl, sourceUrl, categoryId, tags, steps, ingredients);
    }

    public Recipe withDeleted(boolean deleted) {
        return new Recipe(id, version, createdDate, lastModifiedDate, deleted, title, description, servings, prepMinutes,
                cookMinutes, imageUrl, sourceUrl, categoryId, tags, steps, ingredients);
    }

    /** Preparation plus cooking time, if known. */
    public @Nullable Integer totalMinutes() {
        if (prepMinutes == null && cookMinutes == null) {
            return null;
        }
        return (prepMinutes == null ? 0 : prepMinutes) + (cookMinutes == null ? 0 : cookMinutes);
    }
}
