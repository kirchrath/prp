package home.inventory.shopping.copy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;

public interface BatchCopyRepository extends ListCrudRepository<BatchCopy, UUID> {

    Optional<BatchCopy> findByBatchId(UUID batchId);

    List<BatchCopy> findAllByArticleIdAndDeletedFalse(UUID articleId);
}
