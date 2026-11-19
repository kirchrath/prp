package home.inventory.shopping.copy;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;

public interface ArticleCopyRepository extends ListCrudRepository<ArticleCopy, UUID> {

    Optional<ArticleCopy> findByArticleId(UUID articleId);
}
