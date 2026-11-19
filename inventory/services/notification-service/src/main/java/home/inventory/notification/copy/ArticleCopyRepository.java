package home.inventory.notification.copy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;

public interface ArticleCopyRepository extends ListCrudRepository<ArticleCopy, UUID> {

    Optional<ArticleCopy> findByArticleId(UUID articleId);

    List<ArticleCopy> findAllByDeletedFalse();
}
