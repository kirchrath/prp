package home.inventory.stock.article;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;

public interface BarcodeCopyRepository extends ListCrudRepository<BarcodeCopy, UUID> {

    Optional<BarcodeCopy> findFirstByEanAndDeletedFalse(String ean);

    List<BarcodeCopy> findAllByArticleIdAndDeletedFalse(UUID articleId);
}
