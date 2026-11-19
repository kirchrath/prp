package home.inventory.stock.batch;

import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;

public interface StockBatchRepository extends ListCrudRepository<StockBatch, UUID> {

    boolean existsByLocationIdAndDeletedFalse(UUID locationId);

    List<StockBatch> findAllByArticleIdAndDeletedFalse(UUID articleId);

    List<StockBatch> findAllByArticleIdAndLocationIdAndDeletedFalse(UUID articleId, UUID locationId);

    List<StockBatch> findAllByDeletedFalse();
}
