package home.inventory.recipe.shopping;

import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;

public interface ShoppingRequestRepository extends ListCrudRepository<ShoppingRequest, UUID> {
}
