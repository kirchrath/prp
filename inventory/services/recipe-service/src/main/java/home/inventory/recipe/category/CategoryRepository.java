package home.inventory.recipe.category;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;

public interface CategoryRepository extends ListCrudRepository<Category, UUID> {

    List<Category> findAllByDeletedFalseOrderByNameAsc();

    Optional<Category> findByNameIgnoreCaseAndDeletedFalse(String name);
}
