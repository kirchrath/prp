package home.inventory.recipe.recipe;

import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;

public interface RecipeRepository extends ListCrudRepository<Recipe, UUID> {

    List<Recipe> findAllByDeletedFalse();
}
