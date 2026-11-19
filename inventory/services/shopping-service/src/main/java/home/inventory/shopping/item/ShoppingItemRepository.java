package home.inventory.shopping.item;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;

public interface ShoppingItemRepository extends ListCrudRepository<ShoppingItem, UUID> {

    List<ShoppingItem> findAllByDeletedFalse();

    List<ShoppingItem> findAllByDoneTrueAndDeletedFalse();

    Optional<ShoppingItem> findFirstByArticleIdAndAutomaticTrueAndDeletedFalse(UUID articleId);
}
