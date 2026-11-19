package home.inventory.catalog.article;

import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;

public interface ArticleRepository extends ListCrudRepository<Article, UUID> {

    List<Article> findAllByDeletedFalse();
}
